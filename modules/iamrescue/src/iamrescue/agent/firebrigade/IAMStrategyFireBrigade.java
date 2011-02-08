package iamrescue.agent.firebrigade;

import iamrescue.agent.AbstractIAMAgent;
import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.provenance.IProvenanceInformation;
import iamrescue.belief.provenance.ProvenanceLogEntry;
import iamrescue.execution.command.ExtinguishCommand;
import iamrescue.execution.command.IPath;
import iamrescue.execution.command.MoveCommand;
import iamrescue.execution.command.RestCommand;
import iamrescue.routing.Path;
import iamrescue.routing.queries.IRoutingLocation;
import iamrescue.routing.queries.RoutingLocation;
import iamrescue.routing.queries.RoutingQuery;
import iamrescue.routing.util.ISpeedInfo;
import iamrescue.util.PositionXY;
import iamrescue.util.comparators.EntityIDComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

// This is IAMFireBrigadeTest

public class IAMStrategyFireBrigade extends AbstractIAMAgent<FireBrigade> {

	private final double STICK_EXTINGUISH_MULTIPLIER = 2;
	
	// private FirePredictor firePredictor = null;
	private FastFirePredictor firePredictor = null;

	// set this to 1 to switch off sticking behaviour. Prevents thrashing
	private final int STICK_TO_CLOSE_FIRES_TIME = 2000;
	private int lastSiteChangeTime = Integer.MIN_VALUE;
	// private int STICK_TO_DISTANCE = MAX_DISTANCE;
	private final int TIMESTEPS_INTO_THE_FUTURE = 5;// try increase it
	private int MAX_WATER = 15000; // checked
	private int MAX_DISTANCE = 60000; // checked
	private int ALMOST_MAX_DISTANCE = (int) (0.7 * MAX_DISTANCE);
	private int viewDistance = 30000; // from
	// iamrescue/belief/BuildingSearchUtility.java

	private static final Logger LOGGER = Logger
			.getLogger(IAMStrategyFireBrigade.class);

	private static final int ALWAYS_VIEW_TIME_THRESHOLD = 2;
	private HeatTransferGraph heatTransferGraph;
	private ISpeedInfo speed;

	private final boolean DEBUG_FIRE = false;

	// private static ArrayList<IAMStrategyFireBrigade> agents = new
	// ArrayList<IAMStrategyFireBrigade>();

	private ArrayList<FireBrigade> fireBrigades;

	private int timestepsSinceCurrentTargetObserved = 0;

	private FireStrategy strategy = null;
	// private ArrayList<ArrayList<Building>> fireSite = null;
	// private ArrayList<ArrayList<Building>> centreOfFireSite = new
	// ArrayList<ArrayList<Building>>();

	private final int TimeAtTargetPerStep = 2;
	private FireStrategyState strategyState = new FireStrategyState(
			TimeAtTargetPerStep);

	private Building lastTargetAllocated;

	private VisibilityMap visibilityMap;

	@Override
	protected void postConnect() {
		super.postConnect();

		this.MAX_WATER = config.getIntValue("fire.tank.maximum");
		this.MAX_DISTANCE = config.getIntValue("fire.extinguish.max-distance");

		this.viewDistance = config.getIntValue("perception.los.max-distance",
				30000);

		// pathPlanner = this.getRoutingModule();

		IAMWorldModel world = getWorldModel();

		// firePredictor = new FirePredictor(world);
		firePredictor = new FastFirePredictor(this.getTimer(), world, me()
				.getID());

		heatTransferGraph = firePredictor.getHeatTransferGraph();

		// initialise fireBrigades
		fireBrigades = new ArrayList<FireBrigade>();

		// we get all the firebrigades IDs
		Collection<StandardEntity> allFireBrigades = world
				.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE);
		for (StandardEntity standardEntity : allFireBrigades)
			fireBrigades.add((FireBrigade) standardEntity);

		Collections.sort(fireBrigades, EntityIDComparator.DEFAULT_INSTANCE);

		// showWorldModelViewer();
		strategy = new FireStrategy(getFireBrigades(), getRoutingModule(), this
				.getID(), this);

