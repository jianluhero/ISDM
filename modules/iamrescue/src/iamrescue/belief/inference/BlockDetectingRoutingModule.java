/**
 * 
 */
package iamrescue.belief.inference;

import iamrescue.agent.ISimulationTimer;
import iamrescue.agent.ITimeStepListener;
import iamrescue.belief.IAMWorldModel;
import iamrescue.execution.IExecutionService;
import iamrescue.execution.command.IIAMAgentCommand;
import iamrescue.execution.command.IPath;
import iamrescue.execution.command.MoveCommand;
import iamrescue.routing.IRoutingModule;
import iamrescue.routing.WorldModelConverter;
import iamrescue.routing.costs.BlockableRoutingCostFunction;
import iamrescue.routing.costs.IRoutingCostChangeListener;
import iamrescue.routing.costs.IRoutingCostFunction;
import iamrescue.routing.dijkstra.BidirectionalDijkstrasRoutingModule;
import iamrescue.routing.dijkstra.SimpleDijkstrasRoutingModule;
import iamrescue.routing.dijkstra.SimpleGraph;
import iamrescue.routing.queries.IRoutingQuery;
import iamrescue.routing.util.ISpeedInfo;
import iamrescue.util.PositionXY;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class BlockDetectingRoutingModule implements IRoutingCostChangeListener,
		IRoutingModule, ITimeStepListener {

	private IExecutionService executionService;
	private EntityID myID;
	// private SimpleTimeLearningRoutingCostFunction learningFunction;
	// private ISpatialIndex spatial;
	private ISimulationTimer timer;
	private BlockableRoutingCostFunction blockableRouting;
	private PositionXY currentPositionEntrance = null;
	private ISpeedInfo speedInfo;
	private SimpleDijkstrasRoutingModule parent;
	private IAMWorldModel worldModel;

	private static final double SLOW_THRESHOLD = 0.1;
	private static final double NO_MOVE_THRESHOLD = 0.01;
	private static final int MAX_JAM_AVOID_TIME = 6;
	private static final int MAX_BLOCK_AVOID_TIME = 20;
	private static final int NEARBY = 5000;
	private static final int SLOW_DOWN_MULTIPLIER = 100;
	private static final int BLOCK_MULTIPLIER = 100000;
	private static final int CONSECUTIVE_RANDOM_STEPS_NO_MOVE_THRESHOLD = 5;

	private static final Logger LOGGER = Logger
			.getLogger(BlockDetectingRoutingModule.class);

	public BlockDetectingRoutingModule(IAMWorldModel worldModel,
			IRoutingCostFunction routingCostFunction, ISimulationTimer timer,
			IExecutionService executionService, EntityID myID,
			ISpeedInfo speedInfo, boolean bidirectional) {
		this.blockableRouting = new BlockableRoutingCostFunction(
				routingCostFunction, timer);
		if (bidirectional) {
			parent = new BidirectionalDijkstrasRoutingModule(worldModel,
					blockableRouting, timer);
		} else {
			parent = new SimpleDijkstrasRoutingModule(worldModel,
					blockableRouting, timer);
		}
		// this.spatial = spatial;
		this.worldModel = worldModel;
		this.executionService = executionService;
		this.myID = myID;
		this.timer = timer;
		blockableRouting.setListener(this);
		timer.addTimeStepListener(this);

		this.speedInfo = speedInfo;
		/*
		 * if (routingCostFunction instanceof
		 * SimpleTimeLearningRoutingCostFunction) { this.learningFunction =
		 * (SimpleTimeLearningRoutingCostFunction) routingCostFunction; } else {
		 * this.learningFunction = new SimpleTimeLearningRoutingCostFunction(
		 * worldModel, false, myID, timer); }
		 */
	}

	@Override
	public void allRoutingCostsChanged() {
		Collection<StandardEntity> areas = worldModel
				.getEntitiesOfType(StandardEntityURN.ROAD,
						StandardEntityURN.BUILDING,
						StandardEntityURN.AMBULANCE_CENTRE,
						StandardEntityURN.REFUGE,
						StandardEntityURN.FIRE_STATION,
						StandardEntityURN.POLICE_OFFICE);
		for (StandardEntity entity : areas) {
			parent.getConverter().recomputeArea((Area) entity);
		}
	}

	@Override
	public void routingCostChanged(Area area) {
		parent.getConverter().recomputeArea(area);
	}

	@Override
	public void notifyTimeStepStarted(int timeStep) {
		parent.notifyTimeStepStarted(timeStep);

		if (timeStep < 4
				|| timeStep == executionService.getLastRandomMoveTime() + 1) {
			// Don't do anything
			return;
		}

		// Do block detection now.
		IIAMAgentCommand lastSubmittedCommand = executionService
				.getLastSubmittedCommand();
		if (lastSubmittedCommand instanceof MoveCommand) {
			MoveCommand move = (MoveCommand) lastSubmittedCommand;
			IPath path = move.getPath();

			// Get distance travelled
			int distanceJustTravelled = speedInfo.getLastTimeStepDistance();
			Human me = (Human) worldModel.getEntity(myID);

			if (!path.getStart().equals(me.getPosition())) {
				// Find entrance
				List<EntityID> locations = path.getLocations();
				List<PositionXY> xyPath = path.getXYPath();
				for (int i = 1; i < locations.size(); i++) {
					if (locations.get(i).equals(me.getPosition())) {
						currentPositionEntrance = xyPath.get(i);
						// System.out.println("Setting entrance to : "
						// + currentPositionEntrance);
					}
				}
			}

			boolean randomlyStepping = false;

			if (executionService.getLastRandomMoveTime() >= timer.getTime() - 1) {
				if (executionService.getConsecutiveRandomSteps() >= CONSECUTIVE_RANDOM_STEPS_NO_MOVE_THRESHOLD) {
					randomlyStepping = true;
				}
			}

			if (randomlyStepping
					|| distanceJustTravelled <= SLOW_THRESHOLD
							* speedInfo.getDistancePerTimeStep()) {
				boolean verySlow = randomlyStepping
						|| distanceJustTravelled <= NO_MOVE_THRESHOLD
								* speedInfo.getDistancePerTimeStep();
				// Did not travel very far
				// Check if this was intentional
				if (!me.getPosition().equals(path.getDestination())) {
					// No, agent tried to travel further
					// Is there a block nearby?
					EntityID position = me.getPosition();
					Area area = (Area) worldModel.getEntity(position);
					if (area.isBlockadesDefined()
							&& area.getBlockades().size() > 0) {
						// Yes - likely to be stuck. Note this in routing
						markAsBlocked(area, path,
								(int) (timer.getTime() + 1 + Math.random()
										* MAX_BLOCK_AVOID_TIME),
								verySlow ? BLOCK_MULTIPLIER
										: SLOW_DOWN_MULTIPLIER);
						LOGGER
								.warn("Detected block due to blockade at "
										+ area);
					} else {

						/*
						 * // Are there other agents nearby?
						 * Collection<StandardEntity> near = spatial
						 * .query(SpatialQueryFactory.queryWithinDistance( new
						 * PositionXY(me .getLocation(worldModel)), NEARBY,
						 * Human.class));
						 */
						Collection<StandardEntity> objectsInRange = worldModel
								.getObjectsInRange(me, NEARBY);
						boolean foundHuman = false;
						for (StandardEntity standardEntity : objectsInRange) {
							if (standardEntity instanceof Human) {
								foundHuman = true;
								break;
							}
						}

						if (foundHuman) {
							markAsBlocked(area, path,
									(int) (timer.getTime() + 1 + Math.random()
											* MAX_JAM_AVOID_TIME),
									verySlow ? BLOCK_MULTIPLIER
											: SLOW_DOWN_MULTIPLIER);
							LOGGER.warn("Detected possible congestion at "
									+ area + " blocking route for short time.");
						} else {
							markAsBlocked(area, path,
									(int) (timer.getTime() + 1 + Math.random()
											* MAX_BLOCK_AVOID_TIME),
									verySlow ? BLOCK_MULTIPLIER
											: SLOW_DOWN_MULTIPLIER);
							LOGGER.warn("Detected block due to possibly"
									+ " difficult geometry at " + area);
						}
					}
				}
			}

		}
	}

	/**
	 * @param position
	 * @param path
	 * @param maxValue
	 */
	private void markAsBlocked(Area area, IPath path, int validUntil,
			double multiplier) {

		List<EntityID> locations = path.getLocations();
		List<PositionXY> xyPath = path.getXYPath();

		for (int i = 0; i < locations.size(); i++) {
			if (locations.get(i).equals(area.getID())) {
				PositionXY toBlock = xyPath.get(i + 1);
				if (currentPositionEntrance != null
						&& toBlock.equals(currentPositionEntrance)) {
					LOGGER.debug("ignoring " + toBlock);
					// Don't block exit to this
					return;
				}
				blockableRouting.addTemporaryBlock(area, toBlock, validUntil,
						multiplier);
				return;
			}
		}

		LOGGER.error("Attempting to add temporary block to area, "
				+ "but could not find this in path. Area: " + area + ", path: "
				+ path + " valid until: " + validUntil);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.routing.IRoutingModule#areConnected(rescuecore2.worldmodel.
	 * EntityID, rescuecore2.worldmodel.EntityID)
	 */
	@Override
	public boolean areConnected(EntityID from, EntityID to) {
		return parent.areConnected(from, to);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.routing.IRoutingModule#findShortestPath(iamrescue.routing.queries
	 * .IRoutingQuery)
	 */
	@Override
	public IPath findShortestPath(IRoutingQuery query) {
		return parent.findShortestPath(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.routing.IRoutingModule#findShortestPath(java.util.List)
	 */
	@Override
	public List<IPath> findShortestPath(List<IRoutingQuery> queries) {
		return parent.findShortestPath(queries);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.routing.IRoutingModule#findShortestPath(rescuecore2.worldmodel
	 * .EntityID, java.util.Collection)
	 */
	@Override
	public IPath findShortestPath(EntityID from,
			Collection<EntityID> possibleDestinations) {
		return parent.findShortestPath(from, possibleDestinations);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.routing.IRoutingModule#findShortestPath(rescuecore2.worldmodel
	 * .EntityID, iamrescue.util.PositionXY, java.util.Collection)
	 */
	@Override
	public IPath findShortestPath(EntityID from, PositionXY exactPosition,
			Collection<EntityID> possibleDestinations) {
		return parent.findShortestPath(from, exactPosition,
				possibleDestinations);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.routing.IRoutingModule#findShortestPath(rescuecore2.worldmodel
	 * .EntityID, rescuecore2.worldmodel.EntityID)
	 */
	@Override
	public IPath findShortestPath(EntityID from, EntityID destination) {
		return parent.findShortestPath(from, destination);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.routing.IRoutingModule#findShortestPath(rescuecore2.worldmodel
	 * .EntityID, iamrescue.util.PositionXY, rescuecore2.worldmodel.EntityID,
	 * iamrescue.util.PositionXY)
	 */
	@Override
	public IPath findShortestPath(EntityID from, PositionXY fromPosition,
			EntityID destination, PositionXY destinationPosition) {
		return parent.findShortestPath(from, fromPosition, destination,
				destinationPosition);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.routing.IRoutingModule#getConverter()
	 */
	@Override
	public WorldModelConverter getConverter() {
		return parent.getConverter();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.routing.IRoutingModule#getRoutingCostFunction()
	 */
	@Override
	public IRoutingCostFunction getRoutingCostFunction() {
		return parent.getRoutingCostFunction();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.routing.IRoutingModule#getRoutingGraph()
	 */
	@Override
	public SimpleGraph getRoutingGraph() {
		return parent.getRoutingGraph();
	}

	@Override
	public IPath findShortestPath(Entity from, Entity to) {
		return parent.findShortestPath(from, to);
	}

	@Override
	public IPath findShortestPath(Entity from, Collection<? extends Entity> to) {
		return parent.findShortestPath(from, to);
	}
}