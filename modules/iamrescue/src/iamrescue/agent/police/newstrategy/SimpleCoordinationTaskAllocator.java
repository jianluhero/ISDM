/**
 * 
 */
package iamrescue.agent.police.newstrategy;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.util.PositionXY;
import iamrescue.util.comparators.EntityIDComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javolution.util.FastSet;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class SimpleCoordinationTaskAllocator {

	private IAMWorldModel worldModel;
	private List<PoliceForce> policeAgents;
	private double agentUtility;
	private double civilianUtility;
	private double unsearchedUtility;
	private double burningUtility;
	private ISimulationTimer timer;

	// Assume clearing takes 2x the time of moving
	private double clearingProportion = 2;

	public SimpleCoordinationTaskAllocator(IAMWorldModel worldModel,
			ISimulationTimer timer) {
		this.worldModel = worldModel;
		Collection<StandardEntity> policeCollection = worldModel
				.getEntitiesOfType(StandardEntityURN.POLICE_FORCE);
		policeAgents = new ArrayList<PoliceForce>(policeCollection.size());
		for (StandardEntity police : policeCollection) {
			policeAgents.add((PoliceForce) police);
		}
		Collections.sort(policeAgents, EntityIDComparator.DEFAULT_INSTANCE);
		this.timer = timer;
		setDefaultUtilities();
	}

	/**
	 * @param agentUtility
	 *            the agentUtility to set
	 */
	public void setAgentUtility(double agentUtility) {
		this.agentUtility = agentUtility;
	}

	/**
	 * @param civilianUtility
	 *            the civilianUtility to set
	 */
	public void setCivilianUtility(double civilianUtility) {
		this.civilianUtility = civilianUtility;
	}

	/**
	 * @param unsearchedUtility
	 *            the unsearchedUtility to set
	 */
	public void setUnsearchedUtility(double unsearchedUtility) {
		this.unsearchedUtility = unsearchedUtility;
	}

	/**
	 * @param clearingProportion
	 *            the clearingProportion to set
	 */
	public void setClearingProportion(double clearingProportion) {
		this.clearingProportion = clearingProportion;
	}

	/**
	 * @param burningUtility
	 *            the burningUtility to set
	 */
	public void setBurningUtility(double burningUtility) {
		this.burningUtility = burningUtility;
	}

	/**
	 * 
	 */
	private void setDefaultUtilities() {
		agentUtility = 10;
		civilianUtility = 5;
		burningUtility = 1;
		unsearchedUtility = .1;
	}

	public List<EntityID> getNextTargets(GoalContainer goals, PoliceForce police) {
		// Greedily assign tasks
		// Map<EntityID, Integer> clearingCosts = new FastMap<EntityID,
		// Integer>();
		// Map<EntityID, EntityID> alreadyClearing = new FastMap<EntityID,
		// EntityID>();

		// Police info
		List<PoliceInfo> policeInfo = new ArrayList<PoliceInfo>(policeAgents
				.size());

		// Target info
		List<TargetInfo> targetInfo = new ArrayList<TargetInfo>();

		for (int i = 0; i < policeAgents.size(); i++) {
			PoliceForce policeForce = policeAgents.get(i);
			if (policeForce.isPositionDefined()) {
				Pair<Integer, Integer> location = policeForce
						.getLocation(worldModel);
				if (location != null) {
					policeInfo.add(new PoliceInfo(policeForce.getID(),
							new PositionXY(location)));
				}
			}
		}

		// Now add target info
		List<Human> blockedAgents = goals.getBlockedAgents();
		for (Human human : blockedAgents) {
			targetInfo.add(new TargetInfo(human.getID(), new PositionXY(human
					.getLocation(worldModel)), agentUtility));
		}

		List<Civilian> blockedCivilians = goals.getBlockedCivilians();
		for (Civilian civilian : blockedCivilians) {
			targetInfo.add(new TargetInfo(civilian.getID(), new PositionXY(
					civilian.getLocation(worldModel)), civilianUtility));
		}

		List<Building> blockedUnsearchedBuildings = goals
				.getBlockedUnsearchedBuildings();
		for (Building building : blockedUnsearchedBuildings) {
			targetInfo.add(new TargetInfo(building.getID(), new PositionXY(
					building.getLocation(worldModel)), unsearchedUtility));
		}

		List<Building> blockedBurningBuildings = goals
				.getBlockedBurningBuildings();
		for (Building building : blockedBurningBuildings) {
			targetInfo.add(new TargetInfo(building.getID(), new PositionXY(
					building.getLocation(worldModel)), burningUtility));
		}

		List<ClearingPlan> plans = new ArrayList<ClearingPlan>();
		Set<PoliceInfo> unallocatedPolice = new FastSet<PoliceInfo>();
		unallocatedPolice.addAll(policeInfo);
		Set<TargetInfo> unallocatedTasks = new FastSet<TargetInfo>();
		unallocatedTasks.addAll(targetInfo);

		boolean changed = true;
		double utility = 0;

		while (changed) {
			changed = false;
			for (int i = 0; i < policeInfo.size(); i++) {
				PoliceInfo p = policeInfo.get(i);
				// Is this allocated?
				if (unallocatedPolice.contains(p)) {
					// Unallocated
					// What happens if we add this to another plan?
					double bestUtilityGain = 0;
					ClearingPlan bestExistingPlan = null;
					for (int j = 0; j < plans.size(); j++) {
						double utilityRateBefore = plans.get(j).utilityRate;
						double utilityRateAfter = plans.get(j)
								.checkUtilityRateWithNewAgent(p);
						double improvement = utilityRateAfter
								- utilityRateBefore;
						if (improvement > bestUtilityGain) {
							bestUtilityGain = improvement;
							bestExistingPlan = plans.get(j);
						}
					}

					ClearingPlan bestNewPlan = null;
					// What if we create a new plan from any remaining target
					for (int j = 0; j < targetInfo.size(); j++) {
						TargetInfo info = targetInfo.get(j);
						if (unallocatedTasks.contains(info)) {
						//	con
						}
						ClearingPlan plan = new ClearingPlan(p, info);
						double improvement = plan.utilityRate;
						if (improvement > bestUtilityGain) {
							bestUtilityGain = improvement;
							bestNewPlan = plan;
						}
					}
					if (bestUtilityGain > 0) {
						changed = true;
						if (bestNewPlan != null) {
							p.allocatedPlan = bestNewPlan;
							plans.add(bestNewPlan);
							utility += bestUtilityGain;
							unallocatedPolice.remove(p);
						//	unallocated
						}
					}
				} else {
					// Already allocated!

					// Don't change for now

				}
			}
		}
		return null;
	}

	private class ClearingPlan {
		private List<TargetInfo> targets;
		private double mainDistance;
		private double furthestStartTravelClearingCost;
		private List<PoliceInfo> assignedPolice;
		private PoliceInfo mainAgent;
		private double utility;
		private double utilityRate;

		public ClearingPlan(PoliceInfo agent, TargetInfo target) {
			mainAgent = agent;
			assignedPolice = new ArrayList<PoliceInfo>();
			assignedPolice.add(agent);
			targets = new ArrayList<TargetInfo>();
			targets.add(target);

			// compute distance
			double distance = mainAgent.position.distanceTo(target.position);
			mainDistance = distance;
			furthestStartTravelClearingCost = 0;

			utility = target.utility;
			utilityRate = computeUtilityRate();
		}

		public double checkUtilityRateWithNewAgent(PoliceInfo helping) {
			double costToStart = (clearingProportion + 1)
					* helping.position.distanceTo(mainAgent.position);
			double newCost = mainDistance + (clearingProportion * mainDistance)
					/ (assignedPolice.size() + 1);
			if (costToStart < furthestStartTravelClearingCost) {
				newCost += costToStart;
			}
			return utility / newCost;
		}

		public double checkUtilityRateWithNewTargetAtEnd(TargetInfo info) {
			double extraDistance = info.position.distanceTo(targets.get(targets
					.size() - 1).position);
			double newDistance = mainDistance + extraDistance;
			double newCost = newDistance + newDistance
					* (clearingProportion / assignedPolice.size());
			double newUtility = utility + info.utility;
			return newUtility / newCost;
		}

		public void addNewTargetAtEnd(TargetInfo info) {
			double extraDistance = info.position.distanceTo(targets.get(targets
					.size() - 1).position);
			mainDistance += extraDistance;
			utility += info.utility;
			targets.add(info);
			utilityRate = computeUtilityRate();
		}

		public void addNewTargetAtStart(TargetInfo info) {
			double extraDistance = info.position
					.distanceTo(targets.get(0).position);
			extraDistance -= mainAgent.position
					.distanceTo(targets.get(0).position);
			extraDistance += mainAgent.position.distanceTo(info.position);
			mainDistance += extraDistance;
			utility += info.utility;
			targets.add(0, info);
			utilityRate = computeUtilityRate();
		}

		public double checkUtilityRateWithNewTargetAtStart(TargetInfo info) {
			double extraDistance = info.position
					.distanceTo(targets.get(0).position);
			extraDistance -= mainAgent.position
					.distanceTo(targets.get(0).position);
			extraDistance += mainAgent.position.distanceTo(info.position);
			double newDistance = mainDistance + extraDistance;
			double newCost = newDistance + newDistance
					* (clearingProportion / assignedPolice.size());
			double newUtility = utility + info.utility;
			return newUtility / newCost;
		}

		public void addNewAgent(PoliceInfo helping) {
			double costToStart = (clearingProportion + 1)
					* helping.position.distanceTo(mainAgent.position);
			if (costToStart < furthestStartTravelClearingCost) {
				furthestStartTravelClearingCost = costToStart;
			}
			assignedPolice.add(helping);
			utilityRate = computeUtilityRate();
		}

		public double computeUtilityRate() {
			double cost = mainDistance + (clearingProportion * mainDistance)
					/ assignedPolice.size();
			cost += furthestStartTravelClearingCost;
			return utility / cost;
		}

		@Override
		public int hashCode() {
			return mainAgent.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof ClearingPlan)) {
				return false;
			}
			ClearingPlan c = (ClearingPlan) obj;
			return mainAgent.id.equals(c.mainAgent.id);
		}

	}

	private class TargetInfo {
		private EntityID id;
		private PositionXY position;
		private double utility;

		public TargetInfo(EntityID id, PositionXY position, double utility) {
			this.id = id;
			this.position = position;
			this.utility = utility;
		}

		public int hashCode() {
			return id.hashCode();
		}

		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof TargetInfo)) {
				return false;
			}
			TargetInfo t = (TargetInfo) obj;
			return id.equals(t.id);
		}
	}

	private class PoliceInfo {
		private EntityID id;
		private PositionXY position;
		private ClearingPlan allocatedPlan;

		public PoliceInfo(EntityID id, PositionXY position) {
			super();
			this.id = id;
			this.position = position;
			this.allocatedPlan = null;
		}

		public int hashCode() {
			return id.hashCode();
		}

		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof PoliceInfo)) {
				return false;
			}
			PoliceInfo p = (PoliceInfo) obj;
			return id.equals(p.id);
		}

	}
}