package iamrescue.belief.inference;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.provenance.IPropertyMerger;
import iamrescue.belief.provenance.IProvenanceInformation;
import iamrescue.belief.provenance.InferredOrigin;
import iamrescue.belief.provenance.ProvenanceLogEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.EntityListener;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.EntityRefListProperty;

public class BlockForgettingModule implements EntityListener {

	private Map<EntityID, Integer> lastSensed = new FastMap<EntityID, Integer>();
	private int timeOut;
	private ISimulationTimer timer;
	private Set<Road> updated = new FastSet<Road>();
	private IPropertyMerger merger;
	private IAMWorldModel worldModel;

	public BlockForgettingModule(IAMWorldModel worldModel,
			IPropertyMerger merger, ISimulationTimer timer, int timeOut) {
		this.timeOut = timeOut;
		this.timer = timer;
		this.worldModel = worldModel;
		this.merger = merger;
		Collection<StandardEntity> roads = worldModel
				.getEntitiesOfType(StandardEntityURN.ROAD);
		for (StandardEntity road : roads) {
			road.addEntityListener(this);
		}
	}

	public void forgetOldBlockades() {
		// First process updates
		for (Road updatedRoad : updated) {
			if (updatedRoad.isBlockadesDefined()) {
				if (updatedRoad.getBlockades().size() == 0) {
					lastSensed.remove(updatedRoad.getID());
				} else {
					lastSensed.put(updatedRoad.getID(), timer.getTime());
				}
			}
		}

		// Now time out if necessary
		for (Entry<EntityID, Integer> entry : lastSensed.entrySet()) {
			int timeDiff = timer.getTime() - entry.getValue();
			if (timeDiff >= timeOut) {
				// Forget blockades
				Road road = (Road) worldModel.getEntity(entry.getKey());

				// Now remove list from road
				EntityRefListProperty blockadesProperty = road
						.getBlockadesProperty();

				EntityRefListProperty emptyList = blockadesProperty.copy();
				emptyList.setValue(new ArrayList<EntityID>());

				IProvenanceInformation provenance = worldModel.getProvenance(
						road.getID(), StandardPropertyURN.BLOCKADES);

				provenance.addEntry(new ProvenanceLogEntry(timer.getTime(),
						InferredOrigin.INSTANCE, emptyList));

				blockadesProperty.takeValue(merger.decideValue(provenance));
				if (blockadesProperty.isDefined()
						&& blockadesProperty.getValue().size() == 0) {
					List<EntityID> blockades = road.getBlockades();
					for (EntityID entityID : blockades) {
						// Remove blockade
						worldModel.removeEntity(entityID);
					}
				}

			}
		}

		updated.clear();
	}

	@Override
	public void propertyChanged(Entity e, Property p, Object oldValue,
			Object newValue) {
		if (e instanceof Road
				&& p.getURN().equals(StandardPropertyURN.BLOCKADES.toString())) {
			updated.add((Road) e);
		}
	}
}
