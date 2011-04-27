package maps.gml.formats;

import gis2.Scenario;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import maps.MapReader;
import maps.gml.GMLBuilding;
import maps.gml.GMLDirectedEdge;
import maps.gml.GMLMap;
import maps.gml.GMLShape;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.XPath;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import rescuecore2.log.Logger;

/**
 * in this class, we provide method to do transformation between osgb32 (adopted
 * by ordnance survey map) and wgs84 (used by google map) coordinate system
 * 
 * @author Bing Shi
 * 
 */

public class CoordinateTransformationBetweenOSandWGS {

	private String map = "d:\\map.gml";
	private CoordinateProjection[] coProjections = new CoordinateProjection[876951];
	private static final String FEATURE_CODE_BUILDING = "10021";
	private static final XPath BUILDING_XPATH = DocumentHelper
			.createXPath("//osgb:topographicMember/osgb:TopographicArea[osgb:featureCode[text()='"
					+ FEATURE_CODE_BUILDING + "']]");
	private static final XPath SHAPE_XPATH = DocumentHelper
			.createXPath("osgb:polygon/gml:Polygon/gml:outerBoundaryIs/gml:LinearRing/gml:coordinates");

	private static final String OSGB_NAMESPACE_URI = "http://www.ordnancesurvey.co.uk/xml/namespaces/osgb";

	private static final Namespace OSGB_NAMESPACE = DocumentHelper
			.createNamespace("osgb", OSGB_NAMESPACE_URI);

	private static final QName FEATURE_COLLECTION_QNAME = DocumentHelper
			.createQName("FeatureCollection", OSGB_NAMESPACE);
	private static final QName TOPOGRAPHIC_AREA_QNAME = DocumentHelper
			.createQName("TopographicArea", OSGB_NAMESPACE);

	private static final Map<String, String> URIS = new HashMap<String, String>();
	static {
		URIS.put("gml", Common.GML_NAMESPACE_URI);
		URIS.put("xlink", Common.XLINK_NAMESPACE_URI);
		URIS.put("osgb", OSGB_NAMESPACE_URI);
		BUILDING_XPATH.setNamespaceURIs(URIS);
		SHAPE_XPATH.setNamespaceURIs(URIS);
	}

