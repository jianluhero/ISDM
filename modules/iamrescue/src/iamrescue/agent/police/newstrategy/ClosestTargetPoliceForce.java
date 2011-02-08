/**
 * 
 */
package iamrescue.agent.police.newstrategy;

import iamrescue.agent.AbstractIAMAgent;
import iamrescue.belief.StuckMemory.StuckInfo;
import iamrescue.execution.command.ClearCommand;
import iamrescue.execution.command.IPath;
import iamrescue.execution.command.MoveCommand;
import iamrescue.util.PositionXY;
import iamrescue.util.blocks.BlockDetectionUtil;
import iamrescue.util.comparators.EntityIDComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class ClosestTargetPoliceForce extends AbstractIAMAgent<PoliceForce> {

	private static final String DISTANCE_KEY = "clear.repair.distance";
	private static final boolean SHOW_ROUTING = false;
	private static final int VERY_CLOSE = 1;
	private static final boolean CLEAR_ON_RANDOM_MOVE = true;
	private static final int RANDOM_MOVE_DISTANCE_CLEAR = 5000;
	private int clearDistance;
	private int veryClose;

	private int lastIdleSearch = -100;
	private int consecutiveSearchSteps = 0;
	private static final double SEARCH_PROBABILITY = 0.1;
	private static final int SEARCH_STEPS = 10;

	private static final double RECOMPUTE_THRESHOLD = 0.2;

	private TaskAllocator taskAllocator;
	private FutureClearingRoutingModule clearingModule;
	private Collection<EntityID> refuges;

	private static final Logger LOGGER = Logger
			.getLogger(ClosestTargetPoliceForce.class);

	private double lastDistancePerTimeStep = 0;
	private ChangeSet lastChanged;

	private Set<EntityID> stuckPositions = new FastSet<EntityID>();

	// private boolean iPreferRefuges = false;
	private boolean iPreferPoliceForces = false;
	private boolean iPreferAmbulanceTeams = false;
	private boolean iPreferFireBrigades = false;

	private boolean didDesparateClearLastTime = false;

	// private ISpatialIndex spatialIndex;

	@Override
	protected void postConnect() {
		super.postConnect();
		taskAllocator = new TaskAllocator(getWorldModel(), getTimer(), config,
				getSpeedInfo(), me().getID());
		clearingModule = taskAllocator.getClearingModule();

		lastDistancePerTimeStep = getSpeedInfo().getDistancePerTimeStep();

		Collection<StandardEntity> refugeList = getWorldModel()
				.getEntitiesOfType(StandardEntityURN.REFUGE);
		refuges = new ArrayList<EntityID>(refugeList.size());
		for (StandardEntity standardEntity : refugeList) {
			refuges.add(standardEntity.getID());
		}

		clearDistance = config.getIntValue(DISTANCE_KEY);

		veryClose = (VERY_CLOSE < clearDistance) ? VERY_CLOSE : clearDistance;

		// showRoutingViewer();
		// showWorldModelViewer();

		if (SHOW_ROUTING) {
			Collection<StandardEntity> entitiesOfType = getWorldModel()
					.getEntitiesOfType(StandardEntityURN.POLICE_FORCE);
			List<StandardEntity> entities = new ArrayList<StandardEntity>(
					entitiesOfType);
			Collections.sort(entities, EntityIDComparator.DEFAULT_INSTANCE);
			if (entities.get(0).equals(me())) {
				showRoutingViewer();
			}
		}

		// spatialIndex = new SpatialIndex(getWorldModel());

	}

	@Override
	protected List<StandardEntityURN> getAgentTypes() {
		return Collections.singletonList(StandardEntityURN.POLICE_FORCE);
	}

	@Override
	protected void think(int time, ChangeSet changed) {

		lastChanged = changed;

		checkRoutingRecompute();
		boolean ok = false;

		if (me().getDamage() > 0) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("I am damaged. Heading to refuge");
			}
			clearPathTo(refuges);
			return;
		}

		Set<EntityID> stuckAgents = getWorldModel().getStuckMemory()
				.getStuckAgents();

		if (stuckAgents.size() > 0) {
			// Get closest
			double closestDistance = Double.POSITIVE_INFINITY;
			EntityID closest = null;
			PositionXY myXY = new PositionXY(me().getLocation(getWorldModel()));
			for (EntityID id : stuckAgents) {
				StuckInfo stuckInfo = getWorldModel().getStuckMemory()
						.getStuckInfo(id);
				PositionXY xy = new PositionXY(getWorldModel().getEntity(
						stuckInfo.getPosition()).getLocation(getWorldModel()));
				double distance = xy.distanceTo(myXY);

				if (distance < closestDistance) {
					closestDistance = distance;
					closest = stuckInfo.getPosition();
				}
			}
			if (closest != null) {
				LOGGER.info("Moving to stuck agent at position: " + closest);
				clearPathTo(closest);
				return;
			}
		}

		StandardEntity closestGoal = taskAllocator.computeClosestGoal(me());

		// 
		
		if (closestGoal != null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Heading towards goal "
						+ closestGoal.getFullDescription());
			}
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("No valid goal found");
			}
		}

		if (!(closestGoal instanceof Human)) {
			boolean doIdleSearch = false;

			if (lastIdleSearch == getTimer().getTime() - 1
					&& consecutiveSearchSteps < SEARCH_STEPS) {
				consecutiveSearchSteps++;
				doIdleSearch = true;
			} else {
				if (Math.random() <= SEARCH_PROBABILITY) {
					doIdleSearch = true;
					consecutiveSearchSteps = 1;
				}
			}

			if (doIdleSearch) {
				lastIdleSearch = getTimer().getTime();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("No particularly important goals, "
							+ "so decided to search randomly");
				}
				doDefaultSearch();
				return;
			}
		}

		if (closestGoal != null) {
			if (closestGoal.equals(me())) {
				// Need to clear path to refuge
				ok = clearPathTo(refuges);
			} else {
				ok = clearPathTo(closestGoal.getID());
			}
		}

		if (!ok) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Doing searching instead.");
			}
			doDefaultSearch();
		}
	}

	private void checkRoutingRecompute() {
		double distancePerTimeStep = getSpeedInfo().getDistancePerTimeStep();
		if (lastDistancePerTimeStep != 0
				&& Math.abs(distancePerTimeStep - lastDistancePerTimeStep)
						/ lastDistancePerTimeStep >= RECOMPUTE_THRESHOLD) {
			long time = 0;
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Recomputing graph");
				time = System.nanoTime();
			}
			Collection<StandardEntity> areas = getWorldModel()
					.getEntitiesOfType(StandardEntityURN.ROAD,
							StandardEntityURN.BUILDING,
							StandardEntityURN.FIRE_STATION,
							StandardEntityURN.POLICE_OFFICE,
							StandardEntityURN.AMBULANCE_CENTRE,
							StandardEntityURN.REFUGE);
			for (StandardEntity area : areas) {
				clearingModule.forceRecompute((Area) area);
			}
			lastDistancePerTimeStep = distancePerTimeStep;
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Recomputing graph took "
						+ (System.nanoTime() - time) + "ns");
			}
		}

	}

	/**
	 * @param id
	 */
	private boolean clearPathTo(EntityID id) {
		return clearPathTo(Collections.singleton(id));
	}

	private boolean clearPathTo(Collection<EntityID> targets) {
		IPath path = clearingModule.findShortestPath(me().getID(), targets);
		if (!path.isValid()) {
			return false;
		}

		PositionXY myPosition = new PositionXY(me()
				.getLocation(getWorldModel()));

		List<Blockade> blocksToClear = BlockDetectionUtil
				.findObstructingBlockades(path, getWorldModel());

		Blockade blockToClear = null;

		for (Blockade block : blocksToClear) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Block: " + block);
			}
			if (BlockDetectionUtil.isWithinDistanceToBlockade(myPosition,
					block, clearDistance)) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("In range!");
				}
				if (lastChanged.getChangedEntities().contains(block.getID())) {
					blockToClear = block;
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Found block on path to goal: "
								+ blockToClear);
					}
				}
				break;
			} else {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Not in range: "
							+ BlockDetectionUtil.getDistanceToBlockade(
									myPosition, block));
				}
			}
		}

		if (blockToClear == null) {
			// Check if very close to block
			/*
			 * Collection<StandardEntity> query = getSpatialIndex().query(
			 * SpatialQueryFactory.queryWithinDistance(myPosition, veryClose,
			 * Blockade.class));
			 */
			Collection<StandardEntity> objectsInRange = getWorldModel()
					.getObjectsInRange(me(), clearDistance);

			int tempVeryClose = veryClose;
			if (CLEAR_ON_RANDOM_MOVE
					&& (didDesparateClearLastTime || getExecutionService()
							.getLastRandomMoveTime() >= getTimer().getTime() - 2)
					&& getTimer().getTime() > 3) {
				didDesparateClearLastTime = true;
				tempVeryClose = Math.min(clearDistance,
						RANDOM_MOVE_DISTANCE_CLEAR);
				if (getExecutionService().getConsecutiveRandomSteps() > 5) {
					tempVeryClose = clearDistance;
				}

				if (LOGGER.isDebugEnabled()) {
					LOGGER
							.debug("Trying a desparate clear due to random move. "
									+ "Setting distance to " + tempVeryClose);
				}
			} else {
				// Are any non-police near me?
				// boolean nonPoliceNearby = false;
				for (StandardEntity standardEntity : objectsInRange) {
					if ((standardEntity instanceof FireBrigade)
							|| (standardEntity instanceof AmbulanceTeam)) {
						tempVeryClose = clearDistance / 2;
						break;
					}
				}
			}

			// Collection<StandardEntity> objectsInRange = getWorldModel()
			// .getObjectsInRange(me(), tempVeryClose);

			for (StandardEntity entity : objectsInRange) {
				if (entity instanceof Road) {
					Road road = (Road) entity;
					if (road.isBlockadesDefined()) {
						List<EntityID> blockades = road.getBlockades();
						for (EntityID blockID : blockades) {
							Blockade block = (Blockade) getWorldModel()
									.getEntity(blockID);
							if (lastChanged.getChangedEntities().contains(
									block.getID())
									&& BlockDetectionUtil
											.isWithinDistanceToBlockade(
													myPosition, block,
													tempVeryClose)) {
								blockToClear = (Blockade) block;
								if (LOGGER.isDebugEnabled()) {
									LOGGER.debug("I'm very close to a block: "
											+ blockToClear);
								}
								break;
							}
						}
					}
				}
			}

			// This is reset if no block was found
			didDesparateClearLastTime = false;
		} else {
			didDesparateClearLastTime = false;
		}

		if (blockToClear != null) {
			getExecutionService().execute(new ClearCommand(blockToClear));
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Moving towards goal: " + path.toVerboseString());
			}
			getExecutionService().execute(new MoveCommand(path));
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.agent.AbstractIAMAgent#fallback(int,
	 * rescuecore2.worldmodel.ChangeSet)
	 */
	@Override
	protected void fallback(int time, ChangeSet changed) {
		// TODO Auto-generated method stub

	}
}