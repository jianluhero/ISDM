/**
 * 
 */
package maps.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;

/**
 * @author Sebastian
 * 
 */
public class PolygonSplitter {

	public static Polygon[] splitPolygon(Polygon polygon, double maxNodes,
			double maxCircumference) {

		List<Polygon> unfinishedPolygons = new LinkedList<Polygon>();
		List<Polygon> finishedPolygons = new LinkedList<Polygon>();

		unfinishedPolygons.add(polygon);

		while (unfinishedPolygons.size() > 0) {
			Polygon next = unfinishedPolygons.remove(0);
			if (next.getNumVertices() > maxNodes
					&& next.getCircumference() > maxCircumference) {
				List<Polygon> split = splitPolygonEvenly(next);
				if (split != null) {
					unfinishedPolygons.addAll(split);
				} else {
					finishedPolygons.add(next);
				}
			} else {
				finishedPolygons.add(next);
			}
		}

		return finishedPolygons.toArray(new Polygon[0]);
	}

	/**
	 * @param apexList
	 */
	public static Polygon reduceEdges(Polygon polygon, double minDegree) {
		List<Point2D> points = new ArrayList<Point2D>(polygon.x.length);
		Point2D lastPoint = new Point2D(polygon.x[0], polygon.y[0]);
		points.add(lastPoint);
		for (int i = 1; i < polygon.x.length; i++) {
			Point2D nextPoint = new Point2D(polygon.x[i], polygon.y[i]);
			Point2D nextPointAfter;
			if (i == polygon.x.length - 1) {
				nextPointAfter = points.get(0);
			} else {
				nextPointAfter = new Point2D(polygon.x[i + 1], polygon.y[i + 1]);
			}
			Line2D line1 = new Line2D(lastPoint, nextPoint);
			Line2D line2 = new Line2D(nextPoint, nextPointAfter);
			if (GeometryTools2D.getRightAngleBetweenLines(line1, line2) > minDegree) {
				points.add(nextPoint);
				lastPoint = nextPoint;
			}
		}
		double[] x = new double[points.size()];
		double[] y = new double[points.size()];

		for (int i = 0; i < points.size(); i++) {
			x[i] = points.get(i).getX();
			y[i] = points.get(i).getY();
		}
		return new Polygon(x, y);
	}

	public static List<Polygon> splitPolygonEvenly(Polygon next) {
		List<Line2D> edges = new ArrayList<Line2D>();
		double[] x = next.x;
		double[] y = next.y;

		for (int i = 0; i < x.length; i++) {
			edges.add(new Line2D(new Point2D(x[i], y[i]), new Point2D(x[(i + 1)
					% x.length], y[(i + 1) % x.length])));
		}

		double bestCircumference = Double.MAX_VALUE;

		List<Polygon> bestPolygons = null;

		for (int i = 0; i < x.length; i++) {
			for (int j = i + 2; j < x.length; j++) {
				if (i == 0 && j == x.length - 1) {
					continue;
				}
				Line2D newLine = new Line2D(new Point2D(x[i], y[i]),
						new Point2D(x[j], y[j]));

				// First check this edge does not intersect other edges than
				// those immediately around the points
				boolean valid = true;
				int intersections = 0;
				for (int k = 0; k < edges.size(); k++) {
					if (k == i || ((i - 1 + x.length) % x.length) == k) {
						continue;
					}
					if (k == j || ((j - 1 + x.length) % x.length) == k) {
						continue;
					}
					double intersectionE2HWL = edges.get(k).getIntersection(
							newLine);
					double intersectionHWL2E = newLine.getIntersection(edges
							.get(k));

					// Avoid parallel lines
					if (intersectionE2HWL == Double.NaN) {
						valid = false;
						break;
					}

					// Exactly hits a vertex - ignore this one.
					if (intersectionHWL2E == 0 || intersectionHWL2E == 1) {
						valid = false;
						break;
					}

					if (intersectionE2HWL > 0) {
						if (intersectionHWL2E > 0 && intersectionHWL2E < 1) {
							if (intersectionE2HWL < 1) {
								valid = false;
								break;
							} else {
								intersections++;
							}
						}
					}
					// Check for intersection
					/*
					 * if () { if (intersectionHWL2E >= 0) { if
					 * (intersectionHWL2E < 1) { //valid = false; //break; }
					 * else { // intersections++; } } }
					 */
				}
				if (valid && (intersections % 2 == 0)) {
					// Try to construct polygons
					List<Polygon> polygons = new ArrayList<Polygon>(2);

					List<Point2D> firstPolygon = new ArrayList<Point2D>();
					List<Point2D> secondPolygon = new ArrayList<Point2D>();

					for (int a = i; a != (j + 1) % x.length; a = (a + 1)
							% x.length) {
						firstPolygon.add(new Point2D(x[a], y[a]));
					}
					for (int a = j; a != (i + 1) % x.length; a = (a + 1)
							% x.length) {
						secondPolygon.add(new Point2D(x[a], y[a]));
					}

					double[] firstXs = new double[firstPolygon.size()];
					double[] firstYs = new double[firstPolygon.size()];

					double[] secondXs = new double[secondPolygon.size()];
					double[] secondYs = new double[secondPolygon.size()];

					for (int a = 0; a < firstXs.length; a++) {
						firstXs[a] = firstPolygon.get(a).getX();
						firstYs[a] = firstPolygon.get(a).getY();
					}

					for (int a = 0; a < secondXs.length; a++) {
						secondXs[a] = secondPolygon.get(a).getX();
						secondYs[a] = secondPolygon.get(a).getY();
					}

					polygons.add(new Polygon(firstXs, firstYs));
					polygons.add(new Polygon(secondXs, secondYs));

					double firstC = polygons.get(0).getCircumference();
					double secondC = polygons.get(1).getCircumference();

					double max = (firstC > secondC) ? firstC : secondC;
					if (max < bestCircumference) {
						bestCircumference = max;
						bestPolygons = polygons;
					}
				}
			}
		}
		return bestPolygons;
	}

}
