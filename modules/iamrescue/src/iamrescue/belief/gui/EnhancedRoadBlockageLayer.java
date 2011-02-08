/**
 * 
 */
package iamrescue.belief.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;

import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.view.RoadBlockageLayer;

/**
 * @author Sebastian
 * 
 */
public class EnhancedRoadBlockageLayer extends RoadBlockageLayer {
	private static final int UNKNOWN_WIDTH = 4;
	private static final int UNKNOWN_HEIGHT = 4;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * rescuecore2.standard.view.RoadBlockageLayer#render(rescuecore2.standard
	 * .entities.Blockade, java.awt.Graphics2D,
	 * rescuecore2.misc.gui.ScreenTransform)
	 */
	@Override
	public Shape render(Blockade b, Graphics2D g, ScreenTransform t) {
		if (b.isApexesDefined()) {
			return super.render(b, g, t);
		} else {
			if (b.isXDefined() && b.isYDefined()) {
				int x = b.getX();
				int y = b.getY();
				int[] xs = new int[4];
				int[] ys = new int[4];
				xs[0] = t.xToScreen(x - UNKNOWN_WIDTH / 2);
				xs[1] = t.xToScreen(x + UNKNOWN_WIDTH / 2);
				xs[2] = t.xToScreen(x + UNKNOWN_WIDTH / 2);
				xs[3] = t.xToScreen(x - UNKNOWN_WIDTH / 2);
				ys[0] = t.yToScreen(y - UNKNOWN_HEIGHT / 2);
				ys[1] = t.yToScreen(y - UNKNOWN_HEIGHT / 2);
				ys[2] = t.yToScreen(y + UNKNOWN_HEIGHT / 2);
				ys[3] = t.yToScreen(y + UNKNOWN_HEIGHT / 2);
				Polygon shape = new Polygon(xs, ys, 4);
				g.setColor(Color.BLACK.brighter());
				g.fill(shape);
				return shape;
			} else {
				return null;
			}
		}
	}
}