		// Collection<StandardEntity> entitiesOfType =
		// getWorldModel().getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE);
		// List<StandardEntity> entities = new
		// ArrayList<StandardEntity>(entitiesOfType);
		// Collections.sort(entities,EntityIDComparator.DEFAULT_INSTANCE);
		// if(entities.get(0).equals(me())) {
		// showFireImportanceModel(firePredictor);
		// }

		// System.gc()

		visibilityMap = new VisibilityMap(world, viewDistance);
	}

	protected List<FireBrigade> getFireBrigades() {
		return fireBrigades;
	}

	@Override
	protected void think(int time, ChangeSet changed) {
	//	firePredictor.updateImportanceModel();
		mainThink(time, changed);

		/*int halfSize = (fireBrigades.size() + 1) / 2;
		int restDivision = getTimer().getTime() % halfSize;

		if (me().getID().equals(fireBrigades.get(restDivision).getID())) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(me().getID().getValue() + "am updating");
			}
			firePredictor.updateImportanceModel();
		} else if (restDivision + halfSize < fireBrigades.size()) {
			if (me().getID().equals(
					fireBrigades.get(restDivision + halfSize).getID())) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(me().getID().getValue() + "am updating");
				}
				firePredictor.updateImportanceModel();
			}
		}*/
	}

	protected void mainThink(int time, ChangeSet changed) {
		stopIfInterrupted();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Start of think at timestep " + time + " for: "
					+ this.toString());
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("CurrentTarget is: "
					+ (currentTarget == null ? "null" : currentTarget.getID()
							.getValue()));
		}

		String location = this.getLocation().getURN();
		if (location.equals(StandardEntityURN.REFUGE.toString())) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("We are at the refuge.");
			}
			if (this.me().getWater() < MAX_WATER) {
				// send rest command, and we will fill with water.
				getExecutionService().execute(new RestCommand());
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Filling with water, ending turn.");
				}
				return;
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Will not get more water.");
			}
		}

		if (this.me().getWater() == 0) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("No water, going to refuge to fill up.");
			}

			Collection<StandardEntity> refuges = getWorldModel()
					.getEntitiesOfType(StandardEntityURN.REFUGE);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Going to closest of refuges: " + refuges);
			}
			IPath path = getRoutingModule().findShortestPath(me(), refuges);

			// if the full path is valid and it's a refuge, then go fully in
			if (path.isValid()) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER
							.debug("Sending full path, since we want to go into the refuge, path is: "
									+ path.toString());
				}
				this.sendMove(path);
				return;
			} else {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Could not travel to refuges.");
				}
				doDefaultSearch();
				return;
			}
		}

		speed = this.getSpeedInfo();

		/*
		 * State machine
		 */
		boolean newTarget = false;
		boolean extinguish = false;
		// check if we are within range

		int distanceToTarget = Integer.MAX_VALUE;
		if (currentTarget != null) {
			distanceToTarget = getWorldModel().getDistance(me().getID(),
					currentTarget.getID());
		}

		switch (strategyState.getAgentState()) {
		case FREE:
		default:
			newTarget = true;
			break;
		case TRAVELLING:
			newTarget = true;
			break;/*
			if (distanceToTarget < MAX_DISTANCE) {
				// System.out.println("We are close enough");
				// if (LOGGER.isDebugEnabled()) {
				// LOGGER.debug("We are close enough");
				extinguish = true;
				break;
			} else {
				if (LOGGER.isDebugEnabled()) {
					LOGGER
							.debug("Target is out of range, moving closer. Distance = "
									+ distanceToTarget + " > " + MAX_DISTANCE);
				}

				if (goToBuilding(currentTarget)) {
					// if path is valid, then command was sent, so return
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Going to building "
								+ currentTarget.getID() + ", and ending turn.");
					}
					return;
				} else {
					if (LOGGER.isDebugEnabled()) {
						LOGGER
								.debug("Unable to go to current Target, getting a new target.");
					}
					newTarget = true;
					break;
				}
			}*/
		case EXTINGUISHING:

			// System.out.println("Calculated distance between us and target: "+distanceToTarget);
			// if (LOGGER.isDebugEnabled()) {
			// LOGGER.debug("Calculated distance between us and target: "+distanceToTarget);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Distance to building is: " + distanceToTarget);
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Viewing distance is: " + viewDistance);
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Extinguish distance is: " + MAX_DISTANCE);
			}

			int lastObserved = getTimeLastObserved(currentTarget);
			// boolean knowStatus = (lastObserved == getTimer().getTime());

			/*
			 * if (knowStatus) { if (LOGGER.isDebugEnabled()) {
			 * LOGGER.debug("Current target has been observed this timestep.");
			 * } timestepsSinceCurrentTargetObserved = 0; } else { if
			 * (LOGGER.isDebugEnabled()) { LOGGER.debug(
			 * "Current target hasn't changed this timestep, incrementing time since changed."
			 * ); } timestepsSinceCurrentTargetObserved++; }
			 */

			// int allowedRange = 0;
			// final int maxTimeStepsWithoutSeeingTarget = 4;
			// final int maxTimeStepsWithoutSeeingTarget = 4;
			// if we haven't seen an update to our target in over 4
			// timesteps...

			// boolean needToSee;
			/*
			 * if (getTimer().getTime() - lastObserved >=
			 * ALWAYS_VIEW_TIME_THRESHOLD) { // building data not changed
			 * recently, so go closer to // update our model if
			 * (LOGGER.isDebugEnabled()) { LOGGER.debug(
			 * "current Target not updated recently, going closer to update model. (ext)"
			 * ); } needToSee = true; } else { // building has been changed
			 * recently, so only make sure we // are in the extinguishing range
			 * if (LOGGER.isDebugEnabled()) { LOGGER
			 * .debug("current Target updated recently, will keep distance. (ext)"
			 * ); } needToSee = false; }
			 */

			if (getTimer().getTime() - lastObserved < ALWAYS_VIEW_TIME_THRESHOLD) {

				boolean noLongerOnFire = (lastObserved == getTimer().getTime())
						&& currentTarget.isFierynessDefined()
						&& (currentTarget.getFieryness() > 3);
				boolean allocationTimeOut = strategyState.addTimeAtTarget() >= strategyState
						.getTimePerTarget();

				// We must be at target to be at this state.
				if (noLongerOnFire || allocationTimeOut) {
					// we've done enough
					strategyState.resetTimePerTarget();
					newTarget = true;
					break;
				} else {
					// still extinguish
					extinguish = true;
					break;
				}
			} else {
				if (LOGGER.isDebugEnabled()) {
					LOGGER
							.debug("Target is out of range, moving closer. (ext)");
				}
				if (goToBuilding(currentTarget, true)) {
					// if path is valid, then command was sent, so return
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Going to building "
								+ currentTarget.getID()
								+ ", and ending turn. (ext)");
					}
					return;
				} else {
					if (LOGGER.isDebugEnabled()) {
						LOGGER
								.debug("Unable to go to current Target, getting a new target. (ext)");
					}
					newTarget = true;
					break;
				}
			}
		}

		if (strategy.needToReassign()) {
			newTarget = true;
		}
		// newTarget = true;

		if (newTarget) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("newTarget");
			}

			boolean goingToTarget = false;
			EntityID lastTarget = null;
			int counter = 0;
			final int MAX_COUNTER = 20;

			// Seb's Comment: I don't know why we need a loop here. I think it
			// will run without -- if safe, we should remove this.
			while (!goingToTarget && counter < MAX_COUNTER) {
				counter++;
				stopIfInterrupted();
				try {

					ArrayList<Building> fringeBuildings = new ArrayList<Building>();
					ArrayList<Building> centreBuildings = new ArrayList<Building>();

					for (FastFireSite fireSite : firePredictor.getFireSites()) {
						stopIfInterrupted();
						fringeBuildings.addAll(fireSite.getFringe());
						centreBuildings.addAll(fireSite.getCenter());
					}

					currentTarget = getTarget(fringeBuildings, centreBuildings);
				} catch (Exception e) {
					LOGGER.error("Exception from strategy: " + e.toString());
					e.printStackTrace();
				}

				if (currentTarget == null) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER
								.debug("strategy gave up a null target, do default search.");
					}
					// getExecutionService().execute(new RestCommand());
					doDefaultSearch();
					strategyState
							.setState(iamrescue.agent.firebrigade.FireStrategyState.AgentState.FREE);
					return;
				} else {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("trying to go to target: "
								+ currentTarget.getID()
								+ " from our location: " + me().getPosition());
					}

					distanceToTarget = getWorldModel().getDistance(
							me().getID(), currentTarget.getID()); // update
					// in case currentTarget changed

					boolean observedRecently = (getTimer().getTime()
							- getTimeLastObserved(currentTarget) < ALWAYS_VIEW_TIME_THRESHOLD);

					if (distanceToTarget < MAX_DISTANCE && observedRecently) {
						extinguish = true;
						goingToTarget = true;
						// don't return, extinguish
					} else {
						if (LOGGER.isDebugEnabled()) {
							LOGGER
									.debug("Thought I was extinguishing, but too far.");
						}
						strategyState
								.setState(iamrescue.agent.firebrigade.FireStrategyState.AgentState.TRAVELLING);
						goingToTarget = goToBuilding(currentTarget,
								!observedRecently);
						if (goingToTarget) {
							return;
						} else {

							// go around and get a new target.
							if (lastTarget == currentTarget.getID()) {
								// we're tried to get this target before, so
								// we've looped around all possibles, so
								// just do default search for now.
								if (LOGGER.isDebugEnabled()) {
									LOGGER
											.debug("strategy gave us no targets we can go to, so do default search.");
								}
								doDefaultSearch();
								strategyState
										.setState(iamrescue.agent.firebrigade.FireStrategyState.AgentState.FREE);
								return;
							} else {
								lastTarget = currentTarget.getID();
							}
						}
					}
				}
			}
			if (counter >= MAX_COUNTER) {
				LOGGER
						.error("Reached maximum number of iterations. Defaulting to search");
				doDefaultSearch();
				strategyState
						.setState(iamrescue.agent.firebrigade.FireStrategyState.AgentState.FREE);
				return;
			}
		}

		if (extinguish) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("extinguish");
			}
			ExtinguishCommand extinguishCommand = new ExtinguishCommand();
			extinguishCommand.setBuildingToExtinguish(currentTarget);
			extinguishCommand.setPercentageOfFullPower(1.0);

			stopIfInterrupted();

			getExecutionService().execute(extinguishCommand);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Extinguishing target " + currentTarget.getID()
						+ ", and ending turn.");
			}
			strategyState
					.setState(iamrescue.agent.firebrigade.FireStrategyState.AgentState.EXTINGUISHING);
			return;
		}

		stopIfInterrupted();

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Ending turn by searching.");
		}
		doDefaultSearch();

		return;
	}

	private int getTimeLastObserved(Building b) {
		IProvenanceInformation provenance = getWorldModel().getProvenance(
				b.getID(), StandardPropertyURN.FIERYNESS);
		if (provenance == null) {
			return -100;
		}

		ProvenanceLogEntry lastDefined = provenance.getLastDefined();

		if (lastDefined == null) {
			return -100;
		}

		return lastDefined.getTimeStep();
	}

	private Building getTarget(ArrayList<Building> fringeBuildings,
			ArrayList<Building> centreBuildings) {

		List<Building> orderedBuildingsIncreasingImportance = new ArrayList<Building>();

		if (lastSiteChangeTime + STICK_TO_CLOSE_FIRES_TIME <= getTimer()
				.getTime()) {
			// Consider all sites
			lastSiteChangeTime = getTimer().getTime();
			orderedBuildingsIncreasingImportance.addAll(firePredictor
					.getOrderOfImportantBuildingsToExtinguish(centreBuildings));

			List<Building> fieryness1buildings = new ArrayList<Building>();
			List<Building> fieryness2buildings = new ArrayList<Building>();

			for (Building building : fringeBuildings) {
				if (building.getFieryness() == 1) {
					fieryness1buildings.add(building);
				} else {
					fieryness2buildings.add(building);
				}
			}

			orderedBuildingsIncreasingImportance
					.addAll(firePredictor
							.getOrderOfImportantBuildingsToExtinguish(fieryness2buildings));

			orderedBuildingsIncreasingImportance
					.addAll(firePredictor
							.getOrderOfImportantBuildingsToExtinguish(fieryness1buildings));
		} else {
			// Consider only local targets
			List<Building> closeFringe1 = new ArrayList<Building>();
			List<Building> closeFringe2 = new ArrayList<Building>();

			List<Building> closeCentre = new ArrayList<Building>();

			List<Building> farFringe1 = new ArrayList<Building>();
			List<Building> farFringe2 = new ArrayList<Building>();

			List<Building> farCentre = new ArrayList<Building>();
			List<Building> orderedFringe = firePredictor
					.getOrderOfImportantBuildingsToExtinguish(fringeBuildings);
			List<Building> orderedCentre = firePredictor
					.getOrderOfImportantBuildingsToExtinguish(centreBuildings);

			PositionXY myPosition = new PositionXY(me().getLocation(
					getWorldModel()));

			for (int i = 0; i < orderedFringe.size(); i++) {
				Building building = orderedFringe.get(i);
				PositionXY buildingLocation = new PositionXY(building
						.getLocation(getWorldModel()));
				if (myPosition.distanceTo(buildingLocation) <= MAX_DISTANCE * STICK_EXTINGUISH_MULTIPLIER) {
					if (building.getFieryness() == 1) {
						closeFringe1.add(building);
					} else {
						closeFringe2.add(building);
					}
				} else {
					if (building.getFieryness() == 1) {
						farFringe1.add(building);
					} else {
						farFringe2.add(building);
					}
				}
			}

			for (int i = 0; i < orderedCentre.size(); i++) {
				Building building = orderedCentre.get(i);
				PositionXY buildingLocation = new PositionXY(building
						.getLocation(getWorldModel()));
				if (myPosition.distanceTo(buildingLocation) <= MAX_DISTANCE * STICK_EXTINGUISH_MULTIPLIER) {
					closeCentre.add(building);
				} else {
					farCentre.add(building);
				}
			}

			orderedBuildingsIncreasingImportance.addAll(farCentre);
			orderedBuildingsIncreasingImportance.addAll(farFringe2);
			orderedBuildingsIncreasingImportance.addAll(farFringe1);
			orderedBuildingsIncreasingImportance.addAll(closeCentre);
			orderedBuildingsIncreasingImportance.addAll(closeFringe2);
			orderedBuildingsIncreasingImportance.addAll(closeFringe1);
		}

		if (orderedBuildingsIncreasingImportance.size() == 0) {
			return null;
		} else {
			int i = orderedBuildingsIncreasingImportance.size() - 1;

			Building b = orderedBuildingsIncreasingImportance.get(i);
			int timeLastObserved = getTimeLastObserved(b);
			boolean goToViewDistance = (getTimer().getTime() - timeLastObserved >= ALWAYS_VIEW_TIME_THRESHOLD);
			IPath path = getExtinguishPath(b, goToViewDistance);
			/*
			 * getReusableRouting().findShortestPath(me(),
			 * orderedBuildings.get(i));
			 */
			while (!path.isValid() && i > 0) {
				i--;
				b = orderedBuildingsIncreasingImportance.get(i);
				timeLastObserved = getTimeLastObserved(b);
				goToViewDistance = (getTimer().getTime() - timeLastObserved >= ALWAYS_VIEW_TIME_THRESHOLD);
				path = getExtinguishPath(b, goToViewDistance);
			}
			if (path.isValid()) {
				return orderedBuildingsIncreasingImportance.get(i);
			} else {
				return null;
			}
		}

	}

	private boolean areWeAtNeighbour(Collection<Building> buildingIDs) {
		for (Building id : buildingIDs) {
			for (EntityID neighbourID : id.getNeighbours()) {
				if (neighbourID.getValue() == me().getPosition().getValue()) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean goToBuilding(Building location) {
		return goToBuilding(location, false);
	}

	private boolean goToBuilding(Building location, boolean alsoNeedToSee) {
		IPath path = getExtinguishPath(location, alsoNeedToSee);
		if (path.isValid() && path.getLocations().size() > 1) {
			sendMove(path);
			return true;
		} else {
			LOGGER.debug("Suggested path: " + path);
			return false;
		}
	}

	private boolean goToBuildingOld(Building locations) {
		// full path into the building
		IPath path = getRoutingModule().findShortestPath(me(), locations);
		// get the shortest possible path that we can still extinguish from
		IPath shortPath = removeLastPathNode(path, Collections
				.singleton(locations));
		if (shortPath.isValid()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Sending shorter path: " + shortPath.toString());
			}

			this.sendMove(shortPath);
			return true;
		} else {
			if (path.isValid()) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Sending longer path: " + path.toString());
				}

				this.sendMove(path);
				return true;
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("No paths found in goToBuilding.");
		}
		return false;
	}

	protected void sendMove(IPath path) {
		MoveCommand move = new MoveCommand();
		move.setPath(path);

		stopIfInterrupted();

		this.getExecutionService().execute(move);
	}

	protected void sendMove(List<PositionXY> path) {
		MoveCommand move = new MoveCommand();
		move.setPath(new Path(Collections.singletonList(me().getPosition(
				getWorldModel()).getID()), path));

		stopIfInterrupted();

		this.getExecutionService().execute(move);
	}

	Building currentTarget = null;

	protected IPath removeLastPathNode(IPath path,
			Collection<Building> locations) {
		IPath lastValidPath = path;

		List<EntityID> nodes = null;
		if (path.isValid()) {
			lastValidPath = path;
		} else {
			// path isn't valid, so we have to try to go to any of the
			// destinations neighbours

			List<EntityID> neighbours = new ArrayList<EntityID>();
			for (StandardEntity destination : locations) {
				neighbours.addAll(((Area) destination).getNeighbours());
			}

			if (neighbours.size() == 0) {
				return path; // the Invalid_Path
			}

			IPath newPath = getRoutingModule().findShortestPath(getID(),
					neighbours);
			if (newPath.isValid()) {
				nodes = newPath.getLocations();
				lastValidPath = newPath;
			} else {
				return path; // the Invalid_Path
			}
		}

		nodes = lastValidPath.getLocations();
		// if path has less than two nodes, don't change it
		if (nodes.size() < 2) {
			return path;
		}

		// move back 1 node, or move more if we can still extinguish
		// int moveBack = 2;
		EntityID target = nodes.get(nodes.size() - 1);
		IPath newpath = path;
		path = path.removeLastNode();
		int distance = Integer.MAX_VALUE;
		if (path.isValid()) {
			distance = getWorldModel().getDistance(
					path.getLocations().get(path.getLocations().size() - 1),
					target);
		}
		while (distance < MAX_DISTANCE && path.isValid()) {
			newpath = path;
			path = path.removeLastNode();
			if (path.isValid()) {
				distance = getWorldModel()
						.getDistance(
								path.getLocations().get(
										path.getLocations().size() - 1), target);
			}
		}
		return newpath;

	}

	public StandardEntity getLocation() {
		return ((Human) me()).getPosition(getWorldModel());
	}

	// public FirePredictor getFirePredictor() {
	// return firePredictor;
	// }
	public FastFirePredictor getFirePredictor() {
		return firePredictor;
	}

	// protected boolean newTarget() {
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER.debug("newTarget");
	// }
	//
	// // gets the 1 building with the highest context importance
	// List<Building> importantBuilding =
	// firePredictor.getBuildingsToExtinguish(10, TIMESTEPS_INTO_THE_FUTURE);
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER.debug("Important buildings list is: "
	// + importantBuilding.toString());
	// }
	// for (Building modelBuilding : importantBuilding) {
	// Building newB = (Building) getWorldModel().getEntity(
	// modelBuilding.getID());
	//
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER.debug("Planning to travel to: "
	// + newB.getID().getValue());
	// }
	//
	// if (!isBurntOut(newB) && this.isValidTarget(newB)) {
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER.debug("Building is a valid target.");
	// }
	// if (goToBuilding(newB)) {
	// // if path is valid, then command was sent, so return
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER.debug("Going to building " + newB.getID());
	// }
	// currentTarget = newB;
	// timestepsSinceCurrentTargetChanged = 0;
	// return true;
	// } else {
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER.debug("Go to building failed.");
	// }
	// }
	// }
	// }
	//
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER.debug("Checking for buildings that might be on fire.");
	// }
	// HashSet<Building> maybeOnFire = this.getBuildingsThatMightBeOnFire(this
	// .getWorldModel());
	// for (Building b : maybeOnFire) {
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER.debug("Checking building " + b.getID().getValue());
	// }
	// if (goToBuilding(b)) {
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER.debug("Going to building " + b.getID().getValue());
	// }
	// currentTarget = b;
	// return true;
	// } else {
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER.debug("Couldn't go to building.");
	// }
	// }
	// }
	//
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER.debug("newTarget returning false.");
	// }
	// return false;
	// }

	public HashSet<Building> getBuildingsThatMightBeOnFire(IAMWorldModel wm) {
		HashSet<Building> mightBeOnFire = new HashSet<Building>();
		for (StandardEntity e : wm
				.getEntitiesOfType(StandardEntityURN.BUILDING)) {

			Building b = (Building) e;
			if (!isBurntOut(b)) {
				mightBeOnFire.add(b);
			}
		}
		return mightBeOnFire;
	}

	// protected boolean changeTarget() {
	//
	// if (currentTarget == null) {
	//
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER.debug("Current target is null, so will change.");
	// }
	// return newTarget();
	// }
	//
	// /**
	// * what happens with predicted buildings???
	// */
	// // building is not on fire, so change (or burned out)
	// Building b = (Building) getWorldModel()
	// .getEntity(currentTarget.getID());
	//
	// if (this.getBuildingsThatMightBeOnFire(getWorldModel()).contains(b)) {
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER
	// .debug("Building is in list of those that might be on fire...");
	// }
	//
	// if (this.isValidTarget(b) && !isBurntOut(b)) {
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER
	// .debug("...and it is on fire, so don't change target.");
	// }
	// return false;
	// } else {
	// // TODO check if there are buildings on fire, if so, change
	// // target
	// List<Building> importantBuilding = firePredictor
	// .getBuildingsToExtinguish(10, TIMESTEPS_INTO_THE_FUTURE);
	// if (importantBuilding.size() > 0) {
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER
	// .debug("...but there are some on fire, so we will change target.");
	// }
	// return newTarget();
	// } else {
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER
	// .debug("...and there are none on fire, so we will search.");
	// }
	// currentTarget = null;
	// doDefaultSearch();
	// return true;
	// }
	// }
	// } else if (!this.isValidTarget(b)) {
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER
	// .debug("Current target is not a valid target, so changing.");
	// }
	// return newTarget();
	// }
	//
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER.debug("changeTarget returning false.");
	// }
	// return false;
	// }

	public int getWater() {
		return this.me().getWater();
	}

	public String toString() {
		return "IAMFireBrigade " + this.getID() + ", water:" + getWater()
				+ ", location:" + getLocation().getID().getValue();
	}

	List<FireSite> allFireSites = new ArrayList<FireSite>();

	public boolean isValidTarget(Building b) {

		if (b == null) {
			// System.out.println("FIRE MODEL: isValidTarget failed b = null");
			return false;
		}

		if (b.getFierynessProperty().getValue() != null
				&& (b.getFieryness() == 8 || b.getFieryness() == 4
						|| b.getFieryness() == 5 || b.getFieryness() == 6 || b
						.getFieryness() == 7)) {
			// System.out.println("FIRE MODEL: isValidTarget failed fieryness = 4-8");
			return false;
		}

		if (!b.isOnFire()) {
			// System.out.println("FIRE MODEL: isValidTarget failed b is not on fire (1)");
			return false;
		}

		if (b.isOnFire() && b.getIgnitionProperty().getValue() != null
				&& b.getFieryness() == 8) {
			// System.out.println("FIRE MODEL: isValidTarget failed b is not on fire (fieryness == 8)");
			return false;
		}

		// if getignition is not undefined and is set to false
		if (b.getIgnitionProperty().getValue() != null && !b.getIgnition()) {
			// System.out.println("FIRE MODEL: isValidTarget failed not ignited");
			return false;
		}

		// System.out.println("FIRE MODEL: isValidTarget passed");
		return true;
	}

	// @Override
	// protected StandardEntityURN getAgentType() {
	// return StandardEntityURN.FIRE_BRIGADE;
	// }

	private IPath getExtinguishPath(Building building,
			boolean alsoWithinViewDistance) {

		int distance = (alsoWithinViewDistance) ? (int) Math.min(
				ALMOST_MAX_DISTANCE, 0.9 * viewDistance) : ALMOST_MAX_DISTANCE;

		Collection<StandardEntity> possibleExtinguishPositions = getWorldModel()
				.getObjectsInRange(building, distance);

		PositionXY buildingPosition = new PositionXY(building
				.getLocation(getWorldModel()));

		// LOGGER.debug("Checking " + building);

		List<StandardEntity> extinguishPositions = new ArrayList<StandardEntity>();
		List<IRoutingLocation> destinations = new ArrayList<IRoutingLocation>();
		// IRoutingLocation destination= new RoutingLocation(id, position)
		for (StandardEntity area : possibleExtinguishPositions) {
			// LOGGER.debug("Evaluating " + area);
			if (area instanceof Area && !area.equals(building)) {
				if (area instanceof Building) {
					Building b = (Building) area;
					if (b.isFierynessDefined() && b.getFieryness() >= 1
							&& b.getFieryness() <= 3) {
						// Ignore buildings on fire
						continue;
					}
				}

				if (alsoWithinViewDistance && area instanceof Building) {
					continue;
				}

				PositionXY locationXY = new PositionXY(((Area) area).getX(),
						((Area) area).getY());
				if (locationXY.distanceTo(buildingPosition) < distance) {
					if (alsoWithinViewDistance) {
						Set<EntityID> roadsToExtinguishFrom = visibilityMap
								.getRoadsToExtinguishFrom(building.getID());
						if (roadsToExtinguishFrom != null
								&& roadsToExtinguishFrom.contains(area.getID())) {
							extinguishPositions.add(area);
							destinations.add(new RoutingLocation(area.getID(),
									locationXY));
						}
					} else {
						extinguishPositions.add(area);
						destinations.add(new RoutingLocation(area.getID(),
								locationXY));
					}
				}
			}
		}

		RoutingQuery query = new RoutingQuery(
				new RoutingLocation(me().getID()), destinations);

		return getReusableRouting().findShortestPath(query);
	}

	private Collection<Line2D> getBlockingLines(
			Collection<StandardEntity> potentialInRange, Building toIgnore) {
		// SpatialQuery query = SpatialQueryFactory.queryWithinDistance(
		// new PositionXY(location), viewDistance, Building.class);
		// Collection<StandardEntity> inRange = spatialIndex.query(query);
		Collection<Line2D> result = new FastSet<Line2D>();

		for (StandardEntity se : potentialInRange) {
			if (se instanceof Building && !se.equals(toIgnore)) {
				int[] apexes = ((Building) se).getApexList();
				List<Point2D> points = GeometryTools2D
						.vertexArrayToPoints(apexes);
				List<Line2D> lines = GeometryTools2D
						.pointsToLines(points, true);
				result.addAll(lines);
			}
		}

		return result;
	}

	private void debug(String message) {
		if (LOGGER.isDebugEnabled() || DEBUG_FIRE) {
			LOGGER.debug(message);
			// System.out.println(Calendar.getInstance().getTime().toString()+
			// " IAMFireBrigade ["+this.getID()+"] " +message);
		}
	}

	@Override
	protected void fallback(int time, ChangeSet changed) {
		// TODO move to closest fire, or if in range of one, extinguish it.
		Building closestFireBuilding = null;
		int closestFireBuildingDistance = Integer.MAX_VALUE;
		for (StandardEntity entity : this.getWorldModel()
				.getEntitiesOfType(StandardEntityURN.BUILDING,
						StandardEntityURN.AMBULANCE_CENTRE,
						StandardEntityURN.POLICE_OFFICE,
						StandardEntityURN.FIRE_STATION)) {
			Building building = (Building) entity;
			if (this.isValidTarget(building)) {
				// this building is on fire
				int distance = getWorldModel().getDistance(me().getID(),
						building.getID());
				if (distance < this.MAX_DISTANCE) {
					// within range, so extinguish
					ExtinguishCommand extinguishCommand = new ExtinguishCommand();
					extinguishCommand.setBuildingToExtinguish(building);
					extinguishCommand.setPercentageOfFullPower(1.0);

					stopIfInterrupted();

					getExecutionService().execute(extinguishCommand);
					return;
				} else {
					if (distance < closestFireBuildingDistance) {
						closestFireBuildingDistance = distance;
						closestFireBuilding = building;
					}
				}
			}
		}

		if (closestFireBuilding == null) {
			doDefaultSearch();
		} else {
			goToBuilding(closestFireBuilding);
		}
		return;
	}

	public boolean isBurntOut(Building b) {
		if (b.isFierynessDefined()) {
			if (b.getFieryness() == 8) {
				return true;
			}
		}
		return false;
	}

	public boolean isOnFire(Building b) {
		if (b.isFierynessDefined()) {
			if (b.getFieryness() >= 1 && b.getFieryness() <= 3) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected List<StandardEntityURN> getAgentTypes() {
		return Collections.singletonList(StandardEntityURN.FIRE_BRIGADE);
	}
}