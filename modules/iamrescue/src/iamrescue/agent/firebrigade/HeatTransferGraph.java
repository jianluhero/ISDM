package iamrescue.agent.firebrigade;

import iamrescue.agent.firebrigade.util.RTree;
import iamrescue.belief.IAMWorldModel;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import com.infomatiq.jsi.IntProcedure;
import com.infomatiq.jsi.Rectangle;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class HeatTransferGraph implements Cloneable {

	private Log log = LogFactory.getLog(HeatTransferGraph.class);

	// Agents should have the same seed in order to make sure their beliefs are
	// as similar as possible
	private static final long SEED = 36728;
	private Random random = new Random(SEED);
	private static final double DEFAULT_RAYS_PER_DISTANCE_UNIT = 0.01;
	private double maxSampleDistance;

	private DirectedSparseGraph<Building, HeatTransferRelation> heatTransferGraph = new DirectedSparseGraph<Building, HeatTransferRelation>();
	private RTree rtree = new RTree();
	private IAMWorldModel worldModel;
	private double raysPerDistanceUnit;

	private boolean saveRays;
	private Collection<Line2D> rays = new FastList<Line2D>();

	public HeatTransferGraph(IAMWorldModel worldModel) {
		this(worldModel, false);
	}

	public HeatTransferGraph(IAMWorldModel iwm, boolean saveRays) {
		this(iwm, saveRays, DEFAULT_RAYS_PER_DISTANCE_UNIT);
	}

	/**
	 * 
	 * @param worldModel
	 * @param saveRays
	 *            if true, all emitted rays are saved (for debugging purposes
	 *            only)
	 */
	public HeatTransferGraph(IAMWorldModel worldModel, boolean saveRays,
			double raysPerDistanceUnit) {
		this.worldModel = worldModel;
		this.saveRays = saveRays;
		this.raysPerDistanceUnit = raysPerDistanceUnit;
		Properties props = new Properties();
		props.setProperty("MaxNodeEntries", "10");
		props.setProperty("MinNodeEntries", "5");
		rtree.init(props);

		maxSampleDistance = Math.max(worldModel.getBounds().getWidth(),
				worldModel.getBounds().getHeight()) * 1.1;

		Collection<StandardEntity> buildings = worldModel
				.getEntitiesOfType(StandardEntityURN.BUILDING);

		buildings.addAll(worldModel
				.getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE));
		buildings.addAll(worldModel
				.getEntitiesOfType(StandardEntityURN.FIRE_STATION));
		buildings.addAll(worldModel
				.getEntitiesOfType(StandardEntityURN.POLICE_OFFICE));
		buildings
				.addAll(worldModel.getEntitiesOfType(StandardEntityURN.REFUGE));

		for (StandardEntity standardEntity : buildings) {
			Building building = (Building) standardEntity;

			Rectangle boundingBox = getBoundingBox(building);
			rtree.add(boundingBox, building.getID().getValue());

			heatTransferGraph.addVertex(building);
		}

		long start = System.currentTimeMillis();
		log.debug("added buildings");

		for (StandardEntity standardEntity : buildings) {
			Building building = (Building) standardEntity;
			addEdges(building);
		}

		long finish = System.currentTimeMillis();

		System.out.println("Building Heat Transfer Graph Took "
				+ (finish - start));
	}

	private void addEdges(Building building) {
		log.debug("adding edges for " + building.getID());

		int totalRays = 0;

		// fire rays from each wall
		for (Edge edge : building.getEdges()) {
			int rays = (int) (getLength(edge) * raysPerDistanceUnit);
			totalRays += rays;

			// shoot a number of rays from this wall and check what's hit
			for (int j = 0; j < rays; j++) {
				Line2D ray = createRay(edge);

				// which building did we hit?
				Building neighbour;
				if (lineIntersectsBuilding(ray, building)) {
					// ray hit the building itself, this happens most of the
					// time, so we check this to achieve speedup
					neighbour = building;
				} else {
					neighbour = getFirstIntersectedBuilding(ray, building);
				}

				if (neighbour != null) {
					// create or update the edge in the graph
					HeatTransferRelation relation = heatTransferGraph.findEdge(
							building, neighbour);

					if (relation == null) {
						relation = new HeatTransferRelation(building, neighbour);
						heatTransferGraph
								.addEdge(relation, building, neighbour);
					}

					relation.incrementRaysHit();
				}
				// else: nothing was hit, continue
			}
		}

		Collection<HeatTransferRelation> outEdges = heatTransferGraph
				.getOutEdges(building);
		for (HeatTransferRelation heatTransferRelation : outEdges) {
			heatTransferRelation.normalise(totalRays);
		}

	}

	private Line2D createRay(Edge wall) {
		// pick random point on wall
		double rand = random.nextDouble();
		double startX = wall.getStartX() + rand
				* (wall.getEndX() - wall.getStartX());
		double startY = wall.getStartY() + rand
				* (wall.getEndY() - wall.getStartY());

		// shoot ray at random angle
		double angle = random.nextDouble() * 2 * Math.PI;
		double endX = startX + (Math.cos(angle) * maxSampleDistance);
		double endY = startY + (Math.sin(angle) * maxSampleDistance);

		// debugging only: shoots rays perpendicular to wall
		// Vector2D normal = edge.getLine().getDirection().getNormal();
		// endX = startX + (normal.getX() * maxSampleDistance);
		// endY = startY + (normal.getY() * maxSampleDistance);

		return new Line2D(new Point2D(startX, startY), new Point2D(endX, endY));
	}

	private boolean lineIntersectsBuilding(Line2D ray, Building building) {
		Double buildingIntersect = getBuildingIntersect(ray, building);

		if (saveRays && buildingIntersect != null) {
			rays.add(new Line2D(ray.getOrigin(), ray
					.getPoint(buildingIntersect)));
		}
		return buildingIntersect != null;
	}

	/**
	 * 
	 * @param ray
	 * @param building
	 *            the building emitting the ray. This building is not checked
	 *            against
	 * @return
	 */
	private Building getFirstIntersectedBuilding(Line2D ray, Building building) {
		// check what is hit
		ClosestBuildingIntProcedure closestBuildingIntProcedure = new ClosestBuildingIntProcedure(
				ray, building);
		boolean useNewMethod = true;

		if (useNewMethod) {
			double startX = ray.getOrigin().getX();
			double startY = ray.getOrigin().getY();

			double dX = ray.getDirection().getX()
					/ ray.getDirection().getLength();
			double dY = ray.getDirection().getY()
					/ ray.getDirection().getLength();

			double maxDistance = Math.min(100000, maxSampleDistance / 2);
			double distanceStep = 10000;
			double totalDistance = 0;
			boolean done = false;
			while (!done) {
				double newEndPointX = startX + distanceStep * dX;
				double newEndPointY = startY + distanceStep * dY;

				rtree.intersects((float) startX, (float) startY,
						(float) newEndPointX, (float) newEndPointY,
						closestBuildingIntProcedure);
				if (closestBuildingIntProcedure.getClosestBuildingIntersect() != null) {
					done = true;
				} else {
					startX = newEndPointX;
					startY = newEndPointY;
					if (startX > worldModel.getBounds().getMaxX()) {
						done = true;
					} else if (startX < worldModel.getBounds().getMinX()) {
						done = true;
					} else if (startY > worldModel.getBounds().getMaxY()) {
						done = true;
					} else if (startY < worldModel.getBounds().getMinY()) {
						done = true;
					} else {
						totalDistance += distanceStep;
						if (totalDistance > maxDistance) {
							done = true;
						}
					}
				}
			}
		} else {
			rtree.intersects((float) ray.getOrigin().getX(), (float) ray
					.getOrigin().getY(), (float) ray.getEndPoint().getX(),
					(float) ray.getEndPoint().getY(),
					closestBuildingIntProcedure);

		}

		if (saveRays
				&& closestBuildingIntProcedure.getClosestBuildingIntersect() != null) {
			rays.add(new Line2D(ray.getOrigin(),
					ray.getPoint(closestBuildingIntProcedure
							.getClosestIntersect())));
		}

		return closestBuildingIntProcedure.getClosestBuildingIntersect();
	}

	private double getLength(Edge edge) {
		int diffX = edge.getStartX() - edge.getEndX();
		int diffY = edge.getStartY() - edge.getEndY();

		return Math.sqrt(diffX * diffX + diffY * diffY);
	}

	private Rectangle getBoundingBox(Building building) {
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;

		int[] apexes = building.getApexList();

		for (int i = 0; i < apexes.length; i = i + 2) {
			minX = Math.min(apexes[i], minX);
			minY = Math.min(apexes[i + 1], minY);
			maxX = Math.max(apexes[i], maxX);
			maxY = Math.max(apexes[i + 1], maxY);
		}

		return new Rectangle(minX, minY, maxX, maxY);
	}

	public class ClosestBuildingIntProcedure implements IntProcedure {

		private Line2D ray;
		private Building closestBuildingIntersect;
		private double closestIntersect = Double.MAX_VALUE;
		private Building building;

		public ClosestBuildingIntProcedure(Line2D ray, Building building) {
			this.ray = ray;
			this.building = building;
		}

		@Override
		public boolean execute(int id) {
			if (id == building.getID().getValue()) {
				return true;
			}

			Building building = (Building) worldModel
					.getEntity(new EntityID(id));

			// the line hit the bounding box, test if it actually hit the
			// building as well
			Double x = getBuildingIntersect(ray, building);

			if (x != null && x > 0 && x < closestIntersect) {
				closestIntersect = x;
				closestBuildingIntersect = building;
			}

			return true;
		}

		public Building getClosestBuildingIntersect() {
			return closestBuildingIntersect;
		}

		public double getClosestIntersect() {
			return closestIntersect;
		}
	}

	public DirectedSparseGraph<Building, HeatTransferRelation> getGraph() {
		return heatTransferGraph;
	}

	public Collection<Building> getNeighbouringBuildings(Building building) {
		return heatTransferGraph.getNeighbors(building);
	}

	public Map<Building, Double> getHeatTransferCoefficients(Building building) {
		Map<Building, Double> result = new FastMap<Building, Double>();

		for (HeatTransferRelation edge : heatTransferGraph
				.getOutEdges(building)) {
			result.put(edge.getDestination(), edge.getHeatTransferRate());
		}

		return result;
	}

	public Collection<Building> getBuildings() {
		return heatTransferGraph.getVertices();
	}

	public Map<Building, Double> getHeatTransferRays(Building building) {
		Map<Building, Double> result = new FastMap<Building, Double>();

		for (HeatTransferRelation edge : heatTransferGraph
				.getOutEdges(building)) {
			result.put(edge.getDestination(), (double) edge.getRays());
		}

		return result;
	}

	/**
	 * Computes the fraction at which the ray intersects the building. Returns
	 * null if the ray does not intersect the building
	 * 
	 * @param ray
	 * @param building
	 * @return
	 */
	private Double getBuildingIntersect(Line2D ray, Building building) {
		double rayStartX = ray.getOrigin().getX();
		double rayStartY = ray.getOrigin().getY();
		double rayEndX = ray.getEndPoint().getX();
		double rayEndY = ray.getEndPoint().getY();

		double closestIntersect = Double.MAX_VALUE;

		// the line hit the bounding box, test if it hit the building?
		for (Edge edge : building.getEdges()) {
			// do the line *segments* intersect?
			if (!java.awt.geom.Line2D.linesIntersect(rayStartX, rayStartY,
					rayEndX, rayEndY, edge.getStartX(), edge.getStartY(), edge
							.getEndX(), edge.getEndY()))
				continue;

			// if so, where? (computes line intersection, not *segment*
			// intersection)
			double x = ray.getIntersection(edge.getLine());

			// getIntersection returns NaN in case of parallel lines
			if (!Double.isNaN(x)) {
				if (x > 0 && x < closestIntersect) {
					closestIntersect = x;
				}
			}
		}

		if (closestIntersect == Double.MAX_VALUE)
			return null;

		return closestIntersect;
	}

	public DirectedSparseGraph<Building, HeatTransferRelation> getHeatTransferGraph() {
		return heatTransferGraph;
	}

	public Collection<Line2D> getRays() {
		return rays;
	}

	protected Object clone() throws CloneNotSupportedException {
		HeatTransferGraph clone = (HeatTransferGraph) super.clone();

		// TODO make clone of the heatTransferGraph
		clone.heatTransferGraph = null;// (HeatTransferGraph)
		// heatTransferGraph.clone();
		return clone;
	}

}
