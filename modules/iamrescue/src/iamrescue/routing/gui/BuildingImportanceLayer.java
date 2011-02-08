package iamrescue.routing.gui;

import iamrescue.agent.firebrigade.FastFirePredictor;
import iamrescue.agent.firebrigade.FastFireSite;
import iamrescue.agent.firebrigade.FastImportanceModel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastSet;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.RenderedObject;
import rescuecore2.view.ViewComponent;
import rescuecore2.view.ViewListener;
import rescuecore2.worldmodel.EntityID;

public class BuildingImportanceLayer extends StandardViewLayer implements
		ViewListener {

	private static final Color[] colours = new Color[] { Color.BLACK,
			Color.BLUE, Color.RED, Color.GREEN, Color.CYAN, Color.PINK,
			Color.MAGENTA };

	private FastFirePredictor firePredictor;

	private NumberFormat format;

	public BuildingImportanceLayer(FastFirePredictor firePredictor) {
		this.firePredictor = firePredictor;
		format = NumberFormat.getInstance();
		format.setMaximumFractionDigits(4);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see rescuecore2.view.ViewLayer#getName()
	 */
	@Override
	public String getName() {
		return "Importance Model";
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

		List<FastFireSite> fireSites = firePredictor.getFireSites();
		FastImportanceModel importanceModel = firePredictor
				.getImportanceModel();

		Set<Building> fringe = new FastSet<Building>();

		for (FastFireSite fireSite : fireSites) {
			fringe.addAll(fireSite.getFringe());
		}

		int siteCounter = 0;
		// System.out.println("There are " + fireSites.size() + " fire sites.");

		for (FastFireSite fireSite : fireSites) {
			g.setColor(colours[siteCounter % colours.length]);
			siteCounter++;

			Map<EntityID, Building> buildingsOnFire = fireSite
					.getBuildingsOnFire();

			Building highest = null;
			double highestImportance = Double.MIN_VALUE;
			for (Building building : buildingsOnFire.values()) {
				/*
				 * if (!fringe.contains(building)) { continue; }
				 */
				// System.out.print(building.getFullDescription());

				double contextImportance = firePredictor.calculateImportance(building);
				if (contextImportance > highestImportance
						&& fringe.contains(building)) {
					highestImportance = contextImportance;
					highest = building;
				}
				double realImportance = firePredictor
						.calculateImportance(building);
				int x1 = transform.xToScreen(building.getX());
				int y1 = transform.yToScreen(building.getY());
				g.drawString("FS"
						+ siteCounter
						+ ": "
						+ realImportance
						+ " ["
						+ "ci:"
						+ firePredictor.calculateContextDependentImportance(building)
						+ ",i:"
						+ firePredictor.getImportanceModel().getImportance(
								building) + "]", x1, y1);
				// g.drawOval(x1, y1 - 20, 10, 10);
			}
			if (highest != null) {
				g.setColor(Color.RED);
				int x1 = transform.xToScreen(highest.getX());
				int y1 = transform.yToScreen(highest.getY());
				g.drawOval(x1 - 10, y1 - 10, 20, 20);
				// System.out.println("\nHighest importance: "
				// + highest.getFullDescription() + ", importance: "
				// + highestImportance);
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
		/*
		 * for (RenderedObject obj : objects) { if (obj.getObject() instanceof
		 * Building) { System.out.print } }
		 */}

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