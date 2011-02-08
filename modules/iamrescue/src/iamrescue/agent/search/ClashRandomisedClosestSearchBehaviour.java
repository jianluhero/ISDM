/**
 * 
 */
package iamrescue.agent.search;

import iamrescue.agent.AbstractIAMAgent;
import iamrescue.belief.IAMWorldModel;
import iamrescue.communication.messages.AgentStuckMessage;
import iamrescue.communication.messages.MessagePriority;
import iamrescue.communication.scenario.IAMCommunicationModule;
import iamrescue.execution.IExecutionService;
import iamrescue.execution.command.IPath;
import iamrescue.execution.command.MoveCommand;
import iamrescue.execution.command.RandomMoveCommand;
import iamrescue.routing.IRoutingModule;
import iamrescue.routing.Path;
import iamrescue.util.PositionXY;
import iamrescue.util.comparators.RoadComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javolution.util.FastList;
import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.config.Config;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Simon Coordinated search behaviour
 * 
 *         Agents search a subset of locations - defined at the start as a
 *         shared randomisation of the map
 * 
 *         id dictates which section of the list to favour
 * 
 *         they only do this when they detect another agent - more commenly they
 *         search closest
 * 
 */
public class ClashRandomisedClosestSearchBehaviour implements ISearchBehaviour {

	private EntityID myself;
	private IAMWorldModel worldModel;
	private IExecutionService executionService;
	private IRoutingModule routingModule;
	private boolean blockades;
	private ArrayList<StandardEntity> roads;
	private final double MAX_PROPORTION_ROADS = 0.2;
	private final int MAX_ROADS_TO_CONSIDER = 20;
	private IAMCommunicationModule commModule;
	private boolean stillStuck = false;

	private static final Logger LOGGER = Logger
			.getLogger(ClashRandomisedClosestSearchBehaviour.class);

	private static final String BLOCKADE_KEY = "collapse.create-road-blockages";
	
	private static final int DISTANCE = 50000;

	public ClashRandomisedClosestSearchBehaviour(IAMWorldModel worldModel,
			EntityID myself, IExecutionService executionService,
			IRoutingModule routingModule, Config config,
			IAMCommunicationModule commModule) {
		this.blockades = true;// config.getBooleanValue(BLOCKADE_KEY, true);
		this.worldModel = worldModel;
		this.myself = myself;
		this.executionService = executionService;
		this.routingModule = routingModule;
		this.commModule = commModule;
		roads = new ArrayList<StandardEntity>(worldModel
				.getEntitiesOfType(StandardEntityURN.ROAD));

	}

	@Override
	public void doDefaultSearch() {

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Starting search.");

				Collection<EntityID> unknown = worldModel.getUnknownBuildings();
				Collection<EntityID> safeUnsearched = worldModel
						.getSafeUnsearchedBuildings();

				LOGGER.trace("Unknown: " + unknown.toString());
				LOGGER.trace("Safe Unsearched: " + safeUnsearched.toString());
			}

