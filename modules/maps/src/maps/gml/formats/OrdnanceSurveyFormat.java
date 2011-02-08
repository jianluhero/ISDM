package maps.gml.formats;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javolution.util.FastMap;
import maps.gml.GMLBuilding;
import maps.gml.GMLCoordinates;
import maps.gml.GMLDirectedEdge;
import maps.gml.GMLMap;
import maps.gml.GMLNode;
import maps.gml.GMLRoad;

import maps.gml.GMLShape;
import maps.util.Polygon;
import maps.util.PolygonSplitter;
import maps.gml.GMLMapFormat;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.XPath;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import rescuecore2.log.Logger;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

// TO DO: Handle inner boundaries

/**
 * A MapFormat that can handle maps from the UK Ordnance Survey.
 */

public final class OrdnanceSurveyFormat extends GMLMapFormat {
	/** Singleton instance. */
	public static final OrdnanceSurveyFormat INSTANCE = new OrdnanceSurveyFormat();

	private static final GeometryFactory factory = new GeometryFactory();

	private static final String FEATURE_CODE_BUILDING = "10021";
	private static final String FEATURE_CODE_ROAD = "10172";
	private static final String FEATURE_CODE_FOOTPATH = "10183";

	private static final String FEATURE_CODE_OPEN_SPACE = "10053";
	private static final String FEATURE_CODE_GENERAL_SPACE = "10056";

	private static final String OSGB_NAMESPACE_URI = "http://www.ordnancesurvey.co.uk/xml/namespaces/osgb";

	private static final Namespace OSGB_NAMESPACE = DocumentHelper
			.createNamespace("osgb", OSGB_NAMESPACE_URI);

	private static final QName FEATURE_COLLECTION_QNAME = DocumentHelper
			.createQName("FeatureCollection", OSGB_NAMESPACE);
	private static final QName TOPOGRAPHIC_AREA_QNAME = DocumentHelper
			.createQName("TopographicArea", OSGB_NAMESPACE);

	private static final XPath BUILDING_XPATH = DocumentHelper
			.createXPath("//osgb:topographicMember/osgb:TopographicArea[osgb:featureCode[text()='"
					+ FEATURE_CODE_BUILDING + "']]");
	private static final XPath ROAD_XPATH = DocumentHelper
			.createXPath("//osgb:topographicMember/osgb:TopographicArea[osgb:featureCode[text()='"
					+ FEATURE_CODE_ROAD + "']]");
	private static final XPath SPACE_XPATH = DocumentHelper
			.createXPath("//osgb:topographicMember/osgb:TopographicArea[osgb:featureCode[text()='"
					+ FEATURE_CODE_OPEN_SPACE
					+ "' or text()='"
					+ FEATURE_CODE_GENERAL_SPACE + "']]");
	private static final XPath SHAPE_XPATH = DocumentHelper
			.createXPath("osgb:polygon/gml:Polygon/gml:outerBoundaryIs/gml:LinearRing/gml:coordinates");
	private static final XPath INNER_RING_XPATH = DocumentHelper
			.createXPath("osgb:polygon/gml:Polygon/gml:innerBoundaryIs/gml:LinearRing/gml:coordinates");

	// Map from uri prefix to uri for XPath expressions
	private static final Map<String, String> URIS = new HashMap<String, String>();

	private static final Color NEW_BUILDING_COLOUR = new Color(255, 0, 0, 128);
	private static final Color NEW_ROAD_COLOUR = new Color(255, 0, 0, 128);
	private static final Color BUILDING_COLOUR = new Color(0, 128, 0, 128);
	private static final Color ROAD_COLOUR = new Color(128, 128, 128, 128);

	private static final int FID_PREFIX_LENGTH = 4;

	// private ShapeDebugFrame debug;
	// private List<GMLShapeInfo> background;
	private int maxRoadID;

	private double entrancewidth = 1.0;

	static {
		URIS.put("gml", Common.GML_NAMESPACE_URI);
		URIS.put("xlink", Common.XLINK_NAMESPACE_URI);
		URIS.put("osgb", OSGB_NAMESPACE_URI);

		BUILDING_XPATH.setNamespaceURIs(URIS);
		ROAD_XPATH.setNamespaceURIs(URIS);
		SPACE_XPATH.setNamespaceURIs(URIS);
		SHAPE_XPATH.setNamespaceURIs(URIS);
		INNER_RING_XPATH.setNamespaceURIs(URIS);
	}

	private OrdnanceSurveyFormat() {
	}

	@Override
	public String toString() {
		return "Ordnance survey";
	}

	@Override
	public Map<String, String> getNamespaces() {
		return Collections.unmodifiableMap(URIS);
	}

	@Override
	public boolean isCorrectRootElement(String uri, String localName) {
               return OSGB_NAMESPACE_URI.equals(uri) && "FeatureCollection".equals(localName);
        }

