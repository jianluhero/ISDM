/**
 * 
 */
package iamrescue.agent.search;

import iamrescue.belief.IAMWorldModel;
import iamrescue.execution.IExecutionService;
import iamrescue.execution.command.IPath;
import iamrescue.execution.command.MoveCommand;
import iamrescue.routing.IRoutingModule;
import iamrescue.routing.Path;
import iamrescue.util.PositionXY;
import iamrescue.util.comparators.EntityIDComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javolution.util.FastList;

import org.apache.commons.collections15.SetUtils;
import org.apache.log4j.Logger;

import rescuecore2.config.Config;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Simon
 * Coordinated search behaviour
 * 
 * Agents search a subset of locations - defined at the start as a shared randomisation of the map 
 * 
 * id dictates which section of the list to favour
 * 
 */
public class RandomisedClosestSearchBehaviour implements ISearchBehaviour {

	private EntityID myself;
	private IAMWorldModel worldModel;
	private IExecutionService executionService;
	private IRoutingModule routingModule;
	private boolean blockades;
	private List<EntityID> preference;
	private long randomSeed = 47298123;

	private static final Logger LOGGER = Logger
			.getLogger(RandomisedClosestSearchBehaviour.class);

	private static final String BLOCKADE_KEY = "collapse.create-road-blockages";