	/**
	 * load square file of GB for transformation
	 */
	public void init() {
		try {
			BufferedReader read = new BufferedReader(new FileReader(new File(
					"d:\\osgb.txt")));
			int i = 0;
			while (true) {
				String s = read.readLine();
				if (s != null) {
					StringTokenizer st = new StringTokenizer(s, ",");
					CoordinateProjection coordinateProjection = new CoordinateProjection();
					coordinateProjection.setIndex(Integer.valueOf(st
							.nextToken()));
					coordinateProjection.setEastIndex(Integer.valueOf(st
							.nextToken()));
					coordinateProjection.setNorthIndex(Integer.valueOf(st
							.nextToken()));
					coordinateProjection.setSe(Double.valueOf(st.nextToken()));
					coordinateProjection.setSn(Double.valueOf(st.nextToken()));
					coordinateProjection.setSg(Double.valueOf(st.nextToken()));
					coProjections[i] = coordinateProjection;
					i++;
				} else {
					break;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * from map.gml, get buildings' coordinate information
	 * @return
	 */
	private ArrayList<ArrayList<Double>> getCoordinatesOfBuildings() {
		try {
			FileReader reader = new FileReader(new java.io.File(map));
			Logger.debug("Parsing GML");
			SAXReader saxReader = new SAXReader();
			Document doc = saxReader.read(reader);
			ArrayList<ArrayList<Double>> coordinatesOfBuildings = new ArrayList<ArrayList<Double>>();
			for (Object next : BUILDING_XPATH.selectNodes(doc)) {
				Logger.debug("Found building element: " + next);
				Element e = (Element) next;
				String coordinatesString = ((Element) SHAPE_XPATH.evaluate(e))
						.getText();
				StringTokenizer tokens = new StringTokenizer(coordinatesString,
						" ");
				ArrayList<Double> cos = new ArrayList<Double>();
				while (tokens.hasMoreTokens()) {
					String token = tokens.nextToken();
					StringTokenizer st = new StringTokenizer(token, ",");
					cos.add(Double.valueOf(st.nextToken()));
					cos.add(Double.valueOf(st.nextToken()));
				}
				coordinatesOfBuildings.add(cos);
			}
			return coordinatesOfBuildings;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * transformation coordinates from osgb36 to wgs89, 
	 * and then convert to lat/long representation.
	 * but they are still based on nation grid reference
	 * @param coordinatesOfBuildings
	 * @return
	 */
	public ArrayList<ArrayList<Double>> transformOsgbtoWgs(
			ArrayList<ArrayList<Double>> coordinatesOfBuildings) {
		ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < coordinatesOfBuildings.size(); i++) {
			ArrayList<Double> co = coordinatesOfBuildings.get(i);
			ArrayList<Double> ro = new ArrayList<Double>();
			for (int j = 0; j < co.size(); j = j + 2) {
				double c[] = new double[] { co.get(j), co.get(j + 1) };
				double r1[] = transformOsgbtoWgs(c);
				double r[] = convertEastandNorthToLatLong(r1);
				//kml save coordinate in longitude, latitude, height
				ro.add(r[1]*180/Math.PI);
				ro.add(r[0]*180/Math.PI);
				System.out.println("osgb: "+c[0]+" "+c[1]);
				System.out.println("wgs: "+r1[0] + " " + r1[1]);
				System.out.println("lat/long: "+r[0]*180/Math.PI + " " + r[1]*180/Math.PI);
				System.out.println("===========================");
			}
			result.add(ro);
			
			System.out.println("------------------------");
		}
		return result;
	}

	/**
	 * transform c[] from osgb to wgs
	 * 
	 * @param c c[0] east, c[1] north
	 * @return
	 */
	public double[] transformOsgbtoWgs(double[] c) {
		double preSe = Double.MAX_VALUE;
		double preSn = Double.MAX_VALUE;
		double r[] = new double[] { c[0], c[1] };
		while (true) {
			int east = (int) r[0] / 1000;
			int north = (int) r[1] / 1000;
			int index = east + (north * 701);
			double se0 = coProjections[index].getSe();
			double sn0 = coProjections[index].getSn();
			index = east + 1 + (north * 701);
			double se1 = coProjections[index].getSe();
			double sn1 = coProjections[index].getSn();
			index = east + 1 + ((north + 1) * 701);
			double se2 = coProjections[index].getSe();
			double sn2 = coProjections[index].getSn();
			index = east + ((north + 1) * 701);
			double se3 = coProjections[index].getSe();
			double sn3 = coProjections[index].getSn();
			double dx = r[0] - east * 1000;
			double dy = r[1] - north * 1000;
			double t = dx / 1000;
			double u = dy / 1000;
			double se = (1 - t) * (1 - u) * se0 + t * (1 - u) * se1 + t * u
					* se2 + (1 - t) * u * se3;
			double sn = (1 - t) * (1 - u) * sn0 + t * (1 - u) * sn1 + t * u
					* sn2 + (1 - t) * u * sn3;
			r[0] = c[0] - se;
			r[1] = c[1] - sn;
			//System.out.println(se+"   "+sn);
			if (Math.abs(preSe - se) < 0.0001 && Math.abs(preSn - sn) < 0.0001) {
				return r;
			} else {
				preSe = se;
				preSn = sn;
			}
		}
	}

	/**
	 * transform c[] from wgs to osgb
	 * @param c
	 * @return
	 */
	public double[] transformWgstoOsgb(double[] c) {
		double r[] = new double[2];
		int east = (int) c[0] / 1000;
		int north = (int) c[1] / 1000;
		int index = east + (north * 701);
		double se0 = coProjections[index].getSe();
		double sn0 = coProjections[index].getSn();
		index = east + 1 + (north * 701);
		double se1 = coProjections[index].getSe();
		double sn1 = coProjections[index].getSn();
		index = east + 1 + ((north + 1) * 701);
		double se2 = coProjections[index].getSe();
		double sn2 = coProjections[index].getSn();
		index = east + ((north + 1) * 701);
		double se3 = coProjections[index].getSe();
		double sn3 = coProjections[index].getSn();
		double dx = c[0] - east * 1000;
		double dy = c[1] - north * 1000;
		double t = dx / 1000;
		double u = dy / 1000;
		double se = (1 - t) * (1 - u) * se0 + t * (1 - u) * se1 + t * u * se2
				+ (1 - t) * u * se3;
		double sn = (1 - t) * (1 - u) * sn0 + t * (1 - u) * sn1 + t * u * sn2
				+ (1 - t) * u * sn3;
		r[0] = c[0] + se;
		r[1] = c[1] + sn;
		return r;
	}

	/**
	 * convert to latitude and longitude
	 * adopt national grid projection and since we consider wgs we use ellipsoid of wgs84.
	 * note that the easting and northing for osgb32 and etrs89 
	 * is still based on national grid reference, i.e. etrs89 is not based on UMT
	 * therefore, when converting lat/long, we still consider national grid projection
	 * however, for the lat/long adopted by google map, we should consider wgs ellipsoid
	 * @param c
	 * @return
	 */
	public double[] convertEastandNorthToLatLong(double[] c) {
		double r[] = new double[2];
		double E0 = 400000;
		double N0 = -100000;
		double F0 = 0.9996012717;
		double a = 6378137.0;//6377563.396;
		double b = 6356752.3141;//6356256.910;
		double o0 = 49.0 / 180.0 * Math.PI;
		double r0 = -2.0 / 180.0 * Math.PI;
		double e2 =(a*a-b*b)/(a*a);
		double E = c[0];
		double N = c[1];

		double o1 = (N - N0) / (a * F0) + o0;
		double n = (a - b) / (a + b);
		double temp;
		double M;
		while (true) {
			M = b
					* F0
					* ((1 + n + 1.25 * n * n + 1.25 * n * n * n) * (o1 - o0)
							- (3 * n + 3 * n * n + 21.0 / 8 * n * n * n)
							* Math.sin(o1 - o0) * Math.cos(o1 + o0)
							+ (15.0 / 8 * n * n + 15.0 / 8 * n * n * n)
							* Math.sin(2 * (o1 - o0)) * Math.cos(2 * (o1 + o0)) - 35.0
							/ 24
							* n
							* n
							* n
							* Math.sin(3 * (o1 - o0))
							* Math.cos(3 * (o1 + o0)));
			temp = Math.abs(N - N0 - M);
			if (temp >= 0.00001)
				o1 = (N - N0 - M) / (a * F0) + o1;
			else
				break;
		}
		double v = a * F0*Math.pow(1 - e2 * Math.sin(o1) * Math.sin(o1),
				-0.5);
		double p =a * F0 * (1 - e2) *Math.pow((1 - e2 * Math.sin(o1) * Math.sin(o1)),
				-1.5);
		double l2 = v / p - 1;
		
		double vii = Math.tan(o1) / (2 * p * v);
		double viii = Math.tan(o1)
				/ (24 * p * v * v * v)
				* (5 + 3 * Math.tan(o1) * Math.tan(o1) + l2 - 9 * Math.tan(o1)
						* Math.tan(o1) * l2);
		double ix = Math.tan(o1)
				/ (720 * p * v * v * v * v * v)
				* (61 + 90 * Math.tan(o1) * Math.tan(o1) + 45 * Math.tan(o1)
						* Math.tan(o1) * Math.tan(o1) * Math.tan(o1));
		double X = 1.0 / (Math.cos(o1) * v);
		double XI = (v / p + 2 * Math.tan(o1) * Math.tan(o1))
				/ (Math.cos(o1) * 6 * v * v * v);
		double XII = (5 + 28 * Math.tan(o1) * Math.tan(o1) + 24 * Math.pow(
				Math.tan(o1), 4))
				/ (120 * v * v * v * v * v * Math.cos(o1));
		double XIIA = (61 + 662 * Math.tan(o1) * Math.tan(o1) + 1320
				* Math.pow(Math.tan(o1), 4) + 720 * Math.pow(Math.tan(o1), 6))
				/ (Math.cos(o1) * 5040 * Math.pow(v, 7));
		r[0] = o1 - vii * (E - E0) * (E - E0) + viii * Math.pow(E - E0, 4) - ix
				* Math.pow(E - E0, 6);
	//	System.out.println(E+" "+X+" "+XI+" "+XII+" "+XIIA);
		r[1] = r0 + X * (E - E0) - XI * Math.pow(E - E0, 3) + XII
				* Math.pow(E - E0, 5) - XIIA * Math.pow(E - E0, 7);

		return r;
	}

	public static void main(String args[]) {
		CoordinateTransformationBetweenOSandWGS c = new CoordinateTransformationBetweenOSandWGS();
		c.init();
		//double a[]=new double[]{439896.95,115235.2};//{651409.903,313177.270};//{610060,5643782};//{651409.792,313177.488};
		//System.out.println(c.transformOsgbtoWgs(a)[0]+" "+c.transformOsgbtoWgs(a)[1]);
		//double r[]=c.convertEastandNorthToLatLong(a);
		//System.out.println(r[0]*180/Math.PI+" "+r[1]*180/Math.PI);
		c.outPutBuildingCoordinatestoKML(c.transformOsgbtoWgs(c.getCoordinatesOfBuildings()));
	}

	public void outPutBuildingCoordinatestoKML(ArrayList<ArrayList<Double>> co)
	{
		Element root = DocumentHelper.createElement("kml");
		root.addAttribute("xmlns", "http://earth.google.com/kml/2.1");
		Document doc = DocumentHelper.createDocument(root);
		Element placemark=DocumentHelper.createElement("Placemark");
		Element name=DocumentHelper.createElement("name");
		name.setText("buildings' coordinates in lat/long");
		placemark.add(name);
		for(int i=0;i<co.size();i++)
		{
			ArrayList<Double> c=co.get(i);
			String t="";
			for(int j=0;j<c.size();j=j+2)
			{
				t=t+c.get(j)+","+c.get(j+1)+",0"+"\n";
			}
			Element polygon=DocumentHelper.createElement("Polygon");
			Element outer=DocumentHelper.createElement("outerBoundaryIs");
			Element linearRingElement=DocumentHelper.createElement("LinearRing");
			Element coordinates=DocumentHelper.createElement("coordinates");
			coordinates.setText(t);
			linearRingElement.add(coordinates);
			outer.add(linearRingElement);
			polygon.add(outer);
			placemark.add(polygon);
		}
		root.add(placemark);		
		try{
        XMLWriter writer = new XMLWriter(new FileOutputStream(new File("d:\\coordinates.kml")), OutputFormat.createPrettyPrint());
        writer.write(doc);
        writer.flush();
        writer.close();}catch (Exception e) {
			e.printStackTrace();
		}
	}
}
