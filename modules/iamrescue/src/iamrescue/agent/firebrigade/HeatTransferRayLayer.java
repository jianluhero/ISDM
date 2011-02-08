/**
 * 
 */
package iamrescue.agent.firebrigade;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;

import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.RenderedObject;

public class HeatTransferRayLayer extends StandardViewLayer {

	private HeatTransferGraph graph;

	public HeatTransferRayLayer(HeatTransferGraph graph) {
		this.graph = graph;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see rescuecore2.view.ViewLayer#getName()
	 */
	@Override
	public String getName() {
		return "Fire Model Rays";
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

		Collection<Line2D> rays = graph.getRays();

		g.setColor(Color.black);

		for (Line2D line : rays) {
			int x1 = transform.xToScreen(line.getOrigin().getX());
			int y1 = transform.yToScreen(line.getOrigin().getY());
			int x2 = transform.xToScreen(line.getEndPoint().getX());
			int y2 = transform.yToScreen(line.getEndPoint().getY());

			g.drawLine(x1, y1, x2, y2);
		}

		return new ArrayList<RenderedObject>();
	}
}