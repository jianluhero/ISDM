/**
 * 
 */
package iamrescue.agent.police;

import iamrescue.agent.ISimulationTimer;
import iamrescue.agent.police.goals.CivilianClearingGoal;
import iamrescue.agent.police.goals.ClearingGoalConfiguration;
import iamrescue.agent.police.goals.FireClearingGoal;
import iamrescue.agent.police.goals.PlatoonClearingGoal;
import iamrescue.agent.police.goals.PoliceClearingGoal;
import iamrescue.agent.police.goals.SimpleClearingGoal;
import iamrescue.belief.IAMWorldModel;
import iamrescue.routing.IRoutingModule;
import iamrescue.routing.costs.PassableRoutingCostFunction;
import iamrescue.routing.costs.SimpleDistanceRoutingCostFunction;
import iamrescue.routing.dijkstra.BidirectionalDijkstrasRoutingModule;
import iamrescue.util.OptimalAssignmentCalculator;
import iamrescue.util.comparators.IDComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.EntityListener;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.WorldModel;
import rescuecore2.worldmodel.WorldModelListener;

/**
 * @author Sebastian
 */
public class PoliceTaskModule implements EntityListener,
		WorldModelListener<StandardEntity> {

	private static final double VERY_LARGE_COST = 10000000000.0;

	private static final double VERY_SMALL_COST = 0.00000000000001;

	private SimpleClearingGoal myCurrentGoal = null;

	private Set<SimpleClearingGoal> unassignedGoals = new FastSet<SimpleClearingGoal>();
	private Map<SimpleClearingGoal, EntityID> assignedPolice = new FastMap<SimpleClearingGoal, EntityID>();;
	private List<EntityID> unassignedAgents = new FastList<EntityID>();
	private List<EntityID> refuges = new FastList<EntityID>();

	private IAMWorldModel worldModel;

	// private IRoutingModule normalRouting;
	private IRoutingModule possibleRouting;

	private EntityID myself;

	private IRoutingModule clearingRouting;

	private ClearingGoalConfiguration config;

	public PoliceTaskModule(IAMWorldModel worldModel, EntityID myself,
			IRoutingModule normalRoutingModule, ISimulationTimer timer) {
		this.worldModel = worldModel;
		this.myself = myself;
		possibleRouting = new BidirectionalDijkstrasRoutingModule(worldModel,
				new PassableRoutingCostFunction(worldModel), timer);
		clearingRouting = new BidirectionalDijkstrasRoutingModule(worldModel,
				new SimpleDistanceRoutingCostFunction(worldModel, true), timer);
		config = new ClearingGoalConfiguration(possibleRouting,
				clearingRouting, worldModel, false);
		worldModel.addWorldModelListener(this);
	}

	public void initialise() {
		for (StandardEntity se : worldModel
				.getEntitiesOfType(StandardEntityURN.REFUGE)) {
			refuges.add(se.getID());
		}

		for (StandardEntity se : worldModel
				.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
			unassignedAgents.add(se.getID());
		}

		// Sort by ID
		Collections.sort(refuges, IDComparator.DEFAULT_INSTANCE);
		Collections.sort(unassignedAgents, IDComparator.DEFAULT_INSTANCE);

		generatePoliceGoals();
		generateOtherAgentGoals();
		generateFireGoals();
		// generateRefugeGoals();

		update();
	}

	/**
	 * This should be called every turn
	 */
	public void update() {
		updateAssignedTasks();

		// First check if goals have been achieved
		removeDoneTasks();

		updateAndFilterUnassignedTasks();

		// Now do new assignment
		allocateTasks();
	}

	private void updateAndFilterUnassignedTasks() {
		Iterator<SimpleClearingGoal> iterator = unassignedGoals.iterator();
		while (iterator.hasNext()) {
			SimpleClearingGoal goal = iterator.next();
			goal.evaluateCurrentState();
			if (goal.isDone()) {
				iterator.remove();
			}
		}
	}

	private void updateAssignedTasks() {
		Iterator<Entry<SimpleClearingGoal, EntityID>> assignedIt = assignedPolice
				.entrySet().iterator();
		while (assignedIt.hasNext()) {
			Entry<SimpleClearingGoal, EntityID> entry = assignedIt.next();
			entry.getKey().evaluateCurrentState();
		}
	}

	/**
	 * @return the myCurrentGoal
	 */
	public SimpleClearingGoal getMyCurrentGoal() {
		return myCurrentGoal;
	}

	/**
	 * Removes all tasks that have been achieved.
	 */
	private void removeDoneTasks() {
		Iterator<Entry<SimpleClearingGoal, EntityID>> assignedIt = assignedPolice
				.entrySet().iterator();
		while (assignedIt.hasNext()) {
			Entry<SimpleClearingGoal, EntityID> entry = assignedIt.next();
			if (entry.getKey().isDone()) {
				EntityID agent = entry.getValue();
				if (agent.equals(myself)) {
					myCurrentGoal = null;
				}
				unassignedAgents.add(agent);
				assignedIt.remove();
			}
		}
	}

	/**
	 * Allocates idle police agents to new tasks
	 */
	private void allocateTasks() {

		if (unassignedAgents.size() == 0 || unassignedGoals.size() == 0) {
			return;
		}
		List<PotentialGoal> goals = new ArrayList<PotentialGoal>(
				unassignedGoals.size());
		Iterator<SimpleClearingGoal> goalIt = unassignedGoals.iterator();
		while (goalIt.hasNext()) {
			SimpleClearingGoal goal = goalIt.next();
			double utility = goal.getCurrentUtility();
			if (utility > 0) {
				goals.add(new PotentialGoal(goal, utility));
			}
		}

		// Now sort by priority and select only top goals
		Collections.sort(goals, GoalComparator.DEFAULT_INSTANCE);
		goals = goals.subList(goals.size() - unassignedAgents.size(), goals
				.size());

		// Calculate assignment
		double[][] costs = new double[goals.size()][unassignedAgents.size()];
		int offset = 0;
		for (int i = 0; i < costs.length; i++) {
			SimpleClearingGoal goal = goals.get(i + offset).goal;
			StandardEntity target = worldModel.getEntity(goal.getTarget());
			// boolean foundFeasible = false;
			for (int j = 0; j < costs[i].length; j++) {
				StandardEntity agent = worldModel.getEntity(unassignedAgents
						.get(j));
				double cost = goal.getCost((PoliceForce) agent);
				if (!possibleRouting
						.areConnected(agent.getID(), target.getID())
						|| cost == Double.POSITIVE_INFINITY) {
					cost = VERY_LARGE_COST;
				} else {
					if (!target.getID().equals(agent.getID())) {
						cost += VERY_SMALL_COST * agent.getID().getValue();
					}
				}
				costs[i][j] = cost;
			}
		}

		int[] assignment = OptimalAssignmentCalculator
				.calculateOptimalAssignment(costs);

		for (int i = assignment.length - 1; i >= 0; i--) {
			EntityID agent = unassignedAgents.remove(assignment[i]);
			SimpleClearingGoal goal = goals.get(i).goal;
			unassignedGoals.remove(goal);
			assignedPolice.put(goal, agent);
			if (agent.equals(myself)) {
				myCurrentGoal = goal;
			}
		}
	}

	/**
     * 
     */
	// private void generateRefugeGoals() {
	// Pick refuge with lowest ID
	// unassignedGoals.add(new RefugeConnectionGoal(refuges.get(0)));
	// }

	/**
     * 
     */
	private void generateFireGoals() {
		Collection<StandardEntity> entitiesOfType = worldModel
				.getEntitiesOfType(StandardEntityURN.BUILDING,
						StandardEntityURN.FIRE_STATION,
						StandardEntityURN.POLICE_OFFICE,
						StandardEntityURN.AMBULANCE_CENTRE);
		for (StandardEntity standardEntity : entitiesOfType) {
			Building b = (Building) standardEntity;
			b.addEntityListener(this);
			processBuilding(b);
		}
	}

	private void processBuilding(Building b) {
		if (b.isFierynessDefined() && b.getFieryness() > 0) {
			unassignedGoals.add(new FireClearingGoal(b.getID(), config));
		}
		b.removeEntityListener(this);
	}

	/**
     * 
     */
	private void generateOtherAgentGoals() {
		Collection<StandardEntity> others = worldModel.getEntitiesOfType(
				StandardEntityURN.AMBULANCE_TEAM,
				StandardEntityURN.FIRE_BRIGADE);
		for (StandardEntity se : others) {
			unassignedGoals.add(new PlatoonClearingGoal(se.getID(), config));
		}
	}

	/**
     * 
     */
	private void generatePoliceGoals() {
		Collection<StandardEntity> police = worldModel
				.getEntitiesOfType(StandardEntityURN.POLICE_FORCE);
		for (StandardEntity se : police) {
			unassignedGoals.add(new PoliceClearingGoal(se.getID(), config));
		}
	}

	private static class PotentialGoal {
		/**
		 * @param goal
		 * @param utility
		 */
		public PotentialGoal(SimpleClearingGoal goal, double utility) {
			this.goal = goal;
			this.utility = utility;
		}

		private SimpleClearingGoal goal;
		private double utility;
	}

	private static class GoalComparator implements Comparator<PotentialGoal> {

		public static final GoalComparator DEFAULT_INSTANCE = new GoalComparator();
		private static final IDComparator ID_COMPARATOR = new IDComparator();

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(PotentialGoal goal1, PotentialGoal goal2) {
			if (goal1.utility < goal2.utility) {
				return -1;
			} else if (goal1.utility > goal2.utility) {
				return 1;
			} else {
				return ID_COMPARATOR.compare(goal1.goal.getTarget(), goal2.goal
						.getTarget());
			}
		}
	}

	@Override
	public void propertyChanged(Entity e, Property p, Object oldValue,
			Object newValue) {
		if (e instanceof Building) {
			processBuilding((Building) e);
		} else if (e instanceof Civilian) {
			processCivilian((Civilian) e);
		}
	}

	@Override
	public void entityAdded(WorldModel<? extends StandardEntity> model,
			StandardEntity e) {
		if (e instanceof Civilian) {
			e.addEntityListener(this);
			processCivilian((Civilian) e);
		}
	}

	private void processCivilian(Civilian e) {
		if (e.isPositionDefined()) {
			unassignedGoals.add(new CivilianClearingGoal(e.getID(), config));
			e.removeEntityListener(this);
		}
	}

	@Override
	public void entityRemoved(WorldModel<? extends StandardEntity> model,
			StandardEntity e) {
		e.removeEntityListener(this);
	}
}