			// restrict to my search preferences
			Collection<StandardEntity> range = worldModel.getObjectsInRange(myself, DISTANCE);
			IPath searchPath;
			if (shareLocation()) {
				// ArrayList<EntityID> preferenceList =
				// modulatedList(buildings);
				ArrayList<EntityID> buildings = new ArrayList<EntityID>();
				buildings.addAll(filterByDistance(range,worldModel.getSafeHigh()));
				int divider = buildings.size();
				buildings.addAll(filterByDistance(range,worldModel.getUnknownHigh()));
				if (buildings.size() == 0) {
					buildings.addAll(filterByDistance(range,worldModel
							.getModulatedSafeUnsearchedBuildings()));
					divider = buildings.size();
					buildings.addAll(filterByDistance(range,worldModel.getModulatedUnknownBuildings()));
					if (buildings.size() == 0) {
						buildings.addAll(filterByDistance(range,worldModel
								.getSafeUnsearchedBuildings()));
						divider = buildings.size();
						buildings.addAll(filterByDistance(range,worldModel.getUnknownBuildings()));
						searchPath = visitClosestBuilding(buildings, divider);
					} else {
						searchPath = visitClosestBuilding(buildings, divider);
						if (!searchPath.isValid()) {
							buildings.addAll(filterByDistance(range,worldModel
									.getSafeUnsearchedBuildings()));
							divider = buildings.size();
							buildings.addAll(filterByDistance(range,worldModel.getUnknownBuildings()));
							searchPath = visitClosestBuilding(buildings,divider);
						}
					}
				} else {
					searchPath = visitClosestBuilding(buildings, divider);
					if (!searchPath.isValid()) {
						buildings.addAll(filterByDistance(range,worldModel
								.getModulatedSafeUnsearchedBuildings()));
						divider = buildings.size();
						buildings.addAll(filterByDistance(range,worldModel
								.getModulatedUnknownBuildings()));
						if (buildings.size() == 0) {
							buildings.addAll(filterByDistance(range,worldModel
									.getSafeUnsearchedBuildings()));
							divider = buildings.size();
							buildings.addAll(filterByDistance(range,worldModel.getUnknownBuildings()));
							searchPath = visitClosestBuilding(buildings,
									divider);
						} else {
							searchPath = visitClosestBuilding(buildings,
									divider);
							if (!searchPath.isValid()) {
								buildings.addAll(filterByDistance(range,worldModel
										.getSafeUnsearchedBuildings()));
								divider = buildings.size();
								buildings.addAll(filterByDistance(range,worldModel
										.getUnknownBuildings()));
								searchPath = visitClosestBuilding(buildings,
										divider);
							}
						}
					}
				}
				
				if(buildings.size()==0){
					buildings.addAll(worldModel.getSafeHigh());
					divider = buildings.size();
					buildings.addAll(worldModel.getUnknownHigh());
					if (buildings.size() == 0) {
						buildings.addAll(worldModel
								.getModulatedSafeUnsearchedBuildings());
						divider = buildings.size();
						buildings.addAll(worldModel.getModulatedUnknownBuildings());
						if (buildings.size() == 0) {
							buildings.addAll(worldModel
									.getSafeUnsearchedBuildings());
							divider = buildings.size();
							buildings.addAll(worldModel.getUnknownBuildings());
							searchPath = visitClosestBuilding(buildings, divider);
						} else {
							searchPath = visitClosestBuilding(buildings, divider);
							if (!searchPath.isValid()) {
								buildings.addAll(worldModel
										.getSafeUnsearchedBuildings());
								divider = buildings.size();
								buildings.addAll(worldModel.getUnknownBuildings());
								searchPath = visitClosestBuilding(buildings,divider);
							}
						}
					} else {
						searchPath = visitClosestBuilding(buildings, divider);
						if (!searchPath.isValid()) {
							buildings.addAll(worldModel
									.getModulatedSafeUnsearchedBuildings());
							divider = buildings.size();
							buildings.addAll(worldModel
									.getModulatedUnknownBuildings());
							if (buildings.size() == 0) {
								buildings.addAll(worldModel
										.getSafeUnsearchedBuildings());
								divider = buildings.size();
								buildings.addAll(worldModel.getUnknownBuildings());
								searchPath = visitClosestBuilding(buildings,
										divider);
							} else {
								searchPath = visitClosestBuilding(buildings,
										divider);
								if (!searchPath.isValid()) {
									buildings.addAll(worldModel
											.getSafeUnsearchedBuildings());
									divider = buildings.size();
									buildings.addAll(worldModel
											.getUnknownBuildings());
									searchPath = visitClosestBuilding(buildings,
											divider);
								}
							}
						}
					}
				}
			} else {
				ArrayList<EntityID> buildings = new ArrayList<EntityID>();
				int divider = 0;
				buildings.addAll(filterByDistance(range,worldModel.getSafeHigh()));
				divider = buildings.size();
				buildings.addAll(filterByDistance(range,worldModel.getUnknownHigh()));
				if (buildings.size() == 0) {
					buildings.addAll(filterByDistance(range,worldModel.getSafeUnsearchedBuildings()));
					divider = buildings.size();
					buildings.addAll(filterByDistance(range,worldModel.getUnknownBuildings()));
					searchPath = visitClosestBuilding(buildings, divider);
				} else {
					searchPath = visitClosestBuilding(buildings, divider);
					if (!searchPath.isValid()) {
						buildings.addAll(filterByDistance(range,worldModel
								.getSafeUnsearchedBuildings()));
						divider = buildings.size();
						buildings.addAll(filterByDistance(range,worldModel.getUnknownBuildings()));
						searchPath = visitClosestBuilding(buildings, divider);
					}
				}
				
				if(buildings.size()==0){
					divider = 0;
					buildings.addAll(worldModel.getSafeHigh());
					divider = buildings.size();
					buildings.addAll(worldModel.getUnknownHigh());
					if (buildings.size() == 0) {
						buildings.addAll(worldModel.getSafeUnsearchedBuildings());
						divider = buildings.size();
						buildings.addAll(worldModel.getUnknownBuildings());
						searchPath = visitClosestBuilding(buildings, divider);
					} else {
						searchPath = visitClosestBuilding(buildings, divider);
						if (!searchPath.isValid()) {
							buildings.addAll(worldModel
									.getSafeUnsearchedBuildings());
							divider = buildings.size();
							buildings.addAll(worldModel.getUnknownBuildings());
							searchPath = visitClosestBuilding(buildings, divider);
						}
					}
				}

			}

