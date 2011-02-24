package gis2.scenario;

import gis2.Destination;
import java.util.ArrayList;
import javax.swing.JOptionPane;

import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;

import maps.gml.GMLShape;

/**
 * this class assign delay move feature for civilians
 * can click a shape to assign delay for civilian in this shape
 * or can drag a range of shapes, to assign delay
 * @author Bing Shi
 *
 */
public class AssignDelayTool extends SelectionTool {

	public AssignDelayTool(ScenarioEditor editor) {
		super(editor);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Assign Delay";
	}

	@Override
	protected void processClick(GMLShape shape) {
		ArrayList<Destination> destinations = editor.getScenario()
		.getDestination();
		int id = shape.getID();
		boolean civInShape=false;
		for (int j = 0; j < destinations.size(); j++) {
			Destination destination = destinations.get(j);
			if (destination.getStart() == id) {
				civInShape=true;
			}
		}		
		if(!civInShape)
			return;		
		int d = 0;
		while (true) {
			String delay = JOptionPane.showInputDialog(null,
					"Input the delay time:", "Delay Time",
					JOptionPane.PLAIN_MESSAGE);
			try {
				d = Integer.parseInt(delay);
				break;
			} catch (Exception e) {
				// e.printStackTrace();
				JOptionPane.showMessageDialog(null,
						"input number of int value!");
			}
		}
				
		for (int j = 0; j < destinations.size(); j++) {
			Destination destination = destinations.get(j);
			if (destination.getStart() == id) {
				destination.setDelay(d);
				Logger.debug("civilian in " + destination.getStart()
						+ " delay " + d);
			}
		}
		editor.setChanged();
		editor.updateOverlays();
		editor.getViewer().clearAllBuildingDecorators();
		editor.getViewer().clearAllRoadDecorators();
		editor.getViewer().clearAllSpaceDecorators();
		selectedShapes.clear();
	}

	@Override
	protected boolean shouldHighlight(GMLShape shape) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	protected void processMouseRealsed() {
		// TODO Auto-generated method stub
		int d = 0;

		while (true) {
			String delay = JOptionPane.showInputDialog(null,
					"Input the delay time:", "Delay Time",
					JOptionPane.PLAIN_MESSAGE);
			try {
				d = Integer.parseInt(delay);
				break;
			} catch (Exception e) {
				// e.printStackTrace();
				JOptionPane.showMessageDialog(null,
						"input number of int value!");
			}
		}
		ArrayList<Destination> destinations = editor.getScenario()
				.getDestination();
		for (int i = 0; i < selectedShapes.size(); i++) {
			EntityID id = selectedShapes.get(i);
			for (int j = 0; j < destinations.size(); j++) {
				Destination destination = destinations.get(j);
				if (destination.getStart() == id.getValue()) {
					destination.setDelay(d);
					Logger.debug("civilian in " + destination.getStart()
							+ " delay " + d);
				}
			}
		}
		editor.setChanged();
		editor.updateOverlays();
		editor.getViewer().clearAllBuildingDecorators();
		editor.getViewer().clearAllRoadDecorators();
		editor.getViewer().clearAllSpaceDecorators();
		selectedShapes.clear();
	}
}
