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

import java.util.Collection;
import java.util.List;

import javolution.util.FastList;

import org.apache.log4j.Logger;

import rescuecore2.config.Config;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class SimpleSearchBehaviour implements ISearchBehaviour {

	private EntityID myself;
	private IAMWorldModel worldModel;
	private IExecutionService executionService;
	private IRoutingModule routingModule;
	private boolean blockades;

	private static final Logger LOGGER = Logger
			.getLogger(SimpleSearchBehaviour.class);

	private static final String BLOCKADE_KEY = "collapse.create-road-blockages";

	public SimpleSearchBehaviour(IAMWorldModel worldModel, EntityID myself,
			IExecutionService executionService, IRoutingModule routingModule,
			Config config) {
		this.blockades = true;// config.getBooleanValue(BLOCKADE_KEY, true);
		this.worldModel = worldModel;
		this.myself = myself;
		this.executionService = executionService;
		this.routingModule = routingModule;
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

		IPath searchPath = goInsideClosestUnsearchedBuilding();

		if (!searchPath.isValid()) {
			// System.out.println("no building to go inside");
			searchPath = goToClosestUnseenBuilding();
			if (!searchPath.isValid()) {
				// System.out.println("no building to go to");
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
		}
		// System.out.println("going to" + searchPath.getDestination());
		// if(searchPath.isValid()){
		// System.out.println("valid path");
		// }

		MoveCommand move = new MoveCommand(searchPath);
		if (searchPath.getLocations().size() > 0) {
			executionService.execute(move);
		}
		// System.out.println(searchPath.toString());
		// }else{
		// System.out.println("null path in search behaviour");
		// }
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
