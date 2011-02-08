/**
 * File       : SpatialIndex.java     
 * Created on : 16 Apr 2008
 */
package iamrescue.belief.spatial;

import iamrescue.agent.SimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.util.SpatialUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javolution.util.FastMap;
import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.EntityListener;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.WorldModel;
import rescuecore2.worldmodel.WorldModelListener;

import com.infomatiq.jsi.IntProcedure;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.rtree.RTree;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author ss2
 * 
 */
public class SpatialIndex implements ISpatialIndex, EntityListener,
		WorldModelListener<StandardEntity> {

	/**
	 * Currently uses three R-trees for different types of objects
	 */
	private RTree movingTree;

	private RTree roadsAndNodesTree;

	private RTree blockadeTree;

	private RTree buildingTree;

	// private final static GeometryFactory factory = new GeometryFactory();

	private Map<EntityID, Envelope> insertedEnvelopes;

	private IAMWorldModel worldModel = null;

	private Map<EntityID, Geometry> geometries;

	private Map<EntityID, Integer> lastChanged = new FastMap<EntityID, Integer>();

	private static final Logger LOGGER = Logger.getLogger(SpatialIndex.class
			.getCanonicalName());

	public SpatialIndex(IAMWorldModel worldModel) {
		this.worldModel = worldModel;
		movingTree = new RTree();
		roadsAndNodesTree = new RTree();
		buildingTree = new RTree();
		blockadeTree = new RTree();

		Properties defaultProperties = new Properties();
		defaultProperties.setProperty("MaxNodeEntries", "10");
		defaultProperties.setProperty("MinNodeEntries", "2");

		movingTree.init(defaultProperties);
		roadsAndNodesTree.init(defaultProperties);
		buildingTree.init(defaultProperties);
		blockadeTree.init(defaultProperties);

		insertedEnvelopes = new FastMap<EntityID, Envelope>();
		geometries = new FastMap<EntityID, Geometry>();
		Collection<StandardEntity> allObjects = worldModel.getAllEntities();
		for (StandardEntity object : allObjects) {
			addObject(object);
			if (object instanceof Area) {
				geometries.put(object.getID(), SpatialUtils.createGeometry(
						object, worldModel));
			}
		}
		worldModel.addWorldModelListener(this);
	}

	private void addObject(StandardEntity object) {
		if (SpatialUtils.isSpatialObject(object)) {

			Envelope env = SpatialUtils.createBoundingEnvelope(object,
					worldModel);

			if (env != null) {
				selectQuadtree(object).add(convert(env),
						object.getID().getValue());
				assert !insertedEnvelopes.containsKey(object.getID());
				insertedEnvelopes.put(object.getID(), env);
			}

			// Also register, even if position is null, so that updates are
			// received.
			object.addEntityListener(this);

			/*
			 * Collection<RescueObjectProperty> properties = object
			 * .getSupportedProperties(); for (RescueObjectProperty prop :
			 * spatialProperties) { if (properties.contains(prop)) { try {
			 * 
			 * } catch (PropertyNotSupportedException e) {
			 * log.error("This exception should not have occurred", e); } } }
			 */
		}
	}

	private Rectangle convert(Envelope env) {
		return new Rectangle((float) env.getMinX(), (float) env.getMinY(),
				(float) env.getMaxX(), (float) env.getMaxY());
	}

	private void removeObject(StandardEntity object) {
		if (SpatialUtils.isSpatialObject(object)) {
			Envelope env = insertedEnvelopes.remove(object.getID());
			geometries.remove(object.getID());
			if (env != null) {
				selectQuadtree(object).delete(convert(env),
						object.getID().getValue());
			}
			// Also remove listener(s)!
			object.removeEntityListener(this);
			lastChanged.remove(object.getID());
		}
	}

	private RTree selectQuadtree(StandardEntity object) {
		if (object instanceof Road) {
			return roadsAndNodesTree;
		} else if (object instanceof Building) {
			return buildingTree;
		} else if (object instanceof Human) {
			return movingTree;
		} else if (object instanceof Blockade) {
			return blockadeTree;
		} else {
			LOGGER.error("Cannot find quadtree for object " + object);
			return null;
		}
	}

	public Collection<StandardEntity> query(SpatialQuery query) {
		long startTime = 0;
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Starting query " + query);
			startTime = System.nanoTime();
		}

		Geometry geometry = query.getGeometry();
		Geometry envelope = geometry.getEnvelope();
		int distance = query.getDistance();

		Envelope rectangle;
		if (envelope instanceof Polygon) {
			Polygon polyRectangle = (Polygon) envelope;
			rectangle = new Envelope(polyRectangle.getCoordinates()[0].x
					- distance, polyRectangle.getCoordinates()[1].x + distance,
					polyRectangle.getCoordinates()[1].y - distance,
					polyRectangle.getCoordinates()[2].y + distance);
		} else if (envelope instanceof Point) {
			Point pointRectangle = (Point) envelope;
			if (pointRectangle.isEmpty()) {
				throw new IllegalArgumentException("Invalid shape: " + geometry
						+ ". Must represent either a point or an area.");
			}
			rectangle = new Envelope(pointRectangle.getX() - distance,
					pointRectangle.getX() + distance, pointRectangle.getY()
							- distance, pointRectangle.getY() + distance);
		} else if (envelope instanceof LineString) {
			LineString line = (LineString) envelope;
			double minX = line.getStartPoint().getX();
			double maxX = line.getStartPoint().getX();
			double minY = line.getStartPoint().getY();
			double maxY = line.getStartPoint().getY();
			for (int i = 1; i < line.getNumPoints(); i++) {
				Point p = line.getPointN(i);
				if (p.getX() < minX) {
					minX = p.getX();
				}
				if (p.getX() > maxX) {
					maxX = p.getX();
				}
				if (p.getY() < minY) {
					minY = p.getY();
				}
				if (p.getY() > maxY) {
					maxY = p.getY();
				}
			}
			rectangle = new Envelope(minX - distance, maxX + distance, minY
					- distance, maxY + distance);
		} else {
			throw new IllegalArgumentException("Invalid shape: " + geometry
					+ ". Must represent either a point or an area.");
		}

		ArrayList<StandardEntity> list = new ArrayList<StandardEntity>();

		// Now query all relevant trees
		Class<? extends StandardEntity> queryClass = query.getQueryClass();

		SpatialObjectVisitor visitor = new SpatialObjectVisitor(query, list,
				geometries, worldModel);

		MyIntProcedure prc = new MyIntProcedure(visitor, worldModel);

		Rectangle r = convert(rectangle);

		if (queryClass.isAssignableFrom(Road.class)
				|| Road.class.isAssignableFrom(queryClass)) {
			roadsAndNodesTree.intersects(r, prc);
		}
		if (queryClass.isAssignableFrom(Human.class)
				|| Human.class.isAssignableFrom(queryClass)) {
			movingTree.intersects(r, prc);
		}
		if (queryClass.isAssignableFrom(Building.class)
				|| Building.class.isAssignableFrom(queryClass)) {
			buildingTree.intersects(r, prc);
		}
		if (queryClass.isAssignableFrom(Blockade.class)
				|| Blockade.class.isAssignableFrom(queryClass)) {
			blockadeTree.intersects(r, prc);
		}

		if (LOGGER.isTraceEnabled()) {
			long time = System.nanoTime() - startTime;
			LOGGER.trace("Completed query in " + time + "ns : " + list);
		}
		return list;
	}

	private static class MyIntProcedure implements IntProcedure {
		private SpatialObjectVisitor visitor;
		private IAMWorldModel worldModel;

		public MyIntProcedure(SpatialObjectVisitor visitor,
				IAMWorldModel worldModel) {
			this.visitor = visitor;
			this.worldModel = worldModel;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.infomatiq.jsi.IntProcedure#execute(int)
		 */
		public boolean execute(int id) {
			visitor.visitItem(worldModel.getEntity(new EntityID(id)));
			return true;
		}
	}

	public void propertyChanged(Entity e, Property p, Object oldValue,
			Object newValue) {
		if (p.getURN().equals(StandardPropertyURN.POSITION.toString())
				|| p.getURN().equals(StandardPropertyURN.APEXES.toString())
				|| p.getURN().equals(StandardPropertyURN.EDGES.toString())
				|| p.getURN().equals(StandardPropertyURN.X.toString())
				|| p.getURN().equals(StandardPropertyURN.Y.toString())) {
			StandardEntity se = (StandardEntity) e;
			removeObject(se);
			addObject(se);

		}

	}

	public void entityAdded(WorldModel<? extends StandardEntity> model,
			StandardEntity e) {
		addObject(e);
	}

	public void entityRemoved(WorldModel<? extends StandardEntity> model,
			StandardEntity e) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.debug("Removing " + e);
			LOGGER.debug(blockadeTree.size() + " blockades before.");
		}
		removeObject(e);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.debug(blockadeTree.size() + " blockades after.");
		}
	}
}