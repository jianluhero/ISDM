/**
 * 
 */
package iamrescue.belief.gui;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;

import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.view.HumanLayer;

/**
 * @author Sebastian
 * 
 */
public class EnhancedHumanLayer extends HumanLayer {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * rescuecore2.standard.view.HumanLayer#render(rescuecore2.standard.entities
	 * .Human, java.awt.Graphics2D, rescuecore2.misc.gui.ScreenTransform)
	 */
	@Override
	public Shape render(Human h, Graphics2D g, ScreenTransform t) {
		if (h.isXDefined() && h.isYDefined()) {
			if (!h.isHPDefined()) {
				h = (Human) h.copy();
				h.setHP(10000);
			}
			return super.render(h, g, t);
		} else {
			return new Line2D.Double(0, 0, 0, 0);
		}
	}
}
