/**
 * 
 */
package iamrescue.belief.gui;

import rescuecore2.standard.view.AreaNeighboursLayer;
import rescuecore2.standard.view.BuildingIconLayer;
import rescuecore2.standard.view.PositionHistoryLayer;
import rescuecore2.standard.view.RoadLayer;
import rescuecore2.standard.view.StandardWorldModelViewer;

/**
 * @author Sebastian
 * 
 */
public class IAMWorldModelViewer extends StandardWorldModelViewer {

	@Override
	public void addDefaultLayers() {
		addLayer(new EnhancedBuildingLayer());
		addLayer(new RoadLayer());
		addLayer(new AreaNeighboursLayer());
		addLayer(new EnhancedRoadBlockageLayer());
		addLayer(new BuildingIconLayer());
		addLayer(new EnhancedHumanLayer());
		// addLayer(new CommandLayer());
		addLayer(new PositionHistoryLayer());
	}
}
