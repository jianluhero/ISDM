package iamrescue.routing;

import iamrescue.belief.IAMWorldModel;
import iamrescue.routing.costs.IRoutingCostFunction;
import iamrescue.routing.dijkstra.SimpleGraph;
import iamrescue.routing.dijkstra.SimpleGraph.Node;
import iamrescue.util.PositionXY;
import iamrescue.util.comparators.EntityIDComparator;

import java.util.ArrayList;
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
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.FireStation;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public class WorldModelConverter {
	private IAMWorldModel worldModel;

	private Map<Integer, List<Integer>> neighbourCache = new FastMap<Integer, List<Integer>>();

	private List<SimpleGraphNode> graphToWorldMap = new ArrayList<SimpleGraphNode>();
	private Map<Integer, WorldModelArea> worldToGraphMap = new FastMap<Integer, WorldModelArea>();

	// Keeps track of connected components (regardless of weights)
	private Map<Integer, Integer> routingComponents = new FastMap<Integer, Integer>();

	// PositionXYs -> simple node ids.
	private Map<PositionXY, Integer> pointsToGraphMap = new FastMap<PositionXY, Integer>();

	private SimpleGraph graph;
	private IRoutingCostFunction routingCostFunction;

	private static final Logger LOGGER = Logger
			.getLogger(WorldModelConverter.class);

	public WorldModelConverter(IAMWorldModel worldModel,
			IRoutingCostFunction roadCostFunction) {
		this.worldModel = worldModel;
		this.routingCostFunction = roadCostFunction;
		createGraph();
		checkConnectivity();
	}

	/**
	 * 
	 */
	private void checkConnectivity() {
		List<StandardEntity> areas = new FastList<StandardEntity>();
		areas.addAll(worldModel.getEntitiesOfType(StandardEntityURN.BUILDING,
				StandardEntityURN.ROAD, StandardEntityURN.FIRE_STATION,
				StandardEntityURN.AMBULANCE_CENTRE,
				StandardEntityURN.POLICE_OFFICE, StandardEntityURN.REFUGE));
		int counter = 1;
		while (areas.size() > 0) {
			List<StandardEntity> thisComponent = new FastList<StandardEntity>();
			thisComponent.add(areas.remove(0));
			while (thisComponent.size() > 0) {
				Area thisOne = (Area) thisComponent.remove(0);
				if (!routingComponents.containsKey(thisOne.getID().getValue())) {
					for (EntityID id : thisOne.getNeighbours()) {
						StandardEntity entity = worldModel.getEntity(id);
						if (entity != null) {
							thisComponent.add(entity);
							routingComponents.put(thisOne.getID().getValue(),
									counter);
						}
					}
				}
			}
			counter++;
		}
	}

	public boolean onSameComponent(int worldID1, int worldID2) {
		Integer id1 = routingComponents.get(worldID1);
		Integer id2 = routingComponents.get(worldID2);
		if (id1 == null || id2 == null) {
			return false;
		} else {
			return id1.equals(id2);
		}
	}

	public SimpleGraph getGraph() {
		return graph;
	}

	private void createGraph() {
		// Translate nodes to nodes
		List<StandardEntity> areas = new ArrayList<StandardEntity>();
		areas.addAll(worldModel.getEntitiesOfType(StandardEntityURN.BUILDING,
				StandardEntityURN.ROAD, StandardEntityURN.FIRE_STATION,
				StandardEntityURN.AMBULANCE_CENTRE,
				StandardEntityURN.POLICE_OFFICE, StandardEntityURN.REFUGE));

		// Sort by ID to make sure all agents get same node ids
		Collections.sort(areas, new EntityIDComparator());

		int idCounter = 0;

		for (StandardEntity se : areas) {
			Area area = (Area) se;
			int id = se.getID().getValue();

			// Do we know about this one?
			// If not, create it
			WorldModelArea wmArea = worldToGraphMap.get(id);
			if (wmArea == null) {
				wmArea = new WorldModelArea(id, new FastSet<Integer>());
				worldToGraphMap.put(id, wmArea);
				// System.out.println("Added " + id);
			}

			// All connections that we have already added
			Set<PositionXY> alreadyDone = new FastSet<PositionXY>();
			for (int simpleNeighbour : wmArea.simpleNeighbours) {
				SimpleGraphNode graphNode = graphToWorldMap
						.get(simpleNeighbour);
				alreadyDone.add(graphNode.getRepresentativePoint());
			}

			// Now look at neighbours
			List<Edge> edges = area.getEdges();

			for (Edge edge : edges) {
				if (edge.isPassable()) {
					Point2D midPoint = findMidpoint(edge.getLine());
					PositionXY intMidPoint = new PositionXY((int) Math
							.round(midPoint.getX()), (int) Math.round(midPoint
							.getY()));

					// Only consider this if we haven't added the connection
					if (!alreadyDone.contains(intMidPoint)) {
						// Create new nodes for this

						int neighbour = edge.getNeighbour().getValue();

						StandardEntity entity = worldModel
								.getEntity(new EntityID(neighbour));
						if (entity == null) {
							LOGGER.warn("Neighbour " + neighbour + " of " + id
									+ " does not exist! "
									+ area.getFullDescription());
							continue;
						}

						int simpleID = idCounter++;
						SimpleGraphNode simpleNeighbour = new SimpleGraphNode(
								simpleID, intMidPoint, id, neighbour);
						graphToWorldMap.add(simpleNeighbour);
						wmArea.simpleNeighbours.add(simpleID);

						WorldModelArea areaNeighbour = worldToGraphMap
								.get(neighbour);

						// Do we know about world neighbour?
						if (areaNeighbour == null) {
							areaNeighbour = new WorldModelArea(neighbour,
									new FastSet<Integer>());
							worldToGraphMap.put(neighbour, areaNeighbour);
						}
						areaNeighbour.simpleNeighbours.add(simpleID);
					}
				}
			}
		}
		// Done populating maps

		// Now add nodes to simple graph
		graph = new SimpleGraph();

		for (int i = 0; i < idCounter; i++) {
			graph.addNode();

			// Also add representative points to a map
			PositionXY representativePoint = graphToWorldMap.get(i)
					.getRepresentativePoint();
			if (pointsToGraphMap.containsKey(representativePoint)) {
				LOGGER.warn("Multiple routing vertices share "
						+ "same representative point. "
						+ "That is strange, but should not matter.");
			}
			pointsToGraphMap.put(representativePoint, i);
		}

		LOGGER
				.debug("Added " + idCounter
						+ " nodes to internal routing graph.");

		// Now consider each area and add edges
		Set<Entry<Integer, WorldModelArea>> areasToCompute = worldToGraphMap
				.entrySet();

		for (Entry<Integer, WorldModelArea> entry : areasToCompute) {
			recomputeArea((Area) worldModel.getEntity(new EntityID(entry
					.getKey())));
		}

	}

	public boolean recomputeArea(Area area) {

		boolean changed = false;

		WorldModelArea wmArea = worldToGraphMap.get(area.getID().getValue());
		Set<Integer> simpleNodes = wmArea.simpleNeighbours;
		int[] simpleNodeArray = new int[simpleNodes.size()];
		int counter = 0;
		for (int node : simpleNodes) {
			simpleNodeArray[counter++] = node;
		}

		// Calculate weights
		for (int i = 0; i < simpleNodeArray.length - 1; i++) {
			PositionXY originPoint = graphToWorldMap.get(simpleNodeArray[i])
					.getRepresentativePoint();
			for (int j = i + 1; j < simpleNodeArray.length; j++) {
				PositionXY destinationPoint = graphToWorldMap.get(
						simpleNodeArray[j]).getRepresentativePoint();

				// Compute cost
				double cost = routingCostFunction.getTravelCost(area,
						originPoint, destinationPoint);

				Node node1 = graph.getNodes().get(simpleNodeArray[i]);
				Node node2 = graph.getNodes().get(simpleNodeArray[j]);

				Double cost1to2 = node1.getCostToNeighbour(node2);
				Double cost2to1 = node2.getCostToNeighbour(node1);

				if (cost1to2 == null || cost != cost1to2) {
					node1.setCost(node2, cost);
					changed = true;
				}
				if (cost2to1 == null || cost != cost2to1) {
					node2.setCost(node1, cost);
					changed = true;
				}
			}
			// Store weights
		}

		return changed;
	}

	public Set<Integer> getSimpleGraphIDs(int worldModelID) {
		return worldToGraphMap.get(worldModelID).simpleNeighbours;
	}

	public Integer getSimpleGraphID(PositionXY representativePoint) {
		return pointsToGraphMap.get(representativePoint);
	}

	public Pair<Integer, Integer> getWorldModelIDs(int graphID) {
		int neighbour1 = graphToWorldMap.get(graphID).areaNeighbour1;
		int neighbour2 = graphToWorldMap.get(graphID).areaNeighbour2;
		return new Pair<Integer, Integer>(neighbour1, neighbour2);
	}

	public WorldModelArea getWorldModelArea(int worldID) {
		// System.out.println("Looking for " + worldID);
		return worldToGraphMap.get(worldID);
	}

	public SimpleGraphNode getSimpleGraphNode(int simpleID) {
		return graphToWorldMap.get(simpleID);
	}

	public static class SimpleGraphNode {
		private int id;
		private int areaNeighbour1;
		private int areaNeighbour2;
		private PositionXY representativePoint;

		// private Point2D representativePoint;

		public SimpleGraphNode(int id, PositionXY representativePoint,
				int areaNeighbour1, int areaNeighbour2) {
			this.id = id;
			this.areaNeighbour1 = areaNeighbour1;
			this.areaNeighbour2 = areaNeighbour2;
			this.representativePoint = representativePoint;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "SimpleGraphNode[id:" + id + ",n1:" + areaNeighbour1
					+ ",n2:" + areaNeighbour2 + ",p:" + representativePoint
					+ "]";
		}

		public int getOtherNeighbour(int neighbour) {
			if (areaNeighbour1 == neighbour) {
				return areaNeighbour2;
			} else {
				return areaNeighbour1;
			}
		}

		/**
		 * @return the representativePoint
		 */
		public PositionXY getRepresentativePoint() {
			return representativePoint;
		}

		/**
		 * @return the areaNeighbour1
		 */
		public int getAreaNeighbour1() {
			return areaNeighbour1;
		}

		/**
		 * @return the areaNeighbour2
		 */
		public int getAreaNeighbour2() {
			return areaNeighbour2;
		}
	}

	public static class WorldModelArea {
		private int id;
		private Set<Integer> simpleNeighbours;

		public WorldModelArea(int id, Set<Integer> simpleNeighbours) {
			this.id = id;
			this.simpleNeighbours = simpleNeighbours;
		}

		/**
		 * @return the simpleNeighbours
		 */
		public Set<Integer> getSimpleNeighbours() {
			return simpleNeighbours;
		}
	}

	private static Point2D findMidpoint(Line2D line) {
		return line.getPoint(0.5);
	}

	/**
	 * @param value
	 * @return
	 */
	public List<Integer> getSortedNeighbours(int worldID) {
		List<Integer> neighbours = neighbourCache.get(worldID);
		if (neighbours == null) {
			WorldModelArea worldModelArea = getWorldModelArea(worldID);
			neighbours = new ArrayList<Integer>(worldModelArea
					.getSimpleNeighbours());
			// Set<Integer> simpleNeighbours =
			// worldModelArea.getSimpleNeighbours();
			Collections.sort(neighbours);
			neighbourCache.put(worldID, neighbours);
		}
		return neighbours;
	}
}
