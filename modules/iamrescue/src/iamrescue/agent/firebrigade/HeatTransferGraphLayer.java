/**
 * 
 */
package iamrescue.agent.firebrigade;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.RenderedObject;

public class HeatTransferGraphLayer extends StandardViewLayer {

	private static final Color EDGE_COLOR = new Color(160, 52, 52, 255);
	private static final float MAX_LINE_WIDTH = 20;
	private HeatTransferGraph graph;
	private double maxCoef = Integer.MIN_VALUE;

	public HeatTransferGraphLayer(HeatTransferGraph graph) {
		this.graph = graph;

		Collection<Building> buildings = graph.getBuildings();

		for (Building building : buildings) {
			Map<Building, Double> coef = graph.getHeatTransferRays(building);

			for (Building neigh : coef.keySet()) {
				if (neigh.equals(building))
					continue;

				maxCoef = Math.max(maxCoef, coef.get(neigh));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see rescuecore2.view.ViewLayer#getName()
	 */
	@Override
	public String getName() {
		return "Fire Model Graph";
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

		Collection<Building> vertices = graph.getGraph().getVertices();
		Set<Building> done = new HashSet<Building>();

		for (Building building : vertices) {
			done.add(building);

			int x1 = transform.xToScreen(building.getX());
			int y1 = transform.yToScreen(building.getY());

			for (Building neighbour : graph.getNeighbouringBuildings(building)) {
				if (done.contains(neighbour))
					continue;

				int x2 = transform.xToScreen(neighbour.getX());
				int y2 = transform.yToScreen(neighbour.getY());

				int halfWayX = (x1 + x2) / 2;
				int halfWayY = (y1 + y2) / 2;

				Stroke stroke = g.getStroke();
				Color c = g.getColor();
				g.setColor(EDGE_COLOR);

				Double from = graph.getHeatTransferRays(building)
						.get(neighbour);
				Double to = graph.getHeatTransferRays(neighbour).get(building);

				from = from == null ? 0 : from;
				to = to == null ? 0 : to;

				float strokeWidthFrom = (float) ((from / maxCoef) * MAX_LINE_WIDTH);
				float strokeWidthTo = (float) ((to / maxCoef) * MAX_LINE_WIDTH);

				if (strokeWidthFrom >= 1.0) {
					g.setStroke(new BasicStroke(strokeWidthFrom,
							BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
					g.drawLine(x1, y1, halfWayX, halfWayY);
				}

				if (strokeWidthTo >= 1.0) {
					g.setStroke(new BasicStroke(strokeWidthTo,
							BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
					g.drawLine(halfWayX, halfWayY, x2, y2);
				}

				g.setStroke(stroke);
				g.setColor(c);
			}
			// g.drawString("N " + building.getID(), x1, y1);
		}

		return new ArrayList<RenderedObject>();
	}
}