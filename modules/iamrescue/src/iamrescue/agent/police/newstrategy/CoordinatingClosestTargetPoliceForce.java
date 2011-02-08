/**
 * 
 */
package iamrescue.agent.police.newstrategy;

import iamrescue.agent.AbstractIAMAgent;
import iamrescue.belief.spatial.SpatialQueryFactory;
import iamrescue.execution.command.ClearCommand;
import iamrescue.execution.command.IPath;
import iamrescue.execution.command.MoveCommand;
import iamrescue.routing.AbstractRoutingModule;
import iamrescue.routing.costs.ClearingAndMovingRoutingFunction;
import iamrescue.routing.costs.IRoutingCostFunction;
import iamrescue.routing.dijkstra.SimpleDijkstrasRoutingModule;
import iamrescue.util.PositionXY;
import iamrescue.util.blocks.BlockDetectionUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class CoordinatingClosestTargetPoliceForce extends
		AbstractIAMAgent<PoliceForce> {

	private static final String DISTANCE_KEY = "clear.repair.distance";
	private static final int VERY_CLOSE = 2000;
	private int clearDistance;
	private int veryClose;

	private static final double RECOMPUTE_THRESHOLD = 0.2;

	private ClosestTargetAllocator taskAllocator;
	private AbstractRoutingModule clearingModule;
	private Collection<EntityID> refuges;

	private static final Logger LOGGER = Logger
			.getLogger(CoordinatingClosestTargetPoliceForce.class);

	private double lastDistancePerTimeStep = 0;
	private ChangeSet lastChanged;
	private GoalGenerator goalGenerator;

	@Override
	protected void postConnect() {
		super.postConnect();
		IRoutingCostFunction clearingCostFunction = new ClearingAndMovingRoutingFunction(
				getWorldModel(), config, getSpeedInfo());
		clearingModule = new SimpleDijkstrasRoutingModule(getWorldModel(),
				clearingCostFunction, getTimer());

		taskAllocator = new ClosestTargetAllocator(getWorldModel());
		lastDistancePerTimeStep = getSpeedInfo().getDistancePerTimeStep();
		Collection<StandardEntity> refugeList = getWorldModel()
				.getEntitiesOfType(StandardEntityURN.REFUGE);
		refuges = new ArrayList<EntityID>(refugeList.size());
		for (StandardEntity standardEntity : refugeList) {
			refuges.add(standardEntity.getID());
		}
		clearDistance = config.getIntValue(DISTANCE_KEY);
		veryClose = (VERY_CLOSE < clearDistance) ? VERY_CLOSE : clearDistance;
		this.goalGenerator = new GoalGenerator(getWorldModel(), getTimer(),
				config, getSpeedInfo());

		// showRoutingViewer();
		// showWorldModelViewer();

	}

	@Override
	protected List<StandardEntityURN> getAgentTypes() {
		return Collections.singletonList(StandardEntityURN.POLICE_FORCE);
	}

	@Override
	protected void think(int time, ChangeSet changed) {
		lastChanged = changed;

		// checkRoutingRecompute();
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

		List<StandardEntity> stuckPositions = new ArrayList<StandardEntity>(
				stuckAgents.size());

		for (EntityID entityID : stuckAgents) {
			stuckPositions.add(getWorldModel().getEntity(
					getWorldModel().getStuckMemory().getStuckInfo(entityID)
							.getPosition()));
		}

		taskAllocator.computeAllocation(stuckPositions, stuckAgents,
				goalGenerator, me().getID());

		StandardEntity closestGoal = null;

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
			Collection<StandardEntity> objectsInRange = getWorldModel()
					.getObjectsInRange(me(), veryClose);

			for (StandardEntity entity : objectsInRange) {
				if (entity instanceof Blockade) {
					if (lastChanged.getChangedEntities().contains(
							entity.getID())) {
						blockToClear = (Blockade) entity;
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("I'm very close to a block: "
									+ blockToClear);
						}
						break;
					}
				}
			}
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
		doDefaultSearch();
	}
}