/**
 * 
 */
package iamrescue.agent.police.newstrategy;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.routing.AbstractRoutingModule;
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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastSet;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.WorldModel;
import rescuecore2.worldmodel.WorldModelListener;

/**
 * @author Sebastian
 * 
 */
public class GoalGenerator implements WorldModelListener<StandardEntity> {

	private AbstractRoutingModule freeRouting;
	private List<Integer> refugeSourceList;
	private List<Double> refugeCostList;
	private List<StandardEntity> policeForces;
	private List<StandardEntity> ambulanceAgents;
	private List<StandardEntity> fireBrigadeAgents;
	private List<StandardEntity> civilians;
	private IAMWorldModel worldModel;
	private IRoutingCostFunction freeRoutingCostFunction;
	private ISimulationTimer timer;
	private List<StandardEntity> refuges;
	private Config config;
	private ISpeedInfo speedInfo;
	private EntityID myID;

	public GoalGenerator(IAMWorldModel worldModel, ISimulationTimer timer,
			Config config, ISpeedInfo speedInfo) {
		this(worldModel, timer, config, speedInfo, null);
	}

	public GoalGenerator(IAMWorldModel worldModel, ISimulationTimer timer,
			Config config, ISpeedInfo speedInfo, EntityID myID) {

		this.myID = myID;
		this.config = config;
		this.speedInfo = speedInfo;
		this.timer = timer;
		this.worldModel = worldModel;
		this.freeRoutingCostFunction = new PassableRoutingCostFunction(1,
				Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, worldModel);
		this.freeRouting = new SimpleDijkstrasRoutingModule(worldModel,
				freeRoutingCostFunction, timer);

		// clearingModule = new FutureClearingRoutingModule(config, timer,
		// worldModel, speedInfo);
		// this.myID = myID;

		// Get all police agents
		policeForces = new ArrayList<StandardEntity>();
		policeForces.addAll(worldModel
				.getEntitiesOfType(StandardEntityURN.POLICE_FORCE));

		// Get other agents
		fireBrigadeAgents = new ArrayList<StandardEntity>();
		fireBrigadeAgents.addAll(worldModel
				.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE));

		ambulanceAgents = new ArrayList<StandardEntity>();
		ambulanceAgents.addAll(worldModel
				.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM));

		// Civilians
		civilians = new ArrayList<StandardEntity>();
		civilians.addAll(worldModel
				.getEntitiesOfType(StandardEntityURN.CIVILIAN));

		refuges = new ArrayList<StandardEntity>();
		refuges.addAll(worldModel.getEntitiesOfType(StandardEntityURN.REFUGE));

		if (refuges.size() == 0) {
			// Clear from initial police force positions then
			for (StandardEntity standardEntity : policeForces) {
				refuges.add(worldModel.getEntity(((Human) standardEntity)
						.getPosition()));
			}
		}

		Collections.sort(policeForces, EntityIDComparator.DEFAULT_INSTANCE);
		Collections
				.sort(fireBrigadeAgents, EntityIDComparator.DEFAULT_INSTANCE);
		Collections.sort(ambulanceAgents, EntityIDComparator.DEFAULT_INSTANCE);
		Collections.sort(civilians, EntityIDComparator.DEFAULT_INSTANCE);
		Collections.sort(refuges, EntityIDComparator.DEFAULT_INSTANCE);

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

	public GoalContainer generateGoals() {

		// Extract graph
		SimpleGraph graph = freeRouting.getGraph();

		WorldModelConverter converter = freeRouting.getConverter();

		// Run complete Dijkstra
		DijkstrasShortestPath dijkstra = new DijkstrasShortestPath(graph,
				refugeSourceList, refugeCostList);

		/*DijkstrasShortestPath dijkstraFromMe;
		if (myID != null) {
			dijkstraFromMe = new DijkstrasShortestPath(graph, converter
					.getSimpleGraphIDs(myID));
		}*/

		dijkstra.forceFullCompute();

		GoalContainer goals = new GoalContainer();

		List<StandardEntity> agents = new FastList<StandardEntity>();
		agents.addAll(policeForces);
		agents.addAll(fireBrigadeAgents);
		agents.addAll(ambulanceAgents);

		List<StandardEntity> agentsToClear = extractGoals(agents, dijkstra,
				converter);
		for (StandardEntity agent : agentsToClear) {
			if (agent instanceof PoliceForce) {
				goals.addPoliceForce((PoliceForce) agent);
			} else if (agent instanceof AmbulanceTeam) {
				goals.addAmbulanceTeam((AmbulanceTeam) agent);
			} else {
				goals.addFireBrigade((FireBrigade) agent);
			}
			// goals.addAgent((Human) agent);
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
			if (!(human.isPositionDefined() && (!human.isHPDefined() || human
					.getHP() > 0))) {
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
			Collections.sort(civilians, EntityIDComparator.DEFAULT_INSTANCE);
		}
	}

	@Override
	public void entityRemoved(WorldModel<? extends StandardEntity> model,
			StandardEntity e) {
		// Shouldn't happen
	}

}
