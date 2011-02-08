/**
 * 
 */
package iamrescue.belief.inference;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.entities.RoutingInfoBlockade;
import iamrescue.belief.provenance.IPropertyMerger;
import iamrescue.belief.provenance.InferredOrigin;
import iamrescue.belief.provenance.ProvenanceLogEntry;
import iamrescue.belief.spatial.ISpatialIndex;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

/**
 * @author Sebastian
 * 
 */
public class NegativeSenseUpdater implements INegativeSenseUpdater {

	private static final int DEFAULT_VIEW_DISTANCE = 30000;
	public static final String VIEW_DISTANCE_KEY = "perception.los.max-distance";
	private static final List<String> HUMAN_PROPERTIES = new FastList<String>();
	private static final List<String> BLOCKADE_PROPERTIES = new FastList<String>();

	private int viewDistance;
	private EntityID myself;
	private IAMWorldModel worldModel;
	private ISimulationTimer timer;
	private IPropertyMerger propertyMerger;

	private static final Logger LOGGER = Logger
			.getLogger(NegativeSenseUpdater.class);

	public NegativeSenseUpdater(Config config, IAMWorldModel worldModel,
			EntityID myself, ISimulationTimer timer,
			IPropertyMerger propertyMerger) {
		viewDistance = config.getIntValue(VIEW_DISTANCE_KEY,
				DEFAULT_VIEW_DISTANCE);
		this.worldModel = worldModel;
		this.myself = myself;
		// this.spatialIndex = spatialIndex;
		this.timer = timer;
		this.propertyMerger = propertyMerger;
	}

	public ChangeSet removeUnseenEntities(ChangeSet seen) {
		ChangeSet changes = new ChangeSet();

		Set<EntityID> seenIDs = seen.getChangedEntities();

		StandardEntity me = worldModel.getEntity(myself);
		Pair<Integer, Integer> location = me.getLocation(worldModel);
		// SpatialQuery humanQuery = SpatialQueryFactory.queryWithinDistance(
		// new PositionXY(location), viewDistance, Human.class);

		// Now check blockades
		// SpatialQuery blockadeQuery = SpatialQueryFactory.queryWithinDistance(
		// new PositionXY(location), viewDistance, Blockade.class);

		Collection<StandardEntity> potentialInRange = worldModel
				.getObjectsInRange(myself, viewDistance);

		Set<StandardEntity> inRange = new FastSet<StandardEntity>();

		// inRange.addAll(spatialIndex.query(humanQuery));
		// inRange.addAll(spatialIndex.query(blockadeQuery));

		for (StandardEntity standardEntity : potentialInRange) {
			if ((standardEntity instanceof Human || standardEntity instanceof Blockade)
					&& standardEntity.getLocation(worldModel) != null) {
				inRange.add(standardEntity);
			}
		}

		Collection<Line2D> blockingLines = getBlockingLines(potentialInRange);

		// Check if I should be able to see these.

		Set<StandardEntity> expectedButNotSeen = new FastSet<StandardEntity>();

		for (StandardEntity standardEntity : inRange) {
			if (seenIDs.contains(standardEntity.getID())) {
				continue;
			}
			Pair<Integer, Integer> otherLocation = standardEntity
					.getLocation(worldModel);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Checking " + standardEntity.getFullDescription());
			}

			Line2D lineToHuman = new Line2D(new Point2D(location.first(),
					location.second()), new Point2D(otherLocation.first(),
					otherLocation.second()));
			boolean blocked = false;
			for (Line2D line : blockingLines) {
				double d1 = line.getIntersection(lineToHuman);
				double d2 = lineToHuman.getIntersection(line);
				if (d2 >= 0 && d2 <= 1 && d1 >= 0 && d1 <= 1) {
					// blocked!
					blocked = true;
					break;
				}
			}
			if (!blocked) {
				expectedButNotSeen.add(standardEntity);
			}
		}

		for (StandardEntity unknown : expectedButNotSeen) {
			if (unknown instanceof Human) {
				processUnknownHuman(unknown, changes);
			} else {
				processUnknownBlockade(unknown, changes);
			}
		}

		return changes;
	}

	private void processUnknown(StandardEntity entity,
			List<String> propertyURNs, ChangeSet changed) {
		for (String propertyURN : propertyURNs) {
			Property property = entity.getProperty(propertyURN);
			if (property != null) {
				Property newProperty = property.copy();
				newProperty.undefine();
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Undefined " + newProperty + " of "
							+ entity.getFullDescription());
				}
				ProvenanceLogEntry entry = new ProvenanceLogEntry(timer
						.getTime(), InferredOrigin.INSTANCE, newProperty);
				worldModel.storeProvenance(entity.getID(), entry);
				Property decidedValue = propertyMerger.decideValue(worldModel
						.getProvenance(entity.getID(), propertyURN));
				if (!decidedValue.isDefined()) {
					if (property.isDefined()) {
						changed.addChange(entity.getID(), propertyURN,
								decidedValue);
					}
				}
				if (decidedValue.isDefined()) {
					property.takeValue(decidedValue);
				} else {
					property.undefine();
				}
			}
		}
	}

	private void processUnknownHuman(StandardEntity unknown, ChangeSet changed) {
		processUnknown(unknown, HUMAN_PROPERTIES, changed);
	}

	private void processUnknownBlockade(StandardEntity unknown,
			ChangeSet changed) {
		processUnknown(unknown, BLOCKADE_PROPERTIES, changed);
	}

	/**
	 * @param potentialInRange
	 * @param location
	 * @return
	 */
	private Collection<Line2D> getBlockingLines(
			Collection<StandardEntity> potentialInRange) {
		// SpatialQuery query = SpatialQueryFactory.queryWithinDistance(
		// new PositionXY(location), viewDistance, Building.class);
		// Collection<StandardEntity> inRange = spatialIndex.query(query);
		Collection<Line2D> result = new FastSet<Line2D>();

		for (StandardEntity se : potentialInRange) {
			if (se instanceof Building) {
				int[] apexes = ((Building) se).getApexList();
				List<Point2D> points = GeometryTools2D
						.vertexArrayToPoints(apexes);
				List<Line2D> lines = GeometryTools2D
						.pointsToLines(points, true);
				result.addAll(lines);
			}
		}

		return result;
	}

	static {
		HUMAN_PROPERTIES.add(StandardPropertyURN.POSITION.toString());
		HUMAN_PROPERTIES.add(StandardPropertyURN.X.toString());
		HUMAN_PROPERTIES.add(StandardPropertyURN.Y.toString());

		BLOCKADE_PROPERTIES.add(StandardPropertyURN.APEXES.toString());
		BLOCKADE_PROPERTIES.add(RoutingInfoBlockade.BLOCK_INFO_URN);
		BLOCKADE_PROPERTIES.add(StandardPropertyURN.EDGES.toString());
	}

}