	public RandomisedClosestSearchBehaviour(IAMWorldModel worldModel, EntityID myself,
			IExecutionService executionService, IRoutingModule routingModule,
			Config config) {
		this.blockades = true;// config.getBooleanValue(BLOCKADE_KEY, true);
		this.worldModel = worldModel;
		this.myself = myself;
		this.executionService = executionService;
		this.routingModule = routingModule;
		
		//set start and end Ids for this agent to prefer to search
		//team size is used to make the set
		Collection<StandardEntity> preferences = worldModel.getEntitiesOfType(StandardEntityURN.BUILDING);
		ArrayList<StandardEntity> prefs = new ArrayList<StandardEntity>();
		for(Iterator<StandardEntity> it = preferences.iterator();it.hasNext();){
			StandardEntity b = it.next();
			prefs.add(b);
		}
		
		Collections.shuffle(prefs, new Random(randomSeed ));
		Collection<StandardEntity> agents = worldModel.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM,StandardEntityURN.POLICE_FORCE);
		//assumption - worldmodel returns agents in the same order? need to ask
		int pos=0;
		int position=0;
		for(Iterator<StandardEntity> it = agents.iterator();it.hasNext();){
			StandardEntity agent = it.next();
			if(agent.getID().getValue()==myself.getValue()){
				position = pos;
			}
			pos++;
		}
		int clustersize = prefs.size()/agents.size();
		makePreferenceList(position,clustersize,prefs);
	}
	
	/*
	 * keeps list in prerefnces for my section
	 */
	private void makePreferenceList(int position, int clustersize, ArrayList<StandardEntity> preferences) {
		preference = new ArrayList<EntityID>();
		int start = clustersize*position;
		int it = start;
		//System.out.println("Agent: " + myself.getValue());
		while(it<start+clustersize){
			preference.add(((StandardEntity) preferences.get(it)).getID());
			it++;
			//System.out.println(preferences.get(it).getID().getValue());
		}
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
		
		//create merged list of safeUnsearched and unknown with start of unknown at divider
		ArrayList<EntityID> buildings = new ArrayList<EntityID>();
		buildings.addAll(worldModel.getSafeUnsearchedBuildings());
		int divider = buildings.size();
		buildings.addAll(worldModel.getUnknownBuildings());
		
		//restrict to my search preferences
		ArrayList<EntityID> preferenceList = modulatedList(buildings);
		
		
		/*System.out.println("Agent"+myself.getValue());
		for(int i=0;i<preference.size();i++){
			System.out.println(preference.get(i).getValue());
		}
		System.out.println("pref list");
		for(int i=0;i<preferenceList.size();i++){
			System.out.println(preferenceList.get(i).getValue());
		}*/

		IPath searchPath = visitClosestBuilding(preferenceList,divider);

		if (!searchPath.isValid()) {
			if (blockades) {					
				searchPath = goToClosestUnseenRoad();
				if (!searchPath.isValid()) {
					// System.out.println("no close road");
					searchPath = goToRandomLocation();
				}
			} else {
				searchPath = goToRandomLocation();
			}
		}

		MoveCommand move = new MoveCommand(searchPath);
		if (searchPath.getLocations().size() > 0) {
			executionService.execute(move);
		}
	}
	
	/*
	 * keeps only buildings in my preference set (or if empty original list) 
	 */
	private ArrayList<EntityID> modulatedList(ArrayList<EntityID> buildings) {
		ArrayList<EntityID> copy = (ArrayList<EntityID>) buildings.clone();
		ArrayList<EntityID> ret = new ArrayList<EntityID>();
		for(int i=0;i<buildings.size();i++){
			if(preference.contains(buildings.get(i))){
				ret.add(buildings.get(i));
			}
		}
		if(buildings.size()>0){
			//System.out.println("should be ok");
			return ret;
		} else {
			return copy;
		}
	}

	private IPath visitClosestBuilding(ArrayList<EntityID> buildings, int index) {
		if (buildings.size() > 0) {
			IPath path = routingModule.findShortestPath(myself, buildings);
			if (path.isValid()) {
				List<EntityID> locations = path.getLocations();
				EntityID destination = locations.get(locations.size()-1);
				int pos = buildings.indexOf(destination);
				if(pos<index){
					//safeUnsearched
					return path;
				} else {
					//unKnown
					List<PositionXY> positions = path.getXYPath();
					locations = locations.subList(0, locations.size() - 1);
					positions = positions.subList(0, positions.size() - 1);
					return new Path(locations, positions);
				}
			} else {
				return Path.INVALID_PATH;
			}
		}else{
			return Path.INVALID_PATH;
		}
		
	}

	/**
	 * @return
	 */
	private IPath goToRandomLocation() {
		EntityID id = ((Human) worldModel.getEntity(myself)).getPosition();
		Area area = (Area) worldModel.getEntity(id);
		List<EntityID> neighbours = area.getNeighbours();
		IPath path = routingModule.findShortestPath(myself, neighbours);
		if (!path.isValid()) {
			List<EntityID> locations = new FastList<EntityID>();
			locations.add(id);
			List<PositionXY> positions = new FastList<PositionXY>();
			positions.add(new PositionXY(((Human) worldModel.getEntity(myself))
					.getLocation(worldModel)));
			positions.add(positions.get(0));
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Staying on " + area + " using path: "
						+ path.toString());
			}
			return new Path(locations, positions);
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Going from " + area + " using path: "
						+ path.toString());
			}
			return path;
		}
	}

	/**
	 * @return
	 */
	private IPath goToClosestUnseenRoad() {
		Collection<StandardEntity> roads = worldModel
				.getEntitiesOfType(StandardEntityURN.ROAD);
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

			// System.out.println("GOing to unknown road: " +
			// worldModel.getEntity(path.getLocations().get(path.getLocations().size()-1)).getFullDescription());
			return path;
		} else {
			return Path.INVALID_PATH;
		}
	}

	/**
	 * @return
	 */
	private IPath goToClosestUnseenBuilding() {
		Collection<EntityID> unsearched = worldModel.getUnknownBuildings();
		if (unsearched.size() > 0) {
			IPath path = routingModule.findShortestPath(myself, unsearched);
			if (path.isValid()) {
				List<EntityID> locations = path.getLocations();
				List<PositionXY> positions = path.getXYPath();
				locations = locations.subList(0, locations.size() - 1);
				positions = positions.subList(0, positions.size() - 1);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Going outside unsafe building "
							+ worldModel.getEntity(
									path.getLocations().get(
											path.getLocations().size() - 1))
									.getFullDescription() + " from "
							+ worldModel.getEntity(myself).getFullDescription()
							+ " using path: " + path.toString());
				}
				// System.out.println("GOing outside unsafe bulding: " +
				// path.getLocations().get(path.getLocations().size()-1));
				return new Path(locations, positions);
			}
		}
		return Path.INVALID_PATH;
	}

	/**
	 * @return
	 */
	private IPath goInsideClosestUnsearchedBuilding() {
		Collection<EntityID> unsearched = worldModel
				.getSafeUnsearchedBuildings();
		if (unsearched.size() > 0) {
			IPath path = routingModule.findShortestPath(myself, unsearched);
			if (LOGGER.isDebugEnabled() && path.isValid()) {
				LOGGER.debug("Going inside safe building "
						+ worldModel.getEntity(
								path.getLocations().get(
										path.getLocations().size() - 1))
								.getFullDescription() + " from "
						+ worldModel.getEntity(myself).getFullDescription()
						+ " using path: " + path.toString());
			}
			// System.out.println("GOing inside a safe bulding: " +
			// worldModel.getEntity(path.getLocations().get(path.getLocations().size()-1)).getFullDescription());
			return path;
		} else {
			return Path.INVALID_PATH;
		}
	}

}
