package iamrescue.belief.inference;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.entities.RoutingInfoBlockade;
import iamrescue.belief.provenance.IPropertyMerger;
import iamrescue.belief.provenance.InferredOrigin;
import iamrescue.belief.provenance.ProvenanceLogEntry;
import iamrescue.routing.WorldModelConverter;
import iamrescue.routing.costs.BlockCheckerUtil;
import iamrescue.util.PositionXY;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javolution.util.FastSet;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityListener;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.WorldModel;
import rescuecore2.worldmodel.WorldModelListener;
import rescuecore2.worldmodel.properties.IntArrayProperty;

/**
 * Detects passable roads based on an agent's position history.
 * 
 * @author Sebastian
 * 
 */
public class RoutingInfoUpdater implements WorldModelListener<StandardEntity>,
		EntityListener {
	private IAMWorldModel worldModel;
	private WorldModelConverter converter;

	private Set<RoutingInfoBlockade> updated = new FastSet<RoutingInfoBlockade>();
	private ISimulationTimer timer;

	private IPropertyMerger propertyMerger;

	public RoutingInfoUpdater(IAMWorldModel worldModel,
			WorldModelConverter converter, ISimulationTimer timer,
			IPropertyMerger propertyMerger) {
		this.worldModel = worldModel;
		this.converter = converter;
		this.timer = timer;
		this.propertyMerger = propertyMerger;

		worldModel.addWorldModelListener(this);

		for (StandardEntity blockade : worldModel
				.getEntitiesOfType(StandardEntityURN.BLOCKADE)) {
			blockade.addEntityListener(this);
		}
	}

	public ChangeSet inferUpdatedBlockades() {

		ChangeSet changed = new ChangeSet();

		for (RoutingInfoBlockade b : updated) {
			if (b.isApexesDefined()) {

				List<Integer> positionArray = new ArrayList<Integer>();
				// Only update if we have observed apexes, and know position

				// System.out.println("Checking block " +
				// b.getFullDescription());

				List<Integer> neighbours = converter.getSortedNeighbours(b
						.getPosition().getValue());

				// System.out.println("Neighbours: " + neighbours);

				for (int i = 0; i < neighbours.size() - 1; i++) {
					for (int j = i + 1; j < neighbours.size(); j++) {
						PositionXY from = converter.getSimpleGraphNode(
								neighbours.get(i)).getRepresentativePoint();
						PositionXY to = converter.getSimpleGraphNode(
								neighbours.get(j)).getRepresentativePoint();

						if (BlockCheckerUtil.isBlocking(b, from, to)) {
							/*
							 * positionArray.add(from.getX());
							 * positionArray.add(from.getY());
							 * positionArray.add(to.getX());
							 * positionArray.add(to.getY());
							 */
							positionArray.add(i);
							positionArray.add(j);
							// System.out.println("Blocking " +
							// neighbours.get(i)
							// + " to " + neighbours.get(j));
						}
					}
				}

				int[] blocking = new int[positionArray.size()];
				for (int i = 0; i < blocking.length; i++) {
					blocking[i] = positionArray.get(i);
				}

				/*
				 * if (positionArray.size() > 0) {
				 * 
				 * 
				 * IntArrayProperty blockedEdgesProperty = b
				 * .getBlockedEdgesProperty();
				 * 
				 * boolean needsUpdating = false;
				 * 
				 * if (!blockedEdgesProperty.isDefined()) { needsUpdating =
				 * true; } else { int[] previous = b.getBlockedEdges(); if
				 * (previous.length != blocking.length) { needsUpdating = true;
				 * } else { for (int i = 0; i < previous.length; i++) { if
				 * (previous[i] != blocking[i]) { needsUpdating = true; break; }
				 * } } }
				 * 
				 * if (needsUpdating) {
				 * 
				 * } }
				 */

				IntArrayProperty newProperty = new IntArrayProperty(
						RoutingInfoBlockade.BLOCK_INFO_URN);
				newProperty.setValue(blocking);

				ProvenanceLogEntry entry = new ProvenanceLogEntry(timer
						.getTime(), InferredOrigin.INSTANCE, newProperty);

				worldModel.storeProvenance(b.getID(), entry);
				Property decidedValue = propertyMerger.decideValue(worldModel
						.getProvenance(b.getID(),
								RoutingInfoBlockade.BLOCK_INFO_URN));

				changed.addChange(b.getID(),
						RoutingInfoBlockade.BLOCK_INFO_URN, decidedValue);

				Property property = b.getBlockedEdgesProperty();

				if (decidedValue.isDefined()) {
					property.takeValue(decidedValue);
				} else {
					property.undefine();
				}
			}
		}

		if (updated.size() > 0) {
			updated.clear();
		}

		return changed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * rescuecore2.worldmodel.WorldModelListener#entityAdded(rescuecore2.worldmodel
	 * .WorldModel, rescuecore2.worldmodel.Entity)
	 */
	@Override
	public void entityAdded(WorldModel<? extends StandardEntity> model,
			StandardEntity e) {
		if (e instanceof RoutingInfoBlockade) {
			e.addEntityListener(this);
			updated.add((RoutingInfoBlockade) e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seerescuecore2.worldmodel.WorldModelListener#entityRemoved(rescuecore2.
	 * worldmodel.WorldModel, rescuecore2.worldmodel.Entity)
	 */
	@Override
	public void entityRemoved(WorldModel<? extends StandardEntity> model,
			StandardEntity e) {
		if (e instanceof RoutingInfoBlockade) {
			e.removeEntityListener(this);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * rescuecore2.worldmodel.EntityListener#propertyChanged(rescuecore2.worldmodel
	 * .Entity, rescuecore2.worldmodel.Property, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void propertyChanged(Entity e, Property p, Object oldValue,
			Object newValue) {
		if (p.getURN().equals(StandardPropertyURN.APEXES)) {
			updated.add((RoutingInfoBlockade) e);
		}
	}
}