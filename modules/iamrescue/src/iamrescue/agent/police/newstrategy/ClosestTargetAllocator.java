package iamrescue.agent.police.newstrategy;

import iamrescue.belief.IAMWorldModel;
import iamrescue.util.comparators.IDComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.EntityID;

public class ClosestTargetAllocator {
	private static final int MAX_AGENTS_PER_TARGET = 4;

	private IAMWorldModel worldModel;
	private List<EntityID> police;
	private List<StandardEntity> refuges;

	public ClosestTargetAllocator(IAMWorldModel worldModel) {
		this.worldModel = worldModel;
		Collection<StandardEntity> policeCollection = worldModel
				.getEntitiesOfType(StandardEntityURN.POLICE_FORCE);
		police = new ArrayList<EntityID>(policeCollection.size());
		for (StandardEntity standardEntity : policeCollection) {
			police.add(standardEntity.getID());
		}
		Collections.sort(police, IDComparator.DEFAULT_INSTANCE);
		Collection<StandardEntity> refugeCollection = worldModel
				.getEntitiesOfType(StandardEntityURN.REFUGE);
		refuges = new ArrayList<StandardEntity>(refugeCollection.size());
		for (StandardEntity standardEntity : refugeCollection) {
			refuges.add(standardEntity);
		}
	}

	public EntityID computeAllocation(List<StandardEntity> stuckPositions,
			Set<EntityID> stuckAgents, GoalGenerator goalGenerator,
			EntityID myself) {
		// Collections.sort(stuckAgents, IDComparator.DEFAULT_INSTANCE);

		Map<EntityID, Integer> alreadyAllocated = new FastMap<EntityID, Integer>();

		for (int i = 0; i < police.size(); i++) {
			Human thisPolice = (Human)worldModel.getEntity(police.get(i));
			if (!thisPolice.isPositionDefined()) {
				System.out.println("Crap: " + thisPolice.getFullDescription());
				System.out.println(worldModel.getProvenance(thisPolice.getID(), StandardPropertyURN.POSITION));
				continue;
			}
			EntityID closest = findClosestUnsaturatedGoal(stuckPositions,
					Collections.EMPTY_SET, alreadyAllocated, thisPolice);
			// EntityID closest = null;
			GoalContainer goals = null;
			if (closest == null) {
				goals = goalGenerator.generateGoals();
				closest = findClosestUnsaturatedGoal(goals.getBlockedAgents(),
						stuckAgents, alreadyAllocated, thisPolice);
			}
			if (closest == null) {
				closest = findClosestUnsaturatedGoal(goals
						.getBlockedCivilians(), stuckAgents, alreadyAllocated,
						thisPolice);
			}
			if (closest == null) {
				closest = findClosestUnsaturatedGoal(goals
						.getBlockedUnsearchedBuildings(), stuckAgents,
						alreadyAllocated, thisPolice);
			}
			if (closest == null) {
				closest = findClosestUnsaturatedGoal(goals
						.getBlockedBurningBuildings(), stuckAgents,
						alreadyAllocated, thisPolice);
			}
			if (thisPolice.getID().equals(myself)) {
				return closest;
			}
		}
		return null;
	}

	private EntityID findClosestUnsaturatedGoal(
			List<? extends StandardEntity> targets, Set<EntityID> ignored,
			Map<EntityID, Integer> alreadyAllocated, StandardEntity source) {
		double closestSquaredDistance = Double.MAX_VALUE;
		StandardEntity closest = null;

		Pair<Integer, Integer> locationSource = source.getLocation(worldModel);

		for (StandardEntity target : targets) {
			if (ignored.contains(target.getID())) {
				// Ignore this
				continue;
			}
			if (target instanceof PoliceForce) {
				if (!target.getID().equals(source.getID())) {
					// Do not help other police forces
					continue;
				} else {
					// Add closest refuge to self
					for (StandardEntity refuge : refuges) {
						Integer already = alreadyAllocated.get(refuge.getID());
						if (already == null || already < MAX_AGENTS_PER_TARGET) {
							Pair<Integer, Integer> location = refuge
									.getLocation(worldModel);
							double dx = location.first()
									- locationSource.first();
							double dy = location.second()
									- locationSource.second();
							double squaredDistance = dx * dx + dy * dy;
							if (squaredDistance < closestSquaredDistance) {
								closestSquaredDistance = squaredDistance;
								closest = refuge;
							}
						}
					}
				}
			} else {
				// Not police
				Integer already = alreadyAllocated.get(target.getID());
				if (already == null || already < MAX_AGENTS_PER_TARGET) {
					Pair<Integer, Integer> location = target
							.getLocation(worldModel);
					if (location == null) {
						System.out.println("Crap: " + target.getFullDescription());
					}
					double dx = location.first() - locationSource.first();
					double dy = location.second() - locationSource.second();
					double squaredDistance = dx * dx + dy * dy;
					if (squaredDistance < closestSquaredDistance) {
						closestSquaredDistance = squaredDistance;
						closest = target;
					}
				}
			}
		}

		if (closest != null) {
			Integer already = alreadyAllocated.get(closest.getID());
			if (already == null) {
				already = 0;
			}
			alreadyAllocated.put(closest.getID(), already + 1);
		}
		if (closest == null) {
			return null;
		} else {
			return closest.getID();
		}
	}
}