			if (!searchPath.isValid()) {
				if (blockades) {
					searchPath = goToClosestUnseenRoad(range);
					if (!searchPath.isValid()) {

						searchPath = visitOldestRoad(roads,range);
					}
				} else {
					searchPath = visitOldestRoad(roads,range);
				}
			}

			if (searchPath.isValid()) {
				MoveCommand move = new MoveCommand(searchPath);
				if (searchPath.getLocations().size() > 0) {
					AbstractIAMAgent.stopIfInterrupted();
					executionService.execute(move);
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Search path: " + searchPath);
					}
				}
				stillStuck = false;
			} else {
				RandomMoveCommand random = new RandomMoveCommand();
				AbstractIAMAgent.stopIfInterrupted();
				executionService.execute(random);
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("Could not route anywhere. "
							+ "Stepping to random neighbour. "
							+ "I am probably stuck.");
				}
				if (!stillStuck) {
					AgentStuckMessage help = new AgentStuckMessage(myself,
							((Human) worldModel.getEntity(myself))
									.getPosition());
					help.setPriority(MessagePriority.CRITICAL);
					// I'll repeat this anyway while I am stuck
					help.setTTL(10);
					commModule.enqueueRadioMessageToOwnTeam(help);
					stillStuck = true;
				}
			}
	}
	
	public Collection<EntityID> filterByDistance(Collection<StandardEntity> withinRange, Collection<EntityID> interest){
		// withinRange - objects in range
		// interest - set to filter
		
		FastSet<EntityID> filtered = new FastSet<EntityID>();
		for(Iterator<StandardEntity> it = withinRange.iterator(); it.hasNext();){
			StandardEntity e = (StandardEntity) it.next();
			if(interest.contains(e.getID())){
				filtered.add(e.getID());
			}
		}
		
		return filtered;
	}
	
	public ArrayList<StandardEntity> filterByDistance(Collection<StandardEntity> withinRange, Collection<StandardEntity> interest){
		// withinRange - objects in range
		// interest - set to filter
		
		ArrayList<StandardEntity> filtered = new ArrayList<StandardEntity>();
		for(Iterator<StandardEntity> it = withinRange.iterator(); it.hasNext();){
			StandardEntity e = (StandardEntity) it.next();
			if(interest.contains(e)){
				filtered.add(e);
			}
		}
		
		return filtered;
	}

	/*
	 * decides if an agent is sharing a location with another
	 */
	private boolean shareLocation() {
		Collection<StandardEntity> agents = worldModel.getEntitiesOfType(
				StandardEntityURN.AMBULANCE_TEAM,
				StandardEntityURN.POLICE_FORCE);
		int loc = ((Human) worldModel.getEntity(myself)).getPosition()
				.getValue();
		for (Iterator<StandardEntity> it = agents.iterator(); it.hasNext();) {
			StandardEntity agent = it.next();
			if (((Human) agent).isPositionDefined()
					&& agent.getID().getValue() != myself.getValue()) {
				if (((Human) agent).getPosition().getValue() == loc) {
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * returns the load visited the longest time ago
	 */
	private IPath visitOldestRoad(ArrayList<StandardEntity> roads, Collection<StandardEntity> range) {
		ArrayList<StandardEntity> filtered = filterByDistance(range,roads);
		
		if(filtered.size()>0){
			Collections.sort(filtered, new RoadComparator(worldModel));
			IPath path = Path.INVALID_PATH;
			int endIndex = (int) (MAX_PROPORTION_ROADS * filtered.size());
			if (endIndex == filtered.size()) {
				endIndex = filtered.size() - 1;
			} else if (endIndex == 0) {
				endIndex = 1;
			}

			List<EntityID> targets = new ArrayList<EntityID>(endIndex);

			for (int i = 0; i <= endIndex; i++) {
				targets.add(filtered.get(i).getID());
			}

			path = routingModule.findShortestPath(myself, targets);

			return path;
		} else {
			Collections.sort(roads, new RoadComparator(worldModel));
			IPath path = Path.INVALID_PATH;
			int endIndex = (int) (MAX_PROPORTION_ROADS * roads.size());
			if (endIndex == roads.size()) {
				endIndex = roads.size() - 1;
			} else if (endIndex == 0) {
				endIndex = 1;
			}

			List<EntityID> targets = new ArrayList<EntityID>(endIndex);

			for (int i = 0; i <= endIndex; i++) {
				targets.add(roads.get(i).getID());
			}

			path = routingModule.findShortestPath(myself, targets);

			return path;
		}
		
	}

	private IPath visitClosestBuilding(ArrayList<EntityID> buildings, int index) {
		if (buildings.size() > 0) {
			IPath path = routingModule.findShortestPath(myself, buildings);
			if (path.isValid()) {
				List<EntityID> locations = path.getLocations();
				EntityID destination = locations.get(locations.size() - 1);
				int pos = buildings.indexOf(destination);
				if (pos < index) {
					// safeUnsearched
					return path;
				} else {
					// unKnown
					List<PositionXY> positions = path.getXYPath();
					locations = locations.subList(0, locations.size() - 1);
					positions = positions.subList(0, positions.size() - 1);
					return new Path(locations, positions);
				}
			} else {
				return Path.INVALID_PATH;
			}
		} else {
			return Path.INVALID_PATH;
		}

	}

	/**
	 * @return
	 */
	private IPath goToClosestUnseenRoad(Collection<StandardEntity> range) {
		
		Collection<StandardEntity> roads = worldModel.getEntitiesOfType(StandardEntityURN.ROAD);
		
		ArrayList<StandardEntity> filtered = filterByDistance(range,roads);
		
		if(filtered.size()==0){
			Collection<EntityID> unseenRoads = new FastList<EntityID>();

			for (StandardEntity road : roads) {
				Road r = (Road) road;
				if (!r.isBlockadesDefined()) {
					unseenRoads.add(r.getID());
				}
			}
			if (unseenRoads.size() > 0) {
				IPath path = routingModule.findShortestPath(myself, unseenRoads);
				if (LOGGER.isDebugEnabled() && path.isValid()) {
					LOGGER.debug("Going to unknown road "
						+ worldModel.getEntity(
								path.getLocations().get(
										path.getLocations().size() - 1))
								.getFullDescription() + " from "
						+ worldModel.getEntity(myself).getFullDescription()
						+ " on "
						+ worldModel.getEntity(myself).getFullDescription()
						+ " using path: " + path.toString());
				}
				return path;
			} else {
				return Path.INVALID_PATH;
			}
		} else {
			Collection<EntityID> unseenRoads = new FastList<EntityID>();

			for (StandardEntity road : filtered) {
				Road r = (Road) road;
				if (!r.isBlockadesDefined()) {
					unseenRoads.add(r.getID());
				}
			}
			if (unseenRoads.size() > 0) {
				IPath path = routingModule.findShortestPath(myself, unseenRoads);
				if (LOGGER.isDebugEnabled() && path.isValid()) {
					LOGGER.debug("Going to unknown road "
						+ worldModel.getEntity(
								path.getLocations().get(
										path.getLocations().size() - 1))
								.getFullDescription() + " from "
						+ worldModel.getEntity(myself).getFullDescription()
						+ " on "
						+ worldModel.getEntity(myself).getFullDescription()
						+ " using path: " + path.toString());
				}
				return path;
			} else {
				return Path.INVALID_PATH;
			}
		}
	}

}
