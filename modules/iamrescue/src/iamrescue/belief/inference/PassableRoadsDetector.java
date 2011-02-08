package iamrescue.belief.inference;

import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.entities.BlockInfoRoad;
import iamrescue.belief.spatial.ISpatialIndex;
import iamrescue.belief.spatial.SpatialQueryFactory;
import iamrescue.util.PositionXY;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

/**
 * Detects passable roads based on an agent's position history.
 * 
 * @author Sebastian
 * 
 */
public class PassableRoadsDetector {
	private IAMWorldModel worldModel;
	private static final boolean CONSIDER_ONLY_YOURSELF = true;
	private static final ChangeSet EMPTY_CHANGE_SET = new ChangeSet();

	// Keeps track of when positions were last inferred for a given entity.
	private Map<EntityID, Integer> lastUpdate = new FastMap<EntityID, Integer>();

	// private ISpatialIndex spatial;
	private EntityID myself;
	private boolean skip = false;

	public PassableRoadsDetector(IAMWorldModel worldModel, EntityID myself) {
		this.worldModel = worldModel;
		// this.spatial = spatial;
		this.myself = myself;
		if (!(worldModel.getEntity(myself) instanceof Human)
				&& CONSIDER_ONLY_YOURSELF) {
			skip = true;
		}
		worldModel.index();

	}

	public ChangeSet inferPassableRoads() {

		if (skip) {
			return EMPTY_CHANGE_SET;
		}

		ChangeSet changed = new ChangeSet();

		// Get all humans
		Collection<StandardEntity> humans;

		if (CONSIDER_ONLY_YOURSELF) {
			humans = Collections.singletonList(worldModel.getEntity(myself));
		} else {
			humans = worldModel.getEntitiesOfType(
					StandardEntityURN.AMBULANCE_TEAM,
					StandardEntityURN.POLICE_FORCE,
					StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.CIVILIAN);
		}

		// Get position histories
		for (StandardEntity standardEntity : humans) {
			// if (standardEntity instanceof Human) {
			Human human = (Human) standardEntity;
			if (human.isPositionHistoryDefined()) {
				// Check when this was done
				Integer previous = lastUpdate.get(human.getID());
				int timeStep = worldModel.getProvenance(human.getID(),
						StandardPropertyURN.POSITION_HISTORY).getLatest()
						.getTimeStep();
				if (previous == null || previous != timeStep) {
					// Consider this
					processHuman(human, changed);
					lastUpdate.put(human.getID(), timeStep);
				}
				// }
			}
		}

		return changed;
	}

	private void processHuman(Human human, ChangeSet changed) {
		int[] positionHistory = human.getPositionHistory();
		List<StandardEntity> traversed = new FastList<StandardEntity>();
		for (int i = 0; i < positionHistory.length; i = i + 2) {
			/*
			 * Collection<StandardEntity> result = spatial
			 * .query(SpatialQueryFactory.queryWithinDistance( new
			 * PositionXY(positionHistory[i], positionHistory[i + 1]), 0,
			 * BlockInfoRoad.class));
			 */

			Collection<StandardEntity> result = worldModel.getObjectsInRange(
					positionHistory[i], positionHistory[i + 1], 0);
			for (StandardEntity standardEntity : result) {
				if (standardEntity instanceof BlockInfoRoad) {
					traversed.add(standardEntity);
				}
			}

		}

		// Mark all passed roads - if not, this
		// should be inferred through the actual blockades seen (or communicated
		// by the relevant agent).

		for (StandardEntity se : traversed) {
			if (se instanceof Road) {
				assert se instanceof BlockInfoRoad;
				BlockInfoRoad road = (BlockInfoRoad) se;
				if (!road.isBlockadesDefined()) {
					if (!road.isHasBeenPassedDefined() || !road.hasBeenPassed()) {
						road.setHasBeenPassed(true);
						changed
								.addChange(road, road
										.getHasBeenPassedProperty());
					}
				}
			}
		}
	}
}
