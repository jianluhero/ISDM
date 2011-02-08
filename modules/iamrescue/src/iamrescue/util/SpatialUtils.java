package iamrescue.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.WorldModel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

public class SpatialUtils {

	private final static GeometryFactory factory = new GeometryFactory();

	private static final Logger LOGGER = Logger.getLogger(SpatialUtils.class);

	/**
	 * Converts a list of coordinates into a Geometry object. If a single
	 * coordinate is given, the shape is converted to a Point, otherwise to a
	 * Polygon.
	 * 
	 * @param apexes
	 *            Coordinates.
	 * @return A geometry object representing the shape given by the
	 *         coordinates.
	 */
	public static Geometry convertApexes(PositionXY[] apexes) {
		List<Pair<Integer, Integer>> list = new ArrayList<Pair<Integer, Integer>>(
				apexes.length);
		for (int i = 0; i < apexes.length; i++) {
			list.add(new Pair<Integer, Integer>(apexes[i].getX(), apexes[i]
					.getY()));
		}
		return convertApexes(list);
	}

	public static Geometry convertApexes(int[] apexes) {
		List<Pair<Integer, Integer>> list = new ArrayList<Pair<Integer, Integer>>(
				apexes.length / 2);
		for (int i = 0; i < apexes.length; i = i + 2) {
			list.add(new Pair<Integer, Integer>(apexes[i], apexes[i + 1]));
		}
		return convertApexes(list);
	}

	public static Geometry convertApexes(Pair<Integer, Integer> point) {
		return convertApexes(Collections.singletonList(point));
	}

	/**
	 * Converts a list of coordinates into a Geometry object. If a single
	 * coordinate is given, the shape is converted to a Point, otherwise to a
	 * Polygon.
	 * 
	 * @param apexes
	 *            Coordinates.
	 * @return A geometry object representing the shape given by the
	 *         coordinates.
	 */
	public static Geometry convertApexes(List<Pair<Integer, Integer>> apexes) {
		if (apexes.size() == 0) {
			throw new IllegalArgumentException(
					"Cannot handle empty apex array.");
		}

		if (apexes.size() == 1) {
			return factory.createPoint(new Coordinate(apexes.get(0).first(),
					apexes.get(0).second()));
		} else if (apexes.size() == 2) {
			Coordinate[] coordinates = new Coordinate[2];
			int i = 0;
			for (Pair<Integer, Integer> pair : apexes) {
				coordinates[i] = new Coordinate(pair.first(), pair.second());
				i++;
			}
			return factory.createLineString(coordinates);
		} else {
			Coordinate[] coordinates = new Coordinate[apexes.size() + 1];
			int i = 0;
			for (Pair<Integer, Integer> pair : apexes) {
				coordinates[i] = new Coordinate(pair.first(), pair.second());
				i++;
			}
			coordinates[apexes.size()] = coordinates[0];
			return factory.createPolygon(factory.createLinearRing(coordinates),
					new LinearRing[0]);
		}
	}

	public static Geometry convertApexes(PositionXY apex) {
		return factory.createPoint(new Coordinate(apex.getX(), apex.getY()));
	}

	public static boolean isSpatialObject(StandardEntity se) {
		return (se instanceof Building || se instanceof Road
				|| se instanceof Human || se instanceof Blockade);
	}

	public static Geometry createGeometry(StandardEntity se,
			StandardWorldModel worldModel) {
		if (!isSpatialObject(se)) {
			LOGGER.error("Not a spatial object: " + se.getFullDescription());
			return null;
		}
		List<Pair<Integer, Integer>> coordPairs = new LinkedList<Pair<Integer, Integer>>();
		if (se instanceof Area) {
			Area a = (Area) se;
			if (a.isEdgesDefined()) {
				coordPairs = convertEdgesToApexes(a.getEdges());
			}
		} else if (se instanceof Blockade) {
			Blockade b = (Blockade) se;
			if (b.isApexesDefined()) {
				return convertApexes(b.getApexes());
			}
		} else {
			Human h = (Human) se;
			if (h.isPositionDefined()) {
				coordPairs.add(h.getLocation(worldModel));
			}
		}

		if (coordPairs.size() == 0) {
			LOGGER.error("No coordinates: " + se.getFullDescription());
			return null;
		} else {
			return convertApexes(coordPairs);
		}
	}

	/**
	 * @param edges
	 * @return
	 */
	private static List<Pair<Integer, Integer>> convertEdgesToApexes(
			List<Edge> edges) {
		List<Pair<Integer, Integer>> edgesList = new ArrayList<Pair<Integer, Integer>>();
		for (Edge edge : edges) {
			edgesList.add(new Pair<Integer, Integer>(edge.getStartX(), edge
					.getStartY()));
		}
		// Don't need last x/y

		return edgesList;

	}

	public static Envelope createBoundingEnvelope(StandardEntity se,
			StandardWorldModel worldModel) {
		List<Pair<Integer, Integer>> coordPairs = new LinkedList<Pair<Integer, Integer>>();
		if (se instanceof Area) {
			Area a = (Area) se;
			if (a.isEdgesDefined()) {
				coordPairs = convertEdgesToApexes(a.getEdges());
			}
		} else if (se instanceof Blockade) {
			Blockade b = (Blockade) se;
			if (b.isApexesDefined()) {
				int[] apexes = b.getApexes();
				for (int i = 0; i < apexes.length; i = i + 2) {
					coordPairs.add(new Pair<Integer, Integer>(apexes[i],
							apexes[i + 1]));
				}
			}
		} else {
			Human h = (Human) se;
			if (h.isXDefined() && h.isYDefined()) {
				coordPairs.add(h.getLocation(worldModel));
			}
		}

		if (coordPairs.size() == 0) {
			return null;
		}

		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minY = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (Pair<Integer, Integer> pair : coordPairs) {
			if (pair == null) {
				return null;
			}
			if (pair.first() < minX) {
				minX = pair.first();
			}
			if (pair.first() > maxX) {
				maxX = pair.first();
			}
			if (pair.second() < minY) {
				minY = pair.second();
			}
			if (pair.second() > maxY) {
				maxY = pair.second();
			}
		}
		return new Envelope(minX, maxX, minY, maxY);
	}

	/*
	 * public static int computeSearchRadius(AgentConfiguration
	 * agentConfiguration) { int maxDistancePerTurn = agentConfiguration
	 * .getInt(AgentConfigurationConstants.MAX_TRAVELLED_DISTANCE_PER_TURN);
	 * double searchRadiusScaler = agentConfiguration
	 * .getDouble(AgentConfigurationConstants.SEARCH_RADIUS_SCALER); return
	 * (int) (maxDistancePerTurn * searchRadiusScaler); }
	 */

	public static StandardEntity getFixedPosition(StandardEntity object,
			WorldModel<StandardEntity> worldModel) {
		if (object == null)
			return null;

		StandardEntity pos = object;
		while (pos != null && (pos instanceof Human)) {
			pos = worldModel.getEntity(((Human) pos).getPosition());
		}
		return pos;
	}
}
