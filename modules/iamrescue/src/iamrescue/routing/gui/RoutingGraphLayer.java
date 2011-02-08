package iamrescue.routing.gui;

import iamrescue.execution.command.IPath;
import iamrescue.routing.IRoutingModule;
import iamrescue.routing.costs.BlockCheckerUtil;
import iamrescue.routing.dijkstra.SimpleGraph;
import iamrescue.routing.dijkstra.SimpleGraph.Node;
import iamrescue.util.PositionXY;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javolution.util.FastSet;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.RenderedObject;
import rescuecore2.view.ViewComponent;
import rescuecore2.view.ViewListener;

public class RoutingGraphLayer extends StandardViewLayer implements
		ViewListener {

	private IRoutingModule routing;
	private Area source;
	private IPath path;

	private boolean showLabels = true;
	private boolean showCosts = true;

	public RoutingGraphLayer(IRoutingModule routing) {
		this.routing = routing;
		System.out.println("Size of routing graph: "
				+ routing.getRoutingGraph().getNodes().size() + " nodes.");
	}

	/**
	 * @param showLabels
	 *            the showLabels to set
	 */
	public void setShowLabels(boolean showLabels) {
		this.showLabels = showLabels;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see rescuecore2.view.ViewLayer#getName()
	 */
	@Override
	public String getName() {
		return "Routing Graph";
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
		g.setColor(Color.BLACK);

		SimpleGraph graph = routing.getRoutingGraph();
		List<Node> nodes = graph.getNodes();

		Set<Node> done = new FastSet<Node>();
		for (Node node : nodes) {
			PositionXY xy = routing.getConverter().getSimpleGraphNode(
					node.getID()).getRepresentativePoint();
			int x1 = transform.xToScreen(xy.getX());
			int y1 = transform.yToScreen(xy.getY());

			for (Node neighbour : node.getNeighbours()) {
				if (!done.contains(neighbour)) {
					PositionXY xy2 = routing.getConverter().getSimpleGraphNode(
							neighbour.getID()).getRepresentativePoint();
					int x2 = transform.xToScreen(xy2.getX());
					int y2 = transform.yToScreen(xy2.getY());

					// boolean blocked = false;
					double costToNeighbour = node.getCostToNeighbour(neighbour);
					if (costToNeighbour >= Double.POSITIVE_INFINITY) {
						g.setColor(Color.RED);
						// blocked = true;
					} else {
						g.setColor(Color.BLACK);
					}

					g.drawLine(x1, y1, x2, y2);

					if (showCosts) {
						g.drawString("" + (int) Math.round(costToNeighbour),
								(x1 + x2) / 2, (y1 + y2) / 2);
					}

					if (BlockCheckerUtil.USE_3_LINES) {
						Line2D line = new Line2D(xy.toPoint2D(), xy2
								.toPoint2D());
						List<Line2D> parallelLines = BlockCheckerUtil
								.generateParallelLines(
										line,
										BlockCheckerUtil.SAFETY_MARGIN_EITHER_SIDE,
										BlockCheckerUtil.FORWARD_MARGIN, 1);
						Color c = g.getColor();
						// if (blocked) {
						g.setColor(new Color(c.getRed(), c.getGreen(), c
								.getBlue(), 40));
						// }
						// g.setColor(g.getColor().brighter().brighter());
						for (Line2D parallelLine : parallelLines) {
							int x1p = transform.xToScreen(parallelLine
									.getOrigin().getX());
							int y1p = transform.yToScreen(parallelLine
									.getOrigin().getY());
							int x2p = transform.xToScreen(parallelLine
									.getEndPoint().getX());
							int y2p = transform.yToScreen(parallelLine
									.getEndPoint().getY());
							g.drawLine(x1p, y1p, x2p, y2p);
						}
					}
				}
			}
			g.setColor(Color.BLACK);
			if (showLabels) {
				g.drawString("N " + node.getID(), x1, y1);
				// + ":"
				// + routing.getConverter().getSimpleGraphNode(
				// node.getID()).getRepresentativePoint(), x1, y1);
			}

			done.add(node);
		}
		if (source != null) {
			g.setColor(Color.ORANGE);
			int x = transform.xToScreen(source.getX());
			int y = transform.yToScreen(source.getY());
			g.drawOval(x - 5, y - 5, 10, 10);

		}
		if (path != null && path.isValid()) {
			g.setColor(Color.RED);
			PositionXY last = path.getXYPath().get(0);
			for (int i = 1; i < path.getXYPath().size(); i++) {
				PositionXY next = path.getXYPath().get(i);
				int x1 = transform.xToScreen(next.getX());
				int y1 = transform.yToScreen(next.getY());
				int x2 = transform.xToScreen(last.getX());
				int y2 = transform.yToScreen(last.getY());
				last = next;
				g.drawLine(x1, y1, x2, y2);
			}
		}

		return new ArrayList<RenderedObject>();

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
				System.out.println("Routing Source : " + area);
				if (source == null) {
					source = area;
					view.repaint();
				} else {
					System.out
							.println("Routing from " + source + " to " + area);
					long start = System.nanoTime();
					path = routing.findShortestPath(source.getID(), Collections
							.singleton(area.getID()));
					long end = System.nanoTime();
					System.out.println("Found after " + (end - start)
							/ 1000000.0 + "ms: " + path);
					source = null;
					view.repaint();
				}
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
}