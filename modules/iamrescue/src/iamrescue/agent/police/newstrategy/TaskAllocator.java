package iamrescue.agent.police.newstrategy;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.execution.command.IPath;
import iamrescue.routing.WorldModelConverter;
import iamrescue.routing.dijkstra.SimpleGraph;
import iamrescue.routing.util.ISpeedInfo;
import iamrescue.util.PositionXY;
import iamrescue.util.comparators.EntityIDComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javolution.util.FastList;
import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public class TaskAllocator {

	private static final int RANDOM_ADDITIONAL_DISTANCE = 60000;
	private static final double CURRENT_TARGET_PREFERENCE = 0.8;
	
	private static final double UNBURIED_BUILDING_CIVILIAN_PENALTY = 0;
	private static final double BURIED_BUILDING_CIVILIAN_PENALTY = 60000;
	private static final double UNKNOWN_CIVILIAN_PENALTY = 120000;
	private static final double UNBURIED_ROAD_CIVILIAN_PENALTY = Double.POSITIVE_INFINITY;
	
	private IAMWorldModel worldModel;
	private List<StandardEntity> policeForces;
	private List<StandardEntity> refuges;
	private FutureClearingRoutingModule clearingModule;
	private GoalGenerator goalGenerator;
	private EntityID lastClosest = null;
	private EntityID myID;
	private GoalContainer lastGeneratedGoals;

	// FutureClearingRoutingModule(config,
	// timer, worldModel,
	// myID);

	public TaskAllocator(IAMWorldModel worldModel, ISimulationTimer timer,
			Config config, ISpeedInfo speedInfo, EntityID myID) {

		this.myID = myID;
		this.worldModel = worldModel;

		clearingModule = new FutureClearingRoutingModule(config, timer,
				worldModel, speedInfo);

		refuges = new ArrayList<StandardEntity>();
		refuges.addAll(worldModel.getEntitiesOfType(StandardEntityURN.REFUGE));

		// Sort all
		Collections.sort(refuges, EntityIDComparator.DEFAULT_INSTANCE);

		this.goalGenerator = new GoalGenerator(worldModel, timer, config,
				speedInfo);
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

	public StandardEntity computeSimpleAssignment(PoliceForce police) {
		return null;
	}

	public GoalContainer getLastGeneratedGoals() {
		return lastGeneratedGoals;
	}
	
	public StandardEntity computeClosestGoal(PoliceForce police) {
		// Generate goals
		GoalContainer generatedGoals = goalGenerator.generateGoals();
		lastGeneratedGoals =generatedGoals;

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
				Pair<Integer, Integer> location = se.getLocation(worldModel);
				if (location != null) {
					double distance = myPosition.distanceTo(new PositionXY(
							location));

					if (RANDOM_ADDITIONAL_DISTANCE > 0 && lastClosest != null
							&& !lastClosest.equals(se.getID())
							&& !se.getID().equals(myID)) {
						distance += (int) (Math.random() * RANDOM_ADDITIONAL_DISTANCE);
					} else {
						if (lastClosest != null
								&& lastClosest.equals(se.getID())) {
							distance *= CURRENT_TARGET_PREFERENCE;
						}
					}
					
					if (se instanceof Civilian) {
						Civilian civ = (Civilian)se;
						distance += getCivilianPenalty(civ);
					}

					if (distance < bestDistance) {
						bestDistance = distance;
						best = se;
					}
				}
			}
			if (best != null) {
				lastClosest = best.getID();
				return best;
			}
		}
		return null;
	}

	private double getCivilianPenalty(Civilian civ) {
		double penalty;
		
		// Prefer certain civilians
		if (civ.isBuriednessDefined()) {
			// First check unburied civilians
			if (civ.getBuriedness() == 0) {
				// Add preference
				// Check if position is defined
				if (civ.isPositionDefined()) {
					EntityID position = civ.getPosition();
					StandardEntity positionEntity = worldModel.getEntity(position);
					if (positionEntity instanceof Building) {
						penalty = UNBURIED_BUILDING_CIVILIAN_PENALTY;
					} else {
						penalty = UNBURIED_ROAD_CIVILIAN_PENALTY;
					}
				} else {
					// Don't know position
					penalty = UNKNOWN_CIVILIAN_PENALTY;
				}
			} else {
				// Buried civilian
				penalty= BURIED_BUILDING_CIVILIAN_PENALTY;
			}
		} else {
			// Still check if on road
			if (civ.isPositionDefined()) {
				EntityID position = civ.getPosition();
				StandardEntity positionEntity = worldModel.getEntity(position);
				if (positionEntity instanceof Building) {
					penalty = BURIED_BUILDING_CIVILIAN_PENALTY;
				} else {
					penalty= UNBURIED_ROAD_CIVILIAN_PENALTY;
				}
			} else {
				// Don't know position
				penalty= UNKNOWN_CIVILIAN_PENALTY;
			}
		}
		return penalty;
	}

	/**
	 * 
	 */

	/**
	 * @return the clearingModule
	 */
	public FutureClearingRoutingModule getClearingModule() {
		return clearingModule;
	}

}
