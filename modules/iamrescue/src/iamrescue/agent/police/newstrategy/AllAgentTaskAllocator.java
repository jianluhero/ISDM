package iamrescue.agent.police.newstrategy;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.execution.command.IPath;
import iamrescue.routing.WorldModelConverter;
import iamrescue.routing.WorldModelConverter.SimpleGraphNode;
import iamrescue.routing.WorldModelConverter.WorldModelArea;
import iamrescue.routing.costs.IRoutingCostFunction;
import iamrescue.routing.costs.PassableRoutingCostFunction;
import iamrescue.routing.dijkstra.DijkstrasShortestPath;
import iamrescue.routing.dijkstra.SimpleDijkstrasRoutingModule;
import iamrescue.routing.dijkstra.SimpleGraph;
import iamrescue.routing.util.ISpeedInfo;
import iamrescue.util.PositionXY;
import iamrescue.util.comparators.EntityIDComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.WorldModel;
import rescuecore2.worldmodel.WorldModelListener;

public class AllAgentTaskAllocator implements
		WorldModelListener<StandardEntity> {

	private FutureClearingRoutingModule clearingModule;// = new
	private Config config;
	private ISimulationTimer timer;
	private IAMWorldModel worldModel;
	private List<StandardEntity> otherAgents;
	private List<StandardEntity> policeForces;
	private List<StandardEntity> civilians;
	private SimpleDijkstrasRoutingModule freeRouting;
	private IRoutingCostFunction freeRoutingCostFunction;
	private List<StandardEntity> refuges;
	private ArrayList<Integer> refugeSourceList;
	private ArrayList<Double> refugeCostList;

	// What each police agent is working on.
	private EntityID[] workingOn;

	// My index in police forces list
	private int myIndex;

	// Predicted completion time of each goal currently attempted.
	private Map<EntityID, Integer> completionTimes = new FastMap<EntityID, Integer>();

	// FutureClearingRoutingModule(config,
	// timer, worldModel,
	// myID);

	public AllAgentTaskAllocator(IAMWorldModel worldModel,
			ISimulationTimer timer, Config config, EntityID myID,
			ISpeedInfo speedInfo) {
		clearingModule = new FutureClearingRoutingModule(config, timer,
				worldModel, speedInfo);
		this.worldModel = worldModel;
		this.timer = timer;
		this.config = config;
		this.freeRoutingCostFunction = new PassableRoutingCostFunction(1,
				Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, worldModel);
		this.freeRouting = new SimpleDijkstrasRoutingModule(worldModel,
				freeRoutingCostFunction, timer);
		// this.myID = myID;

		// Get all police agents
		policeForces = new ArrayList<StandardEntity>();
		policeForces.addAll(worldModel
				.getEntitiesOfType(StandardEntityURN.POLICE_FORCE));
		workingOn = new EntityID[policeForces.size()];

		for (int i = 0; i < workingOn.length; i++) {
			// Initially no-one is working on anything.
			workingOn[i] = null;
		}

		// Get other agents
		otherAgents = new ArrayList<StandardEntity>();
		otherAgents.addAll(worldModel.getEntitiesOfType(
				StandardEntityURN.AMBULANCE_TEAM,
				StandardEntityURN.FIRE_BRIGADE));

		// Civilians
		civilians = new ArrayList<StandardEntity>();
		civilians.addAll(worldModel
				.getEntitiesOfType(StandardEntityURN.CIVILIAN));

		refuges = new ArrayList<StandardEntity>();
		refuges.addAll(worldModel.getEntitiesOfType(StandardEntityURN.REFUGE));

		// Sort all
		Collections.sort(policeForces, EntityIDComparator.DEFAULT_INSTANCE);
		Collections.sort(otherAgents, EntityIDComparator.DEFAULT_INSTANCE);
		Collections.sort(civilians, EntityIDComparator.DEFAULT_INSTANCE);
		Collections.sort(refuges, EntityIDComparator.DEFAULT_INSTANCE);

		myIndex = Collections.binarySearch(policeForces, worldModel
				.getEntity(myID), new EntityIDComparator());

		worldModel.addWorldModelListener(this);

		// Extract converter
		WorldModelConverter converter = freeRouting.getConverter();

		// Get all sources
		Set<Integer> sources = new FastSet<Integer>();
		for (StandardEntity refuge : refuges) {
			WorldModelArea worldModelArea = converter.getWorldModelArea(refuge
					.getID().getValue());
			sources.addAll(worldModelArea.getSimpleNeighbours());
		}

		refugeSourceList = new ArrayList<Integer>(sources.size());
		refugeCostList = new ArrayList<Double>(sources.size());

		for (Integer id : sources) {
			refugeSourceList.add(id);
			refugeCostList.add(0.0);
		}
	}

	public IPath computeBestPath(PoliceForce me) {

		// Compute new cost function
		clearingModule.recomputeAll();

		// Allocate for all police agents
		for (int i = 0; i < policeForces.size(); i++) {

			SimpleGraph routingGraph = clearingModule.getRoutingGraph();
			WorldModelConverter converter = clearingModule.getConverter();

			// Work out start positions
			PoliceForce police = (PoliceForce) policeForces.get(i);
			if (!police.isPositionDefined()) {
				// skip
				continue;
			}

			Area position = (Area) worldModel.getEntity(police.getPosition());
			PositionXY xy = new PositionXY(police.getLocation(worldModel));

			List<Integer> startPositions = new FastList<Integer>();
			List<Double> startCosts = new FastList<Double>();

			Set<Integer> simpleGraphIDs = converter.getSimpleGraphIDs(police
					.getPosition().getValue());
			for (int id : simpleGraphIDs) {
				startPositions.add(i);
				clearingModule.getRoutingCostFunction().getTravelCost(
						position,
						xy,
						converter.getSimpleGraphNode(id)
								.getRepresentativePoint());

			}

			// DijkstrasShortestPath dijkstra = new
			// DijkstrasShortestPath(routingGraph, )

		}
		return null;
	}

	public StandardEntity computeClosestGoal(PoliceForce police) {
		// Generate goals
		GoalContainer generatedGoals = generateGoals();

		PositionXY myPosition = new PositionXY(police.getLocation(worldModel));

		double bestDistance = Double.POSITIVE_INFINITY;
		StandardEntity best = null;

		Collection<List<? extends StandardEntity>> potentialGoals = new FastList<List<? extends StandardEntity>>();
		potentialGoals.add(generatedGoals.getBlockedAgents());
		potentialGoals.add(generatedGoals.getBlockedCivilians());
		potentialGoals.add(generatedGoals.getBlockedBurningBuildings());
		potentialGoals.add(generatedGoals.getBlockedUnsearchedBuildings());

		// Clear all remaining blockades.
		/*
		 * FastList<StandardEntity> blockades = new FastList<StandardEntity>();
		 * for(StandardEntity se : blockades) { new
		 * FastList<StandardEntity>(worldModel
		 * .getEntitiesOfType(StandardEntityURN.BLOCKADE); }
		 * 
		 * potentialGoals.add(blockades);
		 */

		potentialGoals.add(new FastList<StandardEntity>(worldModel
				.getEntitiesOfType(StandardEntityURN.BLOCKADE)));

		// Compute closest goal, in order of lists
		for (Collection<? extends StandardEntity> list : potentialGoals) {
			for (StandardEntity se : list) {
				// if (se instanceof Blockade) {
				// if (!((Blockade) se).isPositionDefined()) {
				// This happens when the position of a blockade is
				// unknown (e.g., due to inconsistent updates)
				// continue;
				// }
				// }
				double distance = myPosition.distanceTo(new PositionXY(se
						.getLocation(worldModel)));
				if (distance < bestDistance) {
					bestDistance = distance;
					best = se;
				}
			}
			if (best != null) {
				return best;
			}
		}
		return null;
	}

	/**
	 * 
	 */
	public GoalContainer generateGoals() {

		// Extract graph
		SimpleGraph graph = freeRouting.getGraph();

		WorldModelConverter converter = freeRouting.getConverter();

		// Run complete Dijkstra
		DijkstrasShortestPath dijkstra = new DijkstrasShortestPath(graph,
				refugeSourceList, refugeCostList);

		dijkstra.forceFullCompute();

		GoalContainer goals = new GoalContainer();

		List<StandardEntity> agents = new FastList<StandardEntity>();
		agents.addAll(policeForces);
		agents.addAll(otherAgents);

		List<StandardEntity> agentsToClear = extractGoals(agents, dijkstra,
				converter);
		for (StandardEntity agent : agentsToClear) {
			goals.addAgent((Human) agent);
		}

		List<StandardEntity> civiliansToClear = extractGoals(civilians,
				dijkstra, converter);
		for (StandardEntity civilian : civiliansToClear) {
			goals.addCivilian((Civilian) civilian);
		}

		List<EntityID> unsearched = new FastList<EntityID>();
		unsearched.addAll(worldModel.getUnknownBuildings());
		unsearched.addAll(worldModel.getSafeUnsearchedBuildings());
		for (EntityID entityID : unsearched) {
			StandardEntity se = worldModel.getEntity(entityID);
			if (needsClearing(se, dijkstra, converter)) {
				goals.addUnsearchedBuilding((Building) se);
			}
		}

		// TODO: Add burning buildings here

		return goals;
	}

	private List<StandardEntity> extractGoals(
			List<StandardEntity> potentialTargets,
			DijkstrasShortestPath dijkstra, WorldModelConverter converter) {
		List<StandardEntity> needClearing = new FastList<StandardEntity>();
		for (StandardEntity standardEntity : potentialTargets) {
			if (needsClearing(standardEntity, dijkstra, converter)) {
				needClearing.add(standardEntity);
			}
		}
		return needClearing;
	}

	private boolean needsClearing(StandardEntity poi,
			DijkstrasShortestPath dijkstra, WorldModelConverter converter) {
		StandardEntity position;
		if (poi instanceof Human) {
			Human human = (Human) poi;
			if (!(human.isPositionDefined() && human.isHPDefined() && human
					.getHP() > 0)) {
				// Ignore if position undefined or dead
				return false;
			}
			position = worldModel.getEntity(human.getPosition());
			if (position instanceof Human) {
				// Loaded on ambulance?
				Human h = (Human) position;
				if (h.isPositionDefined()) {
					position = worldModel.getEntity(h.getPosition());
				} else {
					return false;
				}
			}
		} else {
			position = poi;
		}

		// Get position
		WorldModelArea worldModelArea = converter.getWorldModelArea(position
				.getID().getValue());
		Set<Integer> simpleNeighbours = worldModelArea.getSimpleNeighbours();
		// Need to check simple neighbours
		boolean free = false;
		for (int neighbour : simpleNeighbours) {
			// Check if free
			// System.out.println("Cost to " + neighbour + ": "
			// + dijkstra.getCost(neighbour));
			if (dijkstra.getCost(neighbour) < Double.POSITIVE_INFINITY) {
				if (poi instanceof Human) {
					SimpleGraphNode simpleGraphNode = converter
							.getSimpleGraphNode(neighbour);
					// Now check if possible to reach agent on node
					PositionXY representativePoint = simpleGraphNode
							.getRepresentativePoint();
					double travelCost = freeRoutingCostFunction.getTravelCost(
							(Area) position, representativePoint,
							new PositionXY(poi.getLocation(worldModel)));
					// System.out.println("Cost between " + representativePoint
					// + " and "
					// + new PositionXY(poi.getLocation(worldModel))
					// + ": " + travelCost);
					if (travelCost < Double.POSITIVE_INFINITY) {
						free = true;
						break;
					}
				} else {
					free = true;
					break;
				}
			}
		} // Done checking
		return !free;
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
		if (e instanceof Civilian) {
			civilians.add(e);
			Collections.sort(civilians, new EntityIDComparator());
		}
	}

	/**
	 * @return the clearingModule
	 */
	public FutureClearingRoutingModule getClearingModule() {
		return clearingModule;
	}

	@Override
	public void entityRemoved(WorldModel<? extends StandardEntity> model,
			StandardEntity e) {
		// Shouldn't happen
	}
}
