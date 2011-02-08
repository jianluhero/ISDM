/**
 * File       : SpatialIndex.java     
 * Created on : 16 Apr 2008
 */
package iamrescue.belief.spatial;

import iamrescue.belief.IAMWorldModel;
import iamrescue.util.SpatialUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rescuecore2.standard.entities.Area;
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

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

/**
 * @author ss2
 * 
 */
public class SlowSpatialQuadtreeIndex implements ISpatialIndex, EntityListener,
		WorldModelListener<StandardEntity> {

	/**
	 * Currently uses three quad trees for different types of objects
	 */
	private Quadtree movingTree;

	private Quadtree roadsAndNodesTree;

	private Quadtree buildingTree;

	// private final static GeometryFactory factory = new GeometryFactory();

	private Map<StandardEntity, Envelope> insertedEnvelopes;

	private static Log log = LogFactory.getLog(SlowSpatialQuadtreeIndex.class);

	private IAMWorldModel worldModel = null;

	private Map<EntityID, Geometry> geometries;

	public SlowSpatialQuadtreeIndex(IAMWorldModel worldModel) {
		this.worldModel = worldModel;
		movingTree = new Quadtree();
		roadsAndNodesTree = new Quadtree();
		buildingTree = new Quadtree();
		insertedEnvelopes = new FastMap<StandardEntity, Envelope>();
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
				selectQuadtree(object).insert(env, object);
				assert !insertedEnvelopes.containsKey(object);
				insertedEnvelopes.put(object, env);
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

	private void removeObject(StandardEntity object) {
		if (SpatialUtils.isSpatialObject(object)) {
			Envelope env = insertedEnvelopes.remove(object);
			geometries.remove(object.getID());
			if (env != null) {
				selectQuadtree(object).remove(env, object);
			}
			// Also remove listener(s)!
			object.removeEntityListener(this);
		}
	}

	private Quadtree selectQuadtree(StandardEntity object) {
		if (object instanceof Road) {
			return roadsAndNodesTree;
		} else if (object instanceof Building) {
			return buildingTree;
		} else if (object instanceof Human) {
			return movingTree;
		} else {
			log.error("Cannot find quadtree for object " + object);
			return null;
		}
	}

	public Collection<StandardEntity> query(SpatialQuery query) {
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

		if (queryClass.isAssignableFrom(Road.class)

		|| Road.class.isAssignableFrom(queryClass)) {
			roadsAndNodesTree.query(rectangle, visitor);
		}
		if (queryClass.isAssignableFrom(Human.class)
				|| Human.class.isAssignableFrom(queryClass)) {
			movingTree.query(rectangle, visitor);
		}
		if (queryClass.isAssignableFrom(Building.class)
				|| Building.class.isAssignableFrom(queryClass)) {
			buildingTree.query(rectangle, visitor);
		}
		return list;
	}

	public void propertyChanged(Entity e, Property p, Object oldValue,
			Object newValue) {
		if (p.getURN().equals(StandardPropertyURN.POSITION.toString())
				|| p.getURN().equals(StandardPropertyURN.X.toString())
				|| p.getURN().equals(StandardPropertyURN.Y.toString())) {
			removeObject((StandardEntity) e);
			addObject((StandardEntity) e);
		}
	}

	public void entityAdded(WorldModel<? extends StandardEntity> model,
			StandardEntity e) {
		addObject(e);
	}

	public void entityRemoved(WorldModel<? extends StandardEntity> model,
			StandardEntity e) {
		removeObject(e);
	}

}