	@Override
	public GMLMap read(Document doc) {
		GMLMap result = new GMLMap();
		readBuildings(doc, result);
		readRoads(doc, result);
		readSpaces(doc, result);

		/*
		 * make path between buildings and roads
		 */
		makeEntrances(result);

		/*
		 * cut up roads into roughly convex sections
		 */
		cutRoads(result);

		/*
		 * make road network connected after roads have been parsed
		 */
		// connectRoadNetwork(result);
		connectNetwork(result);

		/*
		 * Output to a map file
		 */

		Document mydoc = RobocupFormat.INSTANCE.write(result);
		XMLWriter writer;
		try {
			String curDir = System.getProperty("user.dir");

			String dirStr = curDir + "/../maps/gml/output/";

			File outputdir = new File(dirStr);

			if (!outputdir.exists()) {
				outputdir.mkdir();
			}

			File outfile = new File(dirStr + "map.gml");

			if (!outfile.exists()) {
				outfile.createNewFile();
			}

			writer = new XMLWriter(new FileOutputStream(outfile), OutputFormat
					.createPrettyPrint());

			writer.write(mydoc);
			writer.flush();
			writer.close();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	private void cutRoads(GMLMap result) {
		// TODO Auto-generated method stub
		/*
		 * create new road segments
		 */
		Set<GMLRoad> roads = result.getRoads();
		List<List<GMLDirectedEdge>> newRoads = new ArrayList<List<GMLDirectedEdge>>();
		for (GMLRoad road : roads) {
			double[] xCoords = new double[road.getEdges().size()];
			double[] yCoords = new double[road.getEdges().size()];
			List<GMLDirectedEdge> edges = road.getEdges();
			Map<Pair<Double, Double>, GMLNode> map = new FastMap<Pair<Double, Double>, GMLNode>();
			int i = 0;
			for (GMLDirectedEdge edge : edges) {
				xCoords[i] = edge.getStartCoordinates().getX();
				yCoords[i] = edge.getStartCoordinates().getY();
				map.put(new Pair<Double, Double>(xCoords[i], yCoords[i]), edge
						.getStartNode());
				i++;
			}
			Polygon polygon = new Polygon(xCoords, yCoords);
			Polygon[] splitPolygon = PolygonSplitter.splitPolygon(polygon, 25,
					50);
			for (i = 0; i < splitPolygon.length; i++) {
				List<GMLDirectedEdge> e = new ArrayList<GMLDirectedEdge>();
				for (int j = 0; j < splitPolygon[i].x.length; j++) {
					double x = splitPolygon[i].x[j];
					double y = splitPolygon[i].y[j];
					double nextX = splitPolygon[i].x[(j + 1)
							% splitPolygon[i].x.length];
					double nextY = splitPolygon[i].y[(j + 1)
							% splitPolygon[i].x.length];

					GMLDirectedEdge edge = new GMLDirectedEdge(result
							.createEdge(
									map.get(new Pair<Double, Double>(x, y)),
									map.get(new Pair<Double, Double>(nextX,
											nextY))), true);
					e.add(edge);
				}
				newRoads.add(e);
			}

		}
		// remove old roads
		result.removeAllRoads();

		// add new roads
		for (List<GMLDirectedEdge> road : newRoads) {
			result.createRoad(road);
			maxRoadID++;
		}

	}

	private void makeEntrances(GMLMap result) {
		/*
		 * draw an entrance to the nearest road; make it 2m wide?
		 */
		Set<GMLBuilding> buildings = result.getBuildings();
		Iterator<GMLBuilding> bit = buildings.iterator();
		int i = 0;
		while (bit.hasNext()) {// && i<4) {
			GMLBuilding building = (GMLBuilding) bit.next();
			/*
			 * for each building find the nearest road centre or point on edge?
			 * 
			 * this is gonna be slow.........
			 */
			GMLRoad road = closestRoad(result, building, maxRoadID);

			GMLDirectedEdge[] edges = closestEdges(building, road, result);
			if (!shareEdge(edges)) {
				// System.out.println("building an edge");
				// System.out.println(edges[0].getStartCoordinates() +
				// " "+edges[0].getEndCoordinates() +
				// " "+edges[1].getStartCoordinates() +
				// " "+edges[1].getEndCoordinates() + " ");
				// System.out.println(building.getCoordinates());
				// System.out.println(edges[0]);
				double midx = (edges[0].getEndCoordinates().getX() + edges[0]
						.getStartCoordinates().getX()) / 2.0;
				double midy = (edges[0].getEndCoordinates().getY() + edges[0]
						.getStartCoordinates().getY()) / 2.0;
				// System.out.println(midx + ", " + midy);
				Point2D bPoint = new Point2D(midx, midy);
				Line2D bline = new Line2D(makePoint(edges[0]
						.getStartCoordinates()), makePoint(edges[0]
						.getEndCoordinates()));
				Line2D rline = new Line2D(makePoint(edges[1]
						.getStartCoordinates()), makePoint(edges[1]
						.getEndCoordinates()));
				// System.out.println(bline.toString());
				Coordinate[] connection = new Coordinate[5];
				Point2D rPoint = GeometryTools2D.getClosestPointOnSegment(
						rline, bPoint);
				double lengthB = GeometryTools2D.getDistance(makePoint(edges[0]
						.getStartCoordinates()), makePoint(edges[0]
						.getEndCoordinates()));
				// System.out.println(lengthB);
				// if(entrancewidth/lengthB<0.5){
				// will fit on this edge of the building
				connection[0] = makeCoordinate(bline
						.getPoint(0.5 - (entrancewidth / lengthB)));
				connection[1] = makeCoordinate(bline
						.getPoint(0.5 + (entrancewidth / lengthB)));
				/*
				 * } else { //wont so make it the limits of the edge
				 * connection[0] = makeCoordinate(bline .getPoint(0.0));
				 * connection[1] = makeCoordinate(bline .getPoint(1.0)); }
				 */

				double distanceOnRoad = GeometryTools2D.getDistance(
						makePoint(edges[1].getStartCoordinates()), rPoint);
				double lengthRoad = GeometryTools2D.getDistance(
						makePoint(edges[1].getStartCoordinates()),
						makePoint(edges[1].getEndCoordinates()));
				if (distanceOnRoad / lengthRoad >= entrancewidth / lengthRoad) {
					connection[2] = makeCoordinate(rline
							.getPoint((distanceOnRoad / lengthRoad)
									- (entrancewidth / lengthRoad)));
					connection[3] = makeCoordinate(rline
							.getPoint((distanceOnRoad / lengthRoad)
									+ (entrancewidth / lengthRoad)));
				} else {
					// wont fit here so move rPoint by 0.3 towards end
					connection[2] = makeCoordinate(rline
							.getPoint(distanceOnRoad / lengthRoad));
					connection[3] = makeCoordinate(rline
							.getPoint((distanceOnRoad / lengthRoad)
									+ (entrancewidth * 2 / lengthRoad)));
				}

				// check the path doesnt twist
				Line2D line1 = new Line2D(makePoint(connection[0]),
						makePoint(connection[3]));
				Line2D line2 = new Line2D(makePoint(connection[1]),
						makePoint(connection[2]));
				double int1 = line1.getIntersection(line2);
				double int2 = line2.getIntersection(line1);
				if (int1 < 1.0 && int1 > 0.0 && int2 < 1.0 && int2 > 0.0) {
					// System.out.println("twisty" +
					// line1.getIntersection(line2));
					connection[4] = connection[2];
					connection[2] = connection[3];
					connection[3] = connection[4];
				}

				connection[4] = connection[0];

				Coordinate[] c = new Coordinate[4];
				c[0] = connection[0];// new
				// Coordinate(bline.getOrigin().getX(),bline.getOrigin().getY());//connection[0];
				c[1] = connection[1];// new
				// Coordinate(bline.getEndPoint().getX(),bline.getEndPoint().getY());//connection[1];
				c[2] = new Coordinate(rPoint.getX(), rPoint.getY());
				c[3] = c[0];
				List<GMLDirectedEdge> e = readEdges(connection, result);

				if (noClash(connection, road, building, result)) {
					// System.out.println("no clash");
					modifyRoad(road, connection[2], connection[3], edges[1],
							result);
					modifyBuilding(building, connection[0], connection[1],
							edges[0], result);
					GMLRoad newRoad = result.createRoad(e);
					// background.add(new GMLShapeInfo(newRoad, "Roads",
					// Color.BLACK, ROAD_COLOUR));
				} else {
					bit.remove();
				}
				i++;
			}
		}
		/*
		 * while(bit.hasNext()){ GMLBuilding building = (GMLBuilding)
		 * bit.next(); result.removeBuilding(building); }
		 */

	}

	private void modifyBuilding(GMLBuilding building, Coordinate c1,
			Coordinate c2, GMLDirectedEdge edge, GMLMap map) {
		/*
		 * add these two points into the road edge so it can be connected
		 * appropiatly
		 */
		List<GMLDirectedEdge> edges = building.getEdges();
		Coordinate[] c = new Coordinate[edges.size() + 3];
		if (GeometryTools2D.getDistance(makePoint(edge.getStartCoordinates()),
				makePoint(c1)) < GeometryTools2D.getDistance(makePoint(edge
				.getStartCoordinates()), makePoint(c2))) {
			// start c1 c2 end
			int i = 0;
			Iterator<GMLDirectedEdge> eit = edges.iterator();
			while (eit.hasNext()) {
				GMLDirectedEdge e = (GMLDirectedEdge) eit.next();
				if (e.getStartCoordinates() != edge.getStartCoordinates()) {
					c[i] = new Coordinate(e.getStartCoordinates().getX(), e
							.getStartCoordinates().getY());
					c[i + 1] = new Coordinate(e.getEndCoordinates().getX(), e
							.getEndCoordinates().getY());
					i++;
				} else {
					c[i] = new Coordinate(e.getStartCoordinates().getX(), e
							.getStartCoordinates().getY());
					c[i + 1] = c1;
					c[i + 2] = c2;
					c[i + 3] = new Coordinate(e.getEndCoordinates().getX(), e
							.getEndCoordinates().getY());
					i++;
					i++;
					i++;
				}
			}
		} else {
			// start c2 c1 end
			int i = 0;
			Iterator<GMLDirectedEdge> eit = edges.iterator();
			while (eit.hasNext()) {
				GMLDirectedEdge e = (GMLDirectedEdge) eit.next();
				if (e.getStartCoordinates() != edge.getStartCoordinates()) {
					c[i] = new Coordinate(e.getStartCoordinates().getX(), e
							.getStartCoordinates().getY());
					c[i + 1] = new Coordinate(e.getEndCoordinates().getX(), e
							.getEndCoordinates().getY());
					i++;
				} else {
					c[i] = new Coordinate(e.getStartCoordinates().getX(), e
							.getStartCoordinates().getY());
					c[i + 1] = c2;
					c[i + 2] = c1;
					c[i + 3] = new Coordinate(e.getEndCoordinates().getX(), e
							.getEndCoordinates().getY());
					i++;
					i++;
					i++;
				}
			}
		}
		map.removeBuilding(building);
		List<GMLDirectedEdge> e = readEdges(c, map);
		GMLBuilding newBuilding = new GMLBuilding(building.getID(), e);
		map.addBuilding(newBuilding);
		// GMLBuilding newBuilding = map.createBuilding(e);
		// background.add(new GMLShapeInfo(newBuilding, "Roads", Color.BLACK,
		// ROAD_COLOUR));
	}

	private void modifyRoad(GMLRoad road, Coordinate c1, Coordinate c2,
			GMLDirectedEdge edge, GMLMap map) {
		/*
		 * add these two points into the road edge so it can be connected
		 * appropiatly
		 */
		List<GMLDirectedEdge> edges = road.getEdges();
		Coordinate[] c = new Coordinate[edges.size() + 3];
		if (GeometryTools2D.getDistance(makePoint(edge.getStartCoordinates()),
				makePoint(c1)) < GeometryTools2D.getDistance(makePoint(edge
				.getStartCoordinates()), makePoint(c2))) {
			// start c1 c2 end
			int i = 0;
			Iterator<GMLDirectedEdge> eit = edges.iterator();
			while (eit.hasNext()) {
				GMLDirectedEdge e = (GMLDirectedEdge) eit.next();
				if (e.getStartCoordinates() != edge.getStartCoordinates()) {
					c[i] = new Coordinate(e.getStartCoordinates().getX(), e
							.getStartCoordinates().getY());
					c[i + 1] = new Coordinate(e.getEndCoordinates().getX(), e
							.getEndCoordinates().getY());
					i++;
				} else {
					c[i] = new Coordinate(e.getStartCoordinates().getX(), e
							.getStartCoordinates().getY());
					c[i + 1] = c1;
					c[i + 2] = c2;
					c[i + 3] = new Coordinate(e.getEndCoordinates().getX(), e
							.getEndCoordinates().getY());
					i++;
					i++;
					i++;
				}
			}
		} else {
			// start c2 c1 end
			int i = 0;
			Iterator<GMLDirectedEdge> eit = edges.iterator();
			while (eit.hasNext()) {
				GMLDirectedEdge e = (GMLDirectedEdge) eit.next();
				if (e.getStartCoordinates() != edge.getStartCoordinates()) {
					c[i] = new Coordinate(e.getStartCoordinates().getX(), e
							.getStartCoordinates().getY());
					c[i + 1] = new Coordinate(e.getEndCoordinates().getX(), e
							.getEndCoordinates().getY());
					i++;
				} else {
					c[i] = new Coordinate(e.getStartCoordinates().getX(), e
							.getStartCoordinates().getY());
					c[i + 1] = c2;
					c[i + 2] = c1;
					c[i + 3] = new Coordinate(e.getEndCoordinates().getX(), e
							.getEndCoordinates().getY());
					i++;
					i++;
					i++;
				}
			}
		}
		map.removeRoad(road);
		List<GMLDirectedEdge> e = readEdges(c, map);
		// GMLRoad newRoad = map.createRoad(e);
		GMLRoad newRoad = new GMLRoad(road.getID(), e);
		map.addRoad(newRoad);
		// background.add(new GMLShapeInfo(newRoad, "Roads", Color.BLACK,
		// ROAD_COLOUR));
	}

	private boolean noClash(Coordinate[] road, GMLRoad connectedRoad,
			GMLBuilding building, GMLMap result) {
		Geometry r = createGeometry(road);
		Iterator<GMLShape> it = result.getAllShapes().iterator();

		while (it.hasNext()) {
			GMLShape shape = (GMLShape) it.next();

			if (shape.getID() != building.getID()
					&& shape.getID() != connectedRoad.getID()) {
				if (shapesIntersect(shape, road)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean shareEdge(GMLDirectedEdge[] edges) {
		double edge1startx = edges[0].getStartCoordinates().getX();
		double edge1starty = edges[0].getStartCoordinates().getY();
		double edge1endx = edges[0].getEndCoordinates().getX();
		double edge1endy = edges[0].getEndCoordinates().getY();
		double edge2startx = edges[1].getStartCoordinates().getX();
		double edge2starty = edges[1].getStartCoordinates().getY();
		double edge2endx = edges[1].getEndCoordinates().getX();
		double edge2endy = edges[1].getEndCoordinates().getY();
		if (edge1startx == edge2startx && edge1starty == edge2starty
				&& edge1endx == edge2endx && edge1endy == edge2endy) {
			return true;
		} else if (edge1endx == edge2startx && edge1endy == edge2starty
				&& edge1startx == edge2endx && edge1starty == edge2endy) {
			return true;
		} else {
			return false;
		}
	}

	private Coordinate makeCoordinate(Point2D point) {
		// TODO Auto-generated method stub
		return new Coordinate(point.getX(), point.getY());
	}

	private Point2D makePoint(Coordinate point) {
		// TODO Auto-generated method stub
		return new Point2D(point.x, point.y);
	}

	private Point2D makePoint(GMLCoordinates gmlCoordinates) {
		return new Point2D(gmlCoordinates.getX(), gmlCoordinates.getY());
	}

	/*
	 * finds the closest edges between a building and road returns the 2 element
	 * array with building edge first
	 */
	private GMLDirectedEdge[] closestEdges(GMLBuilding building, GMLRoad road,
			GMLMap result) {
		List<GMLDirectedEdge> bEdges = building.getEdges();
		List<GMLDirectedEdge> rEdges = road.getEdges();

		Iterator<GMLDirectedEdge> bit = bEdges.iterator();
		GMLDirectedEdge[] closest = new GMLDirectedEdge[2];
		double dist = Double.MAX_VALUE;
		while (bit.hasNext()) {
			GMLDirectedEdge bEdge = (GMLDirectedEdge) bit.next();
			/*
			 * get mid point of building edge
			 */
			double midx = (bEdge.getEndCoordinates().getX() + bEdge
					.getStartCoordinates().getX()) / 2.0;
			double midy = (bEdge.getEndCoordinates().getY() + bEdge
					.getStartCoordinates().getY()) / 2.0;
			Point2D bmid = new Point2D(midx, midy);
			Line2D bline = new Line2D(makePoint(bEdge.getStartCoordinates()),
					makePoint(bEdge.getEndCoordinates()));
			Iterator<GMLDirectedEdge> rit = rEdges.iterator();
			while (rit.hasNext()) {
				GMLDirectedEdge rEdge = (GMLDirectedEdge) rit.next();

				Line2D rline = new Line2D(new Point2D(rEdge
						.getStartCoordinates().getX(), rEdge
						.getStartCoordinates().getY()), new Point2D(rEdge
						.getEndCoordinates().getX(), rEdge.getEndCoordinates()
						.getY()));
				Point2D point = GeometryTools2D.getClosestPointOnSegment(rline,
						bmid);
				double temp = GeometryTools2D.getDistance(point, bmid);
				if (temp <= dist) {// &&
					// GeometryTools2D.getAngleBetweenVectors(bline.getDirection(),
					// rline.getDirection())>140) {
					dist = temp;
					closest[0] = bEdge;
					closest[1] = rEdge;
				}
			}
		}
		return closest;
	}

	/*
	 * find closest road using centroid of building and road edges would be more
	 * accuate but this should do the job in most cases
	 */
	private GMLRoad closestRoad(GMLMap result, GMLBuilding building,
			int maxRoadID2) {
		List<GMLDirectedEdge> Bedges = building.getEdges();

		List<Point2D> tempedges = new ArrayList<Point2D>();
		Iterator<GMLDirectedEdge> eit = Bedges.iterator();
		int i = 0;
		while (eit.hasNext()) {
			GMLDirectedEdge edge = (GMLDirectedEdge) eit.next();
			if (GeometryTools2D.getDistance(makePoint(edge
					.getStartCoordinates()),
					makePoint(edge.getEndCoordinates())) <= entrancewidth * 2) {
				tempedges.add(new Point2D(
						(edge.getStartCoordinates().getX() + edge
								.getEndCoordinates().getX()) / 2, (edge
								.getStartCoordinates().getY() + edge
								.getEndCoordinates().getY()) / 2));
			}
		}
		Point2D[] c = null;
		if (tempedges.size() == 0) {
			eit = Bedges.iterator();
			c = new Point2D[Bedges.size()];
			while (eit.hasNext()) {
				GMLDirectedEdge edge = (GMLDirectedEdge) eit.next();

				c[i] = new Point2D((edge.getStartCoordinates().getX() + edge
						.getEndCoordinates().getX()) / 2, (edge
						.getStartCoordinates().getY() + edge
						.getEndCoordinates().getY()) / 2);
				i++;
			}
		} else {
			c = new Point2D[tempedges.size()];
			Iterator<Point2D> tempit = tempedges.iterator();
			while (tempit.hasNext()) {
				Point2D tempoint = (Point2D) tempit.next();
				c[i] = tempoint;
				i++;
			}
		}

		Set<GMLRoad> roads = result.getRoads();
		Iterator<GMLRoad> rit = roads.iterator();
		GMLRoad closest = null;
		double dist = Double.MAX_VALUE;
		while (rit.hasNext()) {
			GMLRoad road = (GMLRoad) rit.next();
			List<GMLDirectedEdge> Redges = road.getEdges();
			double temp = distanceToClosestEdge(c, Redges);
			if (temp <= dist) {// && road.getID() <= maxRoadID2) {
				dist = temp;
				closest = road;
			}
		}
		return closest;
	}

	private double distanceToClosestEdge(Point2D[] c,
			List<GMLDirectedEdge> redges) {
		double dist = Double.MAX_VALUE;
		for (int i = 0; i < c.length; i++) {
			Iterator<GMLDirectedEdge> it = redges.iterator();
			while (it.hasNext()) {
				GMLDirectedEdge edge = (GMLDirectedEdge) it.next();
				Line2D eline = new Line2D(
						makePoint(edge.getStartCoordinates()), makePoint(edge
								.getEndCoordinates()));
				Point2D rPoint = GeometryTools2D.getClosestPointOnSegment(
						eline, c[i]);
				double temp = GeometryTools2D.getDistance(rPoint, c[i]);
				if (temp <= dist) {
					dist = temp;
				}
			}
		}
		return dist;
	}

	private void connectNetwork(GMLMap result) {
		/*
		 * connect fully parsed road and building network could be slow - for
		 * every shape find all the shapes it neighbours and make connected
		 */
		Set<GMLShape> shapes = result.getAllShapes();

		Iterator<GMLShape> it = shapes.iterator();
		while (it.hasNext()) {
			GMLShape shape = (GMLShape) it.next();
			Iterator<GMLShape> secIt = result.getAllShapes().iterator();
			while (secIt.hasNext()) {
				GMLShape shape2 = (GMLShape) secIt.next();
				if (shape.getID() != shape2.getID()) {
					List<GMLDirectedEdge> connections = connected(shape, shape2);
					for (GMLDirectedEdge connection : connections) {
						shape.setNeighbour(connection, shape2.getID());
					}
				}
			}
		}

	}

	private List<GMLDirectedEdge> connected(GMLShape road, GMLShape road2) {
		List<GMLDirectedEdge> list = new ArrayList<GMLDirectedEdge>();

		/*
		 * decides if two roads share an edge
		 */
		if (road instanceof GMLBuilding && road2 instanceof GMLBuilding) {
			return list;
		}
		List<GMLDirectedEdge> edges1 = road.getEdges();
		List<GMLDirectedEdge> edges2 = road2.getEdges();
		Iterator<GMLDirectedEdge> it1 = edges1.iterator();
		while (it1.hasNext()) {
			GMLDirectedEdge edge1 = (GMLDirectedEdge) it1.next();
			Iterator<GMLDirectedEdge> it2 = edges2.iterator();
			double edge1StartX = edge1.getStartCoordinates().getX();
			double edge1StartY = edge1.getStartCoordinates().getY();
			double edge1EndX = edge1.getEndCoordinates().getX();
			double edge1EndY = edge1.getEndCoordinates().getY();
			while (it2.hasNext()) {
				GMLDirectedEdge edge2 = (GMLDirectedEdge) it2.next();
				double edge2StartX = edge2.getStartCoordinates().getX();
				double edge2StartY = edge2.getStartCoordinates().getY();
				double edge2EndX = edge2.getEndCoordinates().getX();
				double edge2EndY = edge2.getEndCoordinates().getY();

				if (edge1StartX == edge2StartX && edge1StartY == edge2StartY
						&& edge1EndX == edge2EndX && edge1EndY == edge2EndY) {
					list.add(edge1);
				}
				if (edge1EndX == edge2StartX && edge1EndY == edge2StartY
						&& edge1StartX == edge2EndX && edge1StartY == edge2EndY) {
					list.add(edge1);
				}
			}
		}
		return list;
	}

	@Override
	public Document write(GMLMap map) {
		// Not implemented
		throw new RuntimeException("OrdnanceSurveyFormat.write not implemented");
	}

	private void readBuildings(Document doc, GMLMap result) {
		for (Object next : BUILDING_XPATH.selectNodes(doc)) {
			Logger.debug("Found building element: " + next);
			Element e = (Element) next;
			// String fid = e.attributeValue("fid");
			// long id = Long.parseLong(fid.substring(FID_PREFIX_LENGTH)); //
			// Strip off the 'osgb' prefix
			String coordinatesString = ((Element) SHAPE_XPATH.evaluate(e))
					.getText();

			// Geometry g = createGeometry(coordinatesString);
			// System.out.println(g.getArea());
			// if(g.getArea()>0.0000000000001){
			List<GMLDirectedEdge> edges = readEdges(coordinatesString, result);
			GMLBuilding b = result.createBuilding(edges);
			// double area = b.getBounds().getWidth()*b.getBounds().getHeight();
			List<Point2D> vertices = coordinatesToVertices(b
					.getUnderlyingCoordinates());
			double area = GeometryTools2D.computeArea(vertices);
			// 70 is ok
			if (area > 50) {
				// debug.show("New building", new GMLShapeInfo(b,
				// "New building", Color.BLACK, NEW_BUILDING_COLOUR));
				// background.add(new GMLShapeInfo(b, "Buildings", Color.BLACK,
				// BUILDING_COLOUR));
				// System.out.println("adding");
			} else {
				result.removeBuilding(b);
				// System.out.println("removing");
			}
			// }
		}
	}

	private List<Point2D> coordinatesToVertices(List<GMLCoordinates> coords) {
		List<Point2D> result = new ArrayList<Point2D>(coords.size());
		for (GMLCoordinates c : coords) {
			result.add(new Point2D(c.getX(), c.getY()));
		}
		return result;
	}

	private void readRoads(Document doc, GMLMap result) {

		// debug.activate();
		for (Object next : ROAD_XPATH.selectNodes(doc)) {
			Logger.debug("Found road element: " + next);
			Element e = (Element) next;
			// String fid = e.attributeValue("fid");
			// long id = Long.parseLong(fid.substring(FID_PREFIX_LENGTH)); //
			// Strip off the 'osgb' prefix
			String coordinatesString = ((Element) SHAPE_XPATH.evaluate(e))
					.getText();
			Object inner = INNER_RING_XPATH.evaluate(e);

			if ((inner instanceof Collection) && ((Collection) inner).isEmpty()) {
				// no inner rings?
				List<GMLDirectedEdge> edges = readEdges(coordinatesString,
						result);
				GMLRoad road = result.createRoad(edges);
				maxRoadID = road.getID();
				// debug.show("New road", new GMLShapeInfo(road,
				// "New road",Color.BLACK, NEW_ROAD_COLOUR));
				// background.add(new GMLShapeInfo(road, "Roads", Color.BLACK,
				// ROAD_COLOUR));
			} else {
				// 1 or more inner rings
				if (inner instanceof Collection) {
					// more than 1 inner ring so leave it just now
				} else {
					// must be just 1 so is an element
					Element innerE = (Element) inner;
					String innerCoordinatesString = innerE.getText();
					Geometry outerG = createGeometry(coordinatesString);
					Geometry innerG = createGeometry(innerCoordinatesString);
					Geometry shape = outerG.difference(innerG);

					Coordinate[] coords = shape.getCoordinates();
					List<GMLDirectedEdge> edges = readEdges(coords, result);
					GMLRoad road = result.createRoad(edges);
					maxRoadID = road.getID();
					// debug.show("New road", new GMLShapeInfo(road,
					// "New road",Color.BLACK, NEW_ROAD_COLOUR));
					// background.add(new GMLShapeInfo(road, "Roads",
					// Color.BLACK, ROAD_COLOUR));

				}
			}
		}
		// debug.deactivate();
	}

	private boolean sharesEdge(Geometry a, Geometry b) {
		/*
		 * determines whether 2 geometries are touching in more than one point
		 * used in triangulation merging
		 */

		Coordinate[] aC = a.getCoordinates();
		Coordinate[] bC = b.getCoordinates();
		boolean touch1 = true;
		boolean touch2 = false;
		boolean touch3 = false;
		for (int i = 0; i < aC.length; i++) {
			for (int j = 0; j < bC.length; j++) {
				if (aC[i].equals(bC[j]) && !touch1) {
					touch1 = true;
				} else if (aC[i].equals(bC[j]) && touch1 && !touch2) {
					touch2 = true;
				} else if (aC[i].equals(bC[j]) && touch2 && !touch3) {
					touch3 = true;
				} else if (aC[i].equals(bC[j]) && touch3) {
					return true;
				}
			}
		}

		return false;

	}

	private Collection<Line2D> getLines(Geometry a) {
		ArrayList<Line2D> lines = new ArrayList();
		for (int i = 0; i < a.getCoordinates().length; i++) {
			Line2D line;
			if (i < a.getCoordinates().length - 1) {
				line = new Line2D(new Point2D(a.getCoordinates()[i].x, a
						.getCoordinates()[i].y), new Point2D(
						a.getCoordinates()[0].x, a.getCoordinates()[0].y));
			} else {
				line = new Line2D(new Point2D(a.getCoordinates()[i].x, a
						.getCoordinates()[i].y), new Point2D(
						a.getCoordinates()[0].x, a.getCoordinates()[0].y));
			}
			lines.add(line);
		}
		return lines;
	}

	private Coordinate pointOnEdge(Geometry g, Geometry edge) {
		Coordinate[] t = g.getCoordinates();
		Coordinate[] o = edge.getCoordinates();
		for (int i = 0; i < t.length; i++) {
			for (int j = 0; j < o.length; j++) {
				if (t[i].equals2D(o[j])) {
					return t[i];
				}
			}
		}
		return null;
	}

	private boolean onEdge(Geometry g, Geometry outerG) {
		/*
		 * determines if a geometry is on the outer segment or inner
		 */

		Coordinate[] t = g.getCoordinates();
		Coordinate[] o = outerG.getCoordinates();
		for (int i = 0; i < t.length; i++) {
			for (int j = 0; j < o.length; j++) {
				if (t[i].equals(o[j])) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean shapesIntersect(GMLShape shape1, Coordinate[] shape2) {
		List<GMLDirectedEdge> edges1 = shape1.getEdges();
		// List<GMLDirectedEdge> edges2 = shape2.getEdges();

		for (GMLDirectedEdge edge1 : edges1) {
			Line2D edge1Line = new Line2D(
					makePoint(edge1.getStartCoordinates()), makePoint(edge1
							.getEndCoordinates()));

			for (int i = 0; i < shape2.length; i++) {

				Line2D edge2Line = new Line2D(makePoint(shape2[i]),
						makePoint(shape2[(i + 1) % shape2.length]));
				double intersection1 = edge1Line.getIntersection(edge2Line);
				double intersection2 = edge2Line.getIntersection(edge1Line);

				if (intersection1 >= 0 && intersection1 <= 1
						&& intersection2 >= 0 && intersection2 <= 1) {
					return true;
				}
			}
		}
		return false;
	}

	private Geometry createGeometry(Coordinate[] coords) {

		return factory.createLinearRing(coords);

		/*
		 * takes a string of coordinates and returns a jts geometry object
		 */
		// make the WKT string
		/*
		 * String wktString = "LINESTRING ("; for (int i = 0; i < coords.length;
		 * i++) { GMLCoordinates nextApex = new GMLCoordinates(coords[i].x,
		 * coords[i].y); wktString = wktString + nextApex.getX() + " "; if (i +
		 * 1 < coords.length) { wktString = wktString + nextApex.getY() + ", ";
		 * } else { wktString = wktString + nextApex.getY(); } } wktString =
		 * wktString + ")"; try { return new WKTReader().read(wktString); }
		 * catch (ParseException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } return null;
		 */
	}

	private Geometry createGeometry(String coordinatesString) {
		/*
		 * takes a string of coordinates and returns a jts geometry object
		 */
		// make the WKT string
		StringTokenizer tokens = new StringTokenizer(coordinatesString, " ");
		String wktString = "LINESTRING (";
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken();
			GMLCoordinates nextApex = new GMLCoordinates(token);
			wktString = wktString + nextApex.getX() + " ";
			if (tokens.hasMoreTokens()) {
				wktString = wktString + nextApex.getY() + ", ";
			} else {
				wktString = wktString + nextApex.getY();
			}
		}
		wktString = wktString + ")";
		try {
			return new WKTReader().read(wktString);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private void readSpaces(Document doc, GMLMap result) {
		/*
		 * for (Object next : SPACE_XPATH.selectNodes(doc)) {
		 * Logger.debug("Found space element: " + next); Element e =
		 * (Element)next; String fid = e.attributeValue("fid"); long id =
		 * Long.parseLong(fid.substring(4)); // Strip off the 'osgb' prefix
		 * String coordinatesString =
		 * ((Element)SHAPE_XPATH.evaluate(e)).getText(); List<GMLEdge> edges =
		 * readEdges(coordinatesString, result); result.createSpace(edges); }
		 */
	}

	private List<GMLDirectedEdge> readEdges(Coordinate[] coords, GMLMap map) {
		List<GMLDirectedEdge> edges = new ArrayList<GMLDirectedEdge>();
		GMLCoordinates lastApex = null;
		GMLNode fromNode = null;
		GMLNode toNode = null;
		for (int i = 0; i < coords.length; i++) {
			GMLCoordinates nextApex = new GMLCoordinates(coords[i].x,
					coords[i].y);
			toNode = map.createNode(nextApex);
			if (lastApex != null) {
				edges.add(new GMLDirectedEdge(map.createEdge(fromNode, toNode),
						true));
			}
			lastApex = nextApex;
			fromNode = toNode;
		}
		return edges;
	}

	private List<GMLDirectedEdge> readEdges(String coordinatesString, GMLMap map) {
		List<GMLDirectedEdge> edges = new ArrayList<GMLDirectedEdge>();
		StringTokenizer tokens = new StringTokenizer(coordinatesString, " ");
		GMLCoordinates lastApex = null;
		GMLNode fromNode = null;
		GMLNode toNode = null;
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken();
			GMLCoordinates nextApex = new GMLCoordinates(token);
			toNode = map.createNode(nextApex);
			if (lastApex != null) {
				edges.add(new GMLDirectedEdge(map.createEdge(fromNode, toNode),
						true));
			}
			lastApex = nextApex;
			fromNode = toNode;
		}
		return edges;
	}
}
