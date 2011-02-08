package iamrescue.routing;

import iamrescue.agent.AbstractIAMAgent;
import iamrescue.agent.ISimulationTimer;
import iamrescue.agent.ITimeStepListener;
import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.entities.RoutingInfoBlockade;
import iamrescue.execution.command.IPath;
import iamrescue.routing.WorldModelConverter.SimpleGraphNode;
import iamrescue.routing.WorldModelConverter.WorldModelArea;
import iamrescue.routing.costs.IRoutingCostFunction;
import iamrescue.routing.dijkstra.PathSolution;
import iamrescue.routing.dijkstra.SimpleGraph;
import iamrescue.routing.queries.IRoutingLocation;
import iamrescue.routing.queries.IRoutingQuery;
import iamrescue.routing.queries.QueryFactory;
import iamrescue.routing.queries.RoutingLocation;
import iamrescue.routing.queries.RoutingQuery;
import iamrescue.util.PositionXY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.EntityListener;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.WorldModel;
import rescuecore2.worldmodel.WorldModelListener;

public abstract class AbstractRoutingModule implements IRoutingModule,
		EntityListener, WorldModelListener<StandardEntity>, ITimeStepListener {

	private final Map<Integer, PositionXY> EMPTY_MAP = new FastMap<Integer, PositionXY>();
	private static final int[] EMPTY_INT_ARRAY = new int[0];

	protected IAMWorldModel worldModel;
	protected IRoutingCostFunction routingCostFunction;
	protected WorldModelConverter converter;
	protected SimpleGraph graph;
	private Set<EntityID> changedEntities = new FastSet<EntityID>();

	private static final Logger LOGGER = Logger
			.getLogger(AbstractRoutingModule.class);

	public AbstractRoutingModule(IAMWorldModel worldModel,
			IRoutingCostFunction roadCostFunction, ISimulationTimer timer) {

		timer.addTimeStepListener(this);
		this.worldModel = worldModel;
		this.routingCostFunction = roadCostFunction;

		// Listen to all entities for changes
		for (StandardEntity se : worldModel.getEntitiesOfType(
				StandardEntityURN.BUILDING, StandardEntityURN.ROAD,
				StandardEntityURN.FIRE_STATION,
				StandardEntityURN.AMBULANCE_CENTRE,
				StandardEntityURN.POLICE_OFFICE, StandardEntityURN.REFUGE,
				StandardEntityURN.BLOCKADE)) {
			se.addEntityListener(this);
		}

		worldModel.addWorldModelListener(this);

		createGraph();

		// System.out.println("Size of graph is " + graph.getNodes().size());
	}

	/*
	 * public AbstractRoutingModule(IAMWorldModel worldModel,
	 * IRoutingCostFunction roadCostFunction, WorldModelConverter converter,
	 * SimpleGraph graph, ISimulationTimer timer) {
	 * 
	 * timer.addTimeStepListener(this); this.worldModel = worldModel;
	 * this.routingCostFunction = roadCostFunction;
	 * 
	 * // Listen to all entities for changes for (StandardEntity se :
	 * worldModel.getEntitiesOfType( StandardEntityURN.BUILDING,
	 * StandardEntityURN.ROAD, StandardEntityURN.FIRE_STATION,
	 * StandardEntityURN.AMBULANCE_CENTRE, StandardEntityURN.POLICE_OFFICE,
	 * StandardEntityURN.REFUGE, StandardEntityURN.BLOCKADE)) {
	 * se.addEntityListener(this); }
	 * 
	 * worldModel.addWorldModelListener(this);
	 * 
	 * if (converter != null && graph != null) { this.converter = converter;
	 * this.graph = graph; } else { createGraph(); } }
	 */

	/**
	 * @return the worldModel
	 */
	public IAMWorldModel getWorldModel() {
		return worldModel;
	}

	@Override
	public void entityAdded(WorldModel<? extends StandardEntity> model,
			StandardEntity e) {
		if (e instanceof Blockade) {
			e.addEntityListener(this);
		}

	}

	@Override
	public void entityRemoved(WorldModel<? extends StandardEntity> model,
			StandardEntity e) {
		if (e instanceof Blockade) {
			e.removeEntityListener(this);
		}
	}

	private void createGraph() {
		this.converter = new WorldModelConverter(worldModel,
				routingCostFunction);
		this.graph = converter.getGraph();
	}

	/**
	 * @return the graph
	 */
	public SimpleGraph getGraph() {
		return graph;
	}

	/**
	 * @return the converter
	 */
	public WorldModelConverter getConverter() {
		return converter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see routing.IRoutingModule#areConnected(rescuecore2.worldmodel.EntityID,
	 * rescuecore2.worldmodel.EntityID)
	 */
	@Override
	public boolean areConnected(EntityID from, EntityID to) {
		Entity fromEntity = worldModel.getEntity(from);
		Entity toEntity = worldModel.getEntity(to);
		while (fromEntity instanceof Human) {
			fromEntity = worldModel.getEntity(((Human) fromEntity)
					.getPosition());
		}
		while (fromEntity instanceof Blockade) {
			fromEntity = worldModel.getEntity(((Blockade) fromEntity)
					.getPosition());
		}
		while (toEntity instanceof Human) {
			toEntity = worldModel.getEntity(((Human) toEntity).getPosition());
		}
		while (toEntity instanceof Blockade) {
			toEntity = worldModel
					.getEntity(((Blockade) toEntity).getPosition());
		}
		return converter.onSameComponent(fromEntity.getID().getValue(),
				toEntity.getID().getValue());
	}

	@Override
	public IPath findShortestPath(IRoutingQuery query) {

		long start = System.nanoTime();

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Running routing query: " + query);
		}

		EntityID from = query.getStartLocation().getID();

		Set<EntityID> possibleStartPositions = FastSet.newInstance();
		possibleStartPositions.add(from);

		StandardEntity position = worldModel.getEntity(from);
		while (position instanceof Human) {
			Human human = (Human) position;
			if (!human.isPositionDefined()) {
				throw new NullPointerException("Start location " + human
						+ " does not have its position defined.");
			}
			EntityID positionFrom = human.getPosition();
			possibleStartPositions.add(positionFrom);
			position = worldModel.getEntity(positionFrom);
		}
		while (position instanceof Blockade) {
			Blockade block = (Blockade) position;
			if (!block.isPositionDefined()) {
				throw new NullPointerException("Start location " + block
						+ " does not have its position defined.");
			}
			EntityID positionFrom = block.getPosition();
			possibleStartPositions.add(positionFrom);
			position = worldModel.getEntity(positionFrom);
		}

		List<Integer> nodes = new ArrayList<Integer>();
		List<Double> costs = new ArrayList<Double>();

		Map<Integer, Double> nodeCosts = FastMap.newInstance();
		Map<Integer, IRoutingLocation> targetMap = FastMap.newInstance();

		for (IRoutingLocation point : query.getDestinationLocations()) {
			boolean done = false;
			EntityID id = point.getID();
			do {
				if (possibleStartPositions.contains(id)) {
					// Already at destination!
					List<PositionXY> positions = new ArrayList<PositionXY>();
					positions.add(findRepresentativePoint(query
							.getStartLocation()));
					positions.add(findRepresentativePoint(point));
					return new Path(Collections.singletonList(getArea(point)),
							positions);
				}
				StandardEntity entity = worldModel.getEntity(id);
				if (entity instanceof Human) {
					Human h = (Human) entity;
					if (!h.isPositionDefined()) {
						throw new NullPointerException("Human " + h
								+ " does not have its position defined.");
					}
					id = h.getPosition();
				} else if (entity instanceof Blockade) {
					Blockade b = (Blockade) entity;
					if (!b.isPositionDefined()) {
						throw new NullPointerException("Blockade " + b
								+ " does not have its position defined.");
					}
					id = b.getPosition();
				} else {
					done = true;
				}
			} while (!done);
			Pair<List<Integer>, List<Double>> costsForThis = computeSearchNodes(point);
			List<Integer> theseNodes = costsForThis.first();
			List<Double> theseCosts = costsForThis.second();
			for (int i = 0; i < theseNodes.size(); i++) {
				int node = theseNodes.get(i);
				double cost = theseCosts.get(i);
				Double existingCost = nodeCosts.get(node);
				if (existingCost == null || existingCost > cost) {
					nodeCosts.put(node, cost);
					targetMap.put(node, point);
				}
			}
		}

		for (Entry<Integer, Double> target : nodeCosts.entrySet()) {
			nodes.add(target.getKey());
			costs.add(target.getValue());
		}

		IPath path = findShortestPath(query.getStartLocation(), nodes, costs,
				targetMap);

		/*
		 * if (!nodes.contains(path.getLocations().get(
		 * path.getLocations().size() - 1).getValue())) {
		 * System.out.println("oops"); path =
		 * findShortestPath(query.getStartLocation(), nodes, costs, targetMap);
		 * }
		 */

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Completed query in " + (System.nanoTime() - start)
					+ "ns");
			LOGGER.trace("Path: " + path);
		}

		return path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see newrouting.IRoutingModule#findShortestPath(java.util.List)
	 */
	@Override
	public List<IPath> findShortestPath(List<IRoutingQuery> queries) {
		List<IPath> solutions = new ArrayList<IPath>(queries.size());
		for (IRoutingQuery query : queries) {
			solutions.add(findShortestPath(query));
		}
		return solutions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * newrouting.IRoutingModule#findShortestPath(rescuecore2.worldmodel.EntityID
	 * , java.util.Collection)
	 */
	@Override
	public IPath findShortestPath(EntityID from,
			Collection<EntityID> possibleDestinations) {
		return findShortestPath(QueryFactory.createQuery(from,
				possibleDestinations));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * newrouting.IRoutingModule#findShortestPath(rescuecore2.worldmodel.EntityID
	 * , util.PositionXY, java.util.Collection)
	 */
	@Override
	public IPath findShortestPath(EntityID from, PositionXY exactPosition,
			Collection<EntityID> possibleDestinations) {
		return findShortestPath(QueryFactory.createQuery(from, exactPosition,
				possibleDestinations));
	}

	private EntityID getArea(IRoutingLocation position) {
		StandardEntity entity = worldModel.getEntity(position.getID());
		while (entity instanceof Human) {
			entity = ((Human) entity).getPosition(worldModel);
		}
		while (entity instanceof Blockade) {
			entity = worldModel.getEntity(((Blockade) entity).getPosition());
		}
		return entity.getID();
	}

	public IPath findShortestPath(EntityID from, EntityID destination) {
		return findShortestPath(QueryFactory.createQuery(from, destination));
	}

	public IPath findShortestPath(Entity from, Entity destination) {
		return findShortestPath(QueryFactory.createQuery(from, destination));
	}

	public IPath findShortestPath(Entity from,
			Collection<? extends Entity> destinations) {
		return findShortestPath(QueryFactory.createQuery(from, destinations));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * newrouting.IRoutingModule#findShortestPath(rescuecore2.worldmodel.EntityID
	 * , util.PositionXY, rescuecore2.worldmodel.EntityID, util.PositionXY)
	 */
	@Override
	public IPath findShortestPath(EntityID from, PositionXY fromPosition,
			EntityID destination, PositionXY destinationPosition) {
		return findShortestPath(new RoutingQuery(new RoutingLocation(from,
				fromPosition), new RoutingLocation(destination,
				destinationPosition)));
	}

	/**
	 * Finds search nodes related to a point.
	 * 
	 * @param point
	 *            The point
	 * @return Pair of routing nodes and respective costs.
	 */
	protected Pair<List<Integer>, List<Double>> computeSearchNodes(
			IRoutingLocation location) {

		List<Integer> nodes = new ArrayList<Integer>();
		List<Double> costs = new ArrayList<Double>();

		StandardEntity entity = worldModel.getEntity(location.getID());
		PositionXY position = (location.hasPositionDefined()) ? location
				.getPositionXY() : null;

		if (entity instanceof Human) {
			do {
				Human human = (Human) entity;
				if (position == null) {
					position = new PositionXY(human.getLocation(worldModel));
				}
				entity = worldModel.getEntity(human.getPosition());
			} while (entity instanceof Human);
		} else if (entity instanceof Blockade) {
			Blockade blockade = (Blockade) entity;
			if (position == null) {
				position = new PositionXY(blockade.getLocation(worldModel));
			}
			entity = worldModel.getEntity(blockade.getPosition());
		}

		WorldModelArea wmArea = converter.getWorldModelArea(entity.getID()
				.getValue());
		// Get all nodes
		Set<Integer> simpleNodes = wmArea.getSimpleNeighbours();
		for (int neighbour : simpleNodes) {
			double cost = 0;
			if (position != null) {
				cost = routingCostFunction.getTravelCost((Area) entity,
						position, converter.getSimpleGraphNode(neighbour)
								.getRepresentativePoint());
			}
			nodes.add(neighbour);
			costs.add(cost);
		}
		return new Pair<List<Integer>, List<Double>>(nodes, costs);
	}

	private IPath findShortestPath(IRoutingLocation from,
			List<Integer> simpleTargets, List<Double> simpleCosts,
			Map<Integer, IRoutingLocation> finalTargets) {
		AbstractIAMAgent.stopIfInterrupted();
		IRoutingAlgorithm algorithm = obtainSolver(from);
		PathSolution solution = algorithm.getShortestPath(simpleTargets,
				simpleCosts);

		// Additionally check if direct move is possible.
		/*
		 * EntityID id = from.getID();
		 * 
		 * converter.get
		 * 
		 * PositionXY fromPosition; if (from.hasPositionDefined()) {
		 * fromPosition = from.getPositionXY(); } else { fromPosition = new
		 * PositionXY(worldModel.getEntity(id).getLocation( worldModel)); }
		 * 
		 * for (Entry<Integer, IRoutingLocation> finalTarget : finalTargets
		 * .entrySet()) {
		 * 
		 * }
		 */

		if (solution.getCost() == Double.POSITIVE_INFINITY) {
			return Path.INVALID_PATH;
		} else {
			Path path = createPath(from, solution, finalTargets);
			path.setCost(solution.getCost());
			return path;
		}
	}

	private PositionXY findRepresentativePoint(IRoutingLocation location) {
		if (location.hasPositionDefined()) {
			return location.getPositionXY();
		} else {
			return new PositionXY(worldModel.getEntity(location.getID())
					.getLocation(worldModel));
		}
	}

	/**
	 * @param solution
	 * @param finalTargets
	 * @return
	 */
	private Path createPath(IRoutingLocation from, PathSolution solution,
			Map<Integer, IRoutingLocation> finalTargets) {

		// ids : simple node IDs
		int[] ids = solution.getPathIDs();
		if (ids.length == 0) {
			// Empty!
			return new Path(new ArrayList<EntityID>(),
					new ArrayList<PositionXY>());
		}

		// World IDs
		List<EntityID> entities = new ArrayList<EntityID>();
		List<PositionXY> xyPositions = new FastList<PositionXY>();
		EntityID lastID = getArea(from);
		entities.add(lastID);

		SimpleGraphNode node = converter.getSimpleGraphNode(ids[0]);
		// if (!from.equals(node.getRepresentativePoint())) {
		// }

		// Note that first two positions could be the same if starting exactly
		// on edge.
		// This is required to ensure consistent behaviour in
		// ABstractRoutingCOstFunction.
		xyPositions.add(findRepresentativePoint(from));

		xyPositions.add(node.getRepresentativePoint());

		int thisOne = node.getOtherNeighbour(lastID.getValue());
		entities.add(new EntityID(thisOne));
		int lastEntity = thisOne;

		// String debug = converter.getSimpleGraphNode(ids[0]) + "_";

		for (int i = 1; i < ids.length; i++) {
			// debug += converter.getSimpleGraphNode(ids[i]) + "_";
			SimpleGraphNode next = converter.getSimpleGraphNode(ids[i]);

			int neighbour1 = next.getAreaNeighbour1();
			int neighbour2 = next.getAreaNeighbour2();

			int counter = 0;
			boolean repeat;
			do {
				repeat = false;
				if (lastEntity == neighbour1) {
					if (lastEntity == neighbour2) {
						// Ignore this - we're traversing an interior routing
						// node.
						break;
					} else {
						lastEntity = neighbour2;
						entities.add(new EntityID(lastEntity));
						xyPositions.add(next.getRepresentativePoint());
					}
				} else if (lastEntity == neighbour2) {
					lastEntity = neighbour1;
					entities.add(new EntityID(lastEntity));
					xyPositions.add(next.getRepresentativePoint());
				} else {
					// Border case - this happens when moving between edge nodes
					// repeatedly within the same shape
					counter++;
					// Need to go back 2 nodes in this case.
					if (entities.size() <= 1) {
						LOGGER.error("Warning: entities is too small. From:"
								+ from + ", solution:"
								+ Arrays.toString(solution.getPathIDs())
								+ ", entities:" + entities + ", next:" + next);
						return Path.INVALID_PATH;
					}
					entities.remove(entities.size() - 1);
					xyPositions.remove(xyPositions.size() - 1);
					lastEntity = entities.get(entities.size() - 1).getValue();
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Retracing path due to interior routing.");
					}
					repeat = true;
				}
			} while (counter == 1 && repeat);

			if (counter == 2) {
				LOGGER.error("Something bad happened. "
						+ "Counter should not be > 1.");
			}

			// lastEntity = next.getOtherNeighbour(lastEntity);
		}

		IRoutingLocation last = finalTargets.get(lastEntity);
		if (last != null) {
			xyPositions.add(findRepresentativePoint(last));
		} else {
			// By default, route to centre of next entity
			xyPositions.add(new PositionXY(worldModel.getEntity(
					new EntityID(lastEntity)).getLocation(worldModel)));
		}

		return new Path(entities, xyPositions);
	}

	/**
	 * This method is called to get a solver (might be from cache).
	 * 
	 * @param from
	 *            starting point.
	 * @return
	 */
	protected abstract IRoutingAlgorithm obtainSolver(IRoutingLocation from);

	/**
	 * This one creates a *new* solver.
	 * 
	 * @param graph
	 * @param sources
	 * @param costs
	 * @return
	 */
	// protected abstract IRoutingAlgorithm createSolver(SimpleGraph graph,
	// List<Integer> sources, List<Double> costs);

	/*
	 * (non-Javadoc)
	 * 
	 * @see routing.IRoutingModule#getRoadCostFunction()
	 */
	@Override
	public IRoutingCostFunction getRoutingCostFunction() {
		return routingCostFunction;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.agent.ITimeStepListener#notifyTimeStepStarted(int)
	 */
	@Override
	public void notifyTimeStepStarted(int timeStep) {
		Set<Area> changed = new FastSet<Area>();
		for (EntityID id : changedEntities) {
			Entity e = worldModel.getEntity(id);
			if (e instanceof Area) {
				changed.add((Area) e);
			} else if (e instanceof Blockade) {
				if (((Blockade) e).isPositionDefined()) {
					// Get previous known position
					EntityID position = ((Blockade) e).getPosition();
					Area area = (Area) worldModel.getEntity(position);
					/*
					 * if (area == null) { System.out.println("Warning: " +
					 * ((StandardEntity) e).getFullDescription()); }
					 */
					changed.add(area);
				}
			}
		}

		boolean oneChanged = false;
		for (Area area : changed) {
			boolean thisChanged = converter.recomputeArea(area);
			if (thisChanged && !oneChanged) {
				oneChanged = true;
			}
		}

		if (oneChanged) {
			graphChanged();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * rescuecore2.worldmodel.EntityListener#propertyChanged(rescuecore2.worldmodel
	 * .Entity, rescuecore2.worldmodel.Property)
	 */
	public void propertyChanged(Entity e, Property p, Object oldValue,
			Object newValue) {
		// System.out.println(e);
		// changedEntities.add(e.getID());
		if (e instanceof Road) {
			if (p.getURN().equals(StandardPropertyURN.BLOCKADES.toString())) {
				boolean added = false;
				if (newValue instanceof List) {
					// If empty list, always add (bug? - undefined is initially
					// [])
					if (((List) newValue).size() == 0) {
						changedEntities.add(e.getID());
						added = true;
					}
				}
				if (!added
						&& !IAMWorldModel.checkIfPropertyValuesEqual(oldValue,
								newValue)) {
					changedEntities.add(e.getID());
				}
			}
		} else if (e instanceof Blockade) {
			if (p.getURN().equals(StandardPropertyURN.APEXES.toString())) {

				// System.out.println(p + ":" + oldValue + " -> " + newValue);
				if (!IAMWorldModel.checkIfPropertyValuesEqual(oldValue,
						newValue)) {
					// System.out.println("Different!");
					changedEntities.add(e.getID());
				}
			} else if (p.getURN().equals(RoutingInfoBlockade.BLOCK_INFO_URN)) {
				if (!IAMWorldModel.checkIfPropertyValuesEqual(oldValue,
						newValue)) {
					changedEntities.add(e.getID());
					// Also remember that we need to check communicated info
					Blockade b = (Blockade) e;
					// if (p.isDefined()) {
					// LOGGER.warn(b.getFullDescription() + ", " + p + ", "
					// + oldValue + ", " + newValue);
					// worldModel.getBlockCache().setUseCommunicatedInfo(b,
					// true);
					// }
				}
			}
		} else if (e instanceof Building) {
			if (p.getURN().equals(StandardPropertyURN.FIERYNESS.toString())) {
				// System.out.println(p + ":" + oldValue + " -> " + newValue);
				if (!IAMWorldModel.checkIfPropertyValuesEqual(oldValue,
						newValue)) {
					// System.out.println("Different!");
					changedEntities.add(e.getID());
				}
			}
		}

	}

	public void forceRecompute(Area area) {
		converter.recomputeArea(area);
		graphChanged();
	}

	protected abstract void graphChanged();

	@Override
	public SimpleGraph getRoutingGraph() {
		return graph;
	}

}
