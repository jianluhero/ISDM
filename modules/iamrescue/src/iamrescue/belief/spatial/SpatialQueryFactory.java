package iamrescue.belief.spatial;

import iamrescue.util.PositionXY;
import iamrescue.util.SpatialUtils;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

public class SpatialQueryFactory {

    private static final GeometryFactory factory = new GeometryFactory();

    private SpatialQueryFactory() {
    };

    /**
     * Queries all objects that are within the specified distance of the given
     * point (objects are returned as long as any part of them is within the
     * given distance).
     * 
     * @param centre
     *            The point from which to query.
     * @param distance
     *            The required distance.
     */
    public static SpatialQuery queryWithinDistance(PositionXY centre,
            int distance) {
        return new SpatialQuery(SpatialUtils.convertApexes(centre), distance,
                false, StandardEntity.class);
    }

    /**
     * Returns all objects that are intersected by the given rectangle.
     * 
     * @param rectangle
     *            Query rectangle
     */
    public static SpatialQuery queryWithinRectangle(Envelope rectangle) {
        Coordinate[] coordinates = new Coordinate[5];
        coordinates[0] = new Coordinate(rectangle.getMinX(), rectangle
                .getMinY());
        coordinates[1] = new Coordinate(rectangle.getMaxX(), rectangle
                .getMinY());
        coordinates[2] = new Coordinate(rectangle.getMaxX(), rectangle
                .getMaxY());
        coordinates[3] = new Coordinate(rectangle.getMinX(), rectangle
                .getMaxY());
        coordinates[4] = coordinates[0];

        Geometry queryGeometry = factory.createPolygon(factory
                .createLinearRing(coordinates), new LinearRing[0]);

        return new SpatialQuery(queryGeometry, 0, false, StandardEntity.class);
    }

    /**
     * Returns all objects that are intersected by the polygon given by the
     * parameter coordinates.
     * 
     * @param apexes
     *            Positions of the vertices of the query polygon.
     */
    public static SpatialQuery queryWithinPolygon(PositionXY[] polygon) {
        return new SpatialQuery(SpatialUtils.convertApexes(polygon), 0, false,
                StandardEntity.class);
    }

    /**
     * Returns all objects within the given distance of a specified object.
     * Here, the distance is the closest distance from any point of the object
     * to any point of another object.
     * 
     * @param object
     *            The query object.
     * @param distance
     *            The distance to the query object.
     */
    public static SpatialQuery queryWithinDistance(StandardEntity object,
            int distance, StandardWorldModel worldModel) {
        return new SpatialQuery(
                SpatialUtils.createGeometry(object, worldModel), distance,
                false, StandardEntity.class);
    }

    /**
     * Queries all objects of a certain type that are within the specified
     * distance of the given point (objects are returned as long as any part of
     * them is within the given distance).
     * 
     * @param centre
     *            The point from which to query.
     * @param distance
     *            The required distance.
     */
    public static SpatialQuery queryWithinDistance(PositionXY centre,
            int distance, Class<? extends StandardEntity> queryClass) {
        return new SpatialQuery(SpatialUtils.convertApexes(centre), distance,
                false, queryClass);
    }
}
