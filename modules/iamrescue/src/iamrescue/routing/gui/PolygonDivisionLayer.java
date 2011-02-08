package iamrescue.routing.gui;

import iamrescue.belief.IAMWorldModel;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import maps.util.Polygon;
import maps.util.PolygonSplitter;
import maps.util.Triangle;
import maps.util.Triangulation;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.RenderedObject;
import rescuecore2.view.ViewComponent;
import rescuecore2.view.ViewListener;

public class PolygonDivisionLayer extends StandardViewLayer implements
		ViewListener {

	private IAMWorldModel worldModel;

	private Area clicked = null;

	private Polygon[] splitPolygons;
	private Polygon[] triangulatedPolygons;

	private double maxNodes = 30;// 150000;
	private double maxCircumference = 20000;

	private double minDegree = 0;

	public PolygonDivisionLayer() {
		// this.worldModel = worldModel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see rescuecore2.view.ViewLayer#getName()
	 */
	@Override
	public String getName() {
		return "Polygon Division";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see rescuecore2.view.ViewLayer#render(java.awt.Graphics2D,
	 * rescuecore2.misc.gui.ScreenTransform, int, int)
	 */
	@Override
	public Collection<RenderedObject> render(Graphics2D g,
			ScreenTransform transform, int width, int height) {

		if (clicked != null) {

			if (splitPolygons == null) {
				createPolygons();
			}

			g.setColor(Color.BLACK);

			if (triangulatedPolygons != null) {

				for (Polygon polygon : triangulatedPolygons) {
					drawPolygon(polygon, transform, g);
				}
			}

			g.setColor(Color.RED);

			for (Polygon polygon : splitPolygons) {
				drawPolygon(polygon, transform, g);
			}

			/*
			 * int cutDistance = 20000;
			 * 
			 * List<Edge> edges = clicked.getEdges();
			 * 
			 * int[] apexList = clicked.getApexList();
			 * 
			 * int length = 0;
			 * 
			 * for (int i = 0; i < apexList.length - 2; i = i + 2) { double midX
			 * = (apexList[i] + apexList[i + 2]) / 2; double midY = (apexList[i
			 * + 1] + apexList[i + 3]) / 2; double directionX = -apexList[i + 1]
			 * + apexList[i + 3]; double directionY = apexList[i] - apexList[i +
			 * 2];
			 * 
			 * length += Math.sqrt(directionX * directionX + directionY
			 * directionY);
			 * 
			 * if (length > cutDistance) { length = 0; Line2D orthogonal = new
			 * Line2D(midX, midY, directionX, directionY);
			 * 
			 * int negatives = 0; int positives = 0;
			 * 
			 * double lowestNegative = Double.MAX_VALUE; double lowestPositive =
			 * Double.MAX_VALUE;
			 * 
			 * for (Edge edge : edges) { Line2D otherLine = edge.getLine(); if
			 * (edge.getStartX() == apexList[i] || edge.getEndX() ==
			 * apexList[i]) { // Ignore same edge. continue; } double
			 * intersection = orthogonal .getIntersection(otherLine); if
			 * (intersection != Double.NaN) {
			 * 
			 * double otherIntersection = otherLine
			 * .getIntersection(orthogonal); if (otherIntersection >= 0 &&
			 * otherIntersection <= 1) {
			 * 
			 * g.setColor(Color.GREEN); g.drawOval(transform.xToScreen(midX +
			 * intersection * directionX), transform .yToScreen(midY +
			 * intersection directionY), 5, 5); g.drawString("" + intersection,
			 * transform .xToScreen(midX + intersection directionX), transform
			 * .yToScreen(midY + intersection directionY));
			 * g.setColor(Color.BLACK);
			 * 
			 * if (intersection < 0) { negatives++; intersection *= -1; if
			 * (intersection < lowestNegative) { lowestNegative = intersection;
			 * } } else if (intersection > 0) { positives++; if (intersection <
			 * lowestPositive) { lowestPositive = intersection; } } else {
			 * System.out.println("Something went wrong. " +
			 * "Intersection is at " + midX + "," + midY); } } } else { //
			 * Parallel. Check if they intersect double otherX =
			 * otherLine.getOrigin().getX(); double otherY =
			 * otherLine.getOrigin().getY();
			 * 
			 * double scalar = (otherX - midX) / directionX; if (Math.abs(scalar
			 * * directionY + midY - otherY) < 0.000000001) { // Yes, assume
			 * they intersect if (scalar < 0) { negatives++; } else if (scalar >
			 * 1) { positives++; } else {
			 * System.out.println("Something went wrong " +
			 * "with parallel lines. " + "Intersection is at " + midX + "," +
			 * midY);
			 * 
			 * } } } } double scalar = 0; if (negatives % 2 != 0) { // Use
			 * intersection of negative side scalar = - lowestNegative; } else
			 * if (positives % 2 != 0) { scalar = lowestPositive; } else {
			 * System.out.println("Something went wrong. " +
			 * "Same number of positives and negatives"); g.setColor(Color.RED);
			 * g.drawOval(transform.xToScreen(midX), transform .yToScreen(midY),
			 * 5, 5); g.setColor(Color.BLACK); continue; }
			 * g.drawLine(transform.xToScreen(midX), transform .yToScreen(midY),
			 * transform.xToScreen(midX + directionX * scalar),
			 * transform.yToScreen(midY + directionY * scalar)); } }
			 */
		}

		/*
		 * 
		 * 
		 * 
		 * 
		 * g.setColor(Color.BLACK);
		 * 
		 * SimpleGraph graph = routing.getGraph(); List<Node> nodes =
		 * graph.getNodes();
		 * 
		 * Set<Node> done = new FastSet<Node>(); for (Node node : nodes) {
		 * PositionXY xy = routing.getConverter().getSimpleGraphNode(
		 * node.getID()).getRepresentativePoint(); int x1 =
		 * transform.xToScreen(xy.getX()); int y1 =
		 * transform.yToScreen(xy.getY());
		 * 
		 * for (Node neighbour : node.getNeighbours()) { if
		 * (!done.contains(neighbour)) { PositionXY xy2 =
		 * routing.getConverter().getSimpleGraphNode(
		 * neighbour.getID()).getRepresentativePoint(); int x2 =
		 * transform.xToScreen(xy2.getX()); int y2 =
		 * transform.yToScreen(xy2.getY());
		 * 
		 * g.drawLine(x1, y1, x2, y2); } } g.drawString("N " + node.getID(), x1,
		 * y1); done.add(node); } if (path != null) { g.setColor(Color.RED);
		 * PositionXY last = path.getXYPath().get(0); for (int i = 1; i <
		 * path.getXYPath().size(); i++) { PositionXY next =
		 * path.getXYPath().get(i); int x1 = transform.xToScreen(next.getX());
		 * int y1 = transform.yToScreen(next.getY()); int x2 =
		 * transform.xToScreen(last.getX()); int y2 =
		 * transform.yToScreen(last.getY()); last = next; g.drawLine(x1, y1, x2,
		 * y2); } }
		 */
		return new ArrayList<RenderedObject>();

	}

	/**
	 * 
	 */
	private void createPolygons() {
		int[] apexList = clicked.getApexList();

		double[] xs = new double[apexList.length / 2];
		double[] ys = new double[apexList.length / 2];

		for (int i = 0; i < apexList.length; i = i + 2) {
			xs[xs.length - i / 2 - 1] = apexList[i];
			ys[xs.length - i / 2 - 1] = apexList[i + 1];
		}

		Polygon originalPolygon = new Polygon(xs, ys);

		// originalPolygon = PolygonSplitter.reduceEdges(originalPolygon,
		// minDegree);

		splitPolygons = PolygonSplitter.splitPolygon(originalPolygon, maxNodes,
				maxCircumference);

		// splitPolygons = new Polygon[] { splitPolygons[(int)
		// (splitPolygons.length * Math
		// .random())] };

		// splitPolygons = new Polygon[] { originalPolygon };

		List<Polygon> triangulated = new ArrayList<Polygon>();

		for (Polygon p : splitPolygons) {

			Triangle[] triangulatedPolygon = Triangulation.triangulatePolygon(
					p.x, p.y, p.x.length);

			if (triangulatedPolygon != null) {

				Polygon[] polygons = Triangulation.polygonizeTriangles(
						triangulatedPolygon, minDegree);
				for (Polygon triP : polygons) {
					triangulated.add(triP);
				}
			}
		}

		triangulatedPolygons = triangulated.toArray(new Polygon[0]);

	}

	public void setMaxNodes(double maxNodes) {

		this.maxNodes = maxNodes;
		splitPolygons = null;
		component.repaint();
	}

	public void setMaxCircumference(double maxCircumference) {
		this.maxCircumference = maxCircumference;
	}

	public void setMinDegree(double minDegree) {

		this.minDegree = minDegree;
		splitPolygons = null;
		component.repaint();
	}

	/**
	 * @return the maxCircumference
	 */
	public double getMaxNodes() {
		return maxNodes;
	}

	public double getMaxCircumference() {
		return maxCircumference;
	}

	/**
	 * @return the minDegree
	 */
	public double getMinDegree() {
		return minDegree;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seerescuecore2.view.ViewListener#objectsClicked(rescuecore2.view.
	 * ViewComponent, java.util.List)
	 */
	@Override
	public void objectsClicked(ViewComponent view, List<RenderedObject> objects) {
		for (RenderedObject obj : objects) {
			if (obj.getObject() instanceof Area) {
				Area area = (Area) obj.getObject();
				clicked = area;
				splitPolygons = null;
				triangulatedPolygons = null;
				view.repaint();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seerescuecore2.view.ViewListener#objectsRollover(rescuecore2.view.
	 * ViewComponent, java.util.List)
	 */
	@Override
	public void objectsRollover(ViewComponent view, List<RenderedObject> objects) {
		// TODO Auto-generated method stub

	}

	private void drawPolygon(Polygon polygon, ScreenTransform transform,
			Graphics g) {
		double[] pxs = polygon.x;
		double[] pys = polygon.y;
		for (int i = 0; i < pxs.length - 1; i++) {
			g.drawLine(transform.xToScreen(pxs[i]),
					transform.yToScreen(pys[i]), transform
							.xToScreen(pxs[i + 1]), transform
							.yToScreen(pys[i + 1]));

			int lower = (i == 0) ? (pxs.length - 1) : (i - 1);
			int middle = i;
			int upper = (i == pxs.length - 1) ? (0) : (i + 1);
			Line2D line1 = new Line2D(new Point2D(pxs[lower], pys[lower]),
					new Point2D(pxs[middle], pys[middle]));
			Line2D line2 = new Line2D(new Point2D(pxs[middle], pys[middle]),
					new Point2D(pxs[upper], pys[upper]));

			// double angle = GeometryTools2D
			// .getRightAngleBetweenLines(line1, line2);
			// g
			// .drawString("" + Math.round(angle), transform
			// .xToScreen(pxs[i]), transform
			// .yToScreen(pys[i]));

		}
		g.drawLine(transform.xToScreen(pxs[pxs.length - 1]), transform
				.yToScreen(pys[pxs.length - 1]), transform.xToScreen(pxs[0]),
				transform.yToScreen(pys[0]));
	}
}