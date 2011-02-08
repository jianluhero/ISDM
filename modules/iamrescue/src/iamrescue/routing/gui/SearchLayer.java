package iamrescue.routing.gui;

import iamrescue.belief.IAMWorldModel;
import iamrescue.routing.dijkstra.SimpleGraph;
import iamrescue.routing.dijkstra.SimpleGraph.Node;
import iamrescue.util.PositionXY;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javolution.util.FastSet;
import rescuecore2.misc.Pair;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.RenderedObject;
import rescuecore2.view.ViewComponent;
import rescuecore2.view.ViewListener;
import rescuecore2.worldmodel.EntityID;

public class SearchLayer extends StandardViewLayer implements ViewListener {

	private IAMWorldModel worldModel;

	public SearchLayer(IAMWorldModel worldModel) {
		this.worldModel = worldModel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see rescuecore2.view.ViewLayer#getName()
	 */
	@Override
	public String getName() {
		return "Search Graph";
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

		Collection<EntityID> unknownBuildings = worldModel
				.getUnknownBuildings();

		Collection<EntityID> safeUnsearchedBuildings = worldModel
				.getSafeUnsearchedBuildings();

		g.setColor(Color.YELLOW);

		for (EntityID building : unknownBuildings) {
			StandardEntity entity = world.getEntity(building);
			Pair<Integer, Integer> location = entity.getLocation(worldModel);
			int x = transform.xToScreen(location.first());
			int y = transform.yToScreen(location.second());
			g.fillOval(x, y, 15, 15);
		}

		g.setColor(Color.CYAN);

		for (EntityID building : safeUnsearchedBuildings) {
			StandardEntity entity = world.getEntity(building);
			Pair<Integer, Integer> location = entity.getLocation(worldModel);
			int x = transform.xToScreen(location.first());
			int y = transform.yToScreen(location.second());
			g.fillOval(x, y, 10, 10);
		}

		return new ArrayList<RenderedObject>();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * rescuecore2.view.ViewListener#objectsClicked(rescuecore2.view.ViewComponent
	 * , java.util.List)
	 */
	@Override
	public void objectsClicked(ViewComponent view, List<RenderedObject> objects) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * rescuecore2.view.ViewListener#objectsRollover(rescuecore2.view.ViewComponent
	 * , java.util.List)
	 */
	@Override
	public void objectsRollover(ViewComponent view, List<RenderedObject> objects) {
		// TODO Auto-generated method stub

	}

}