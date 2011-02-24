package gis2.scenario;

import gis2.Destination;
import java.util.ArrayList;
import javax.swing.undo.AbstractUndoableEdit;
import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;
import maps.gml.GMLShape;

/**
 * drag a range of shapes, to assign a destination
 * @author Bing Shi
 *
 */
public class AssignDestinationTool extends SelectionTool {

	ArrayList<ArrayList<Destination>> destinationHis;
	private int index;
	private boolean firstChange;

	protected AssignDestinationTool(ScenarioEditor editor) {
		super(editor);		
		destinationHis = new ArrayList<ArrayList<Destination>>();
		index = -1;
		firstChange = true;
	}

	@Override
	public String getName() {
		return "Assign Destination for Civilians";
	}

	/**
	 * save for redo/undo
	 */
	public void saveDestination() {
		ArrayList<Destination> r = new ArrayList<Destination>();
		if (editor.getScenario() == null)
			Logger.debug("scenario is null, fail to save destination");
		else {
			ArrayList<Destination> d = editor.getScenario().getDestination();
			for (int i = 0; i < d.size(); i++) {
				Destination a = new Destination(d.get(i));
				r.add(a);
			}
			index++;
			destinationHis.add(index,r);
			for(int i=index+1;i<destinationHis.size();i++)
			{
				destinationHis.remove(i);				
			}
		}		
	}
	
	/**
	 * redo/undo
	 * @param index
	 */
	public void restoreDestination(int index) {
		if (destinationHis.size() > index && index >= 0) {
			ArrayList<Destination> d = destinationHis.get(index);
			ArrayList<Destination> r = editor.getScenario().getDestination();
			r.clear();
			for (int i = 0; i < d.size(); i++) {
				Destination a = new Destination(d.get(i));
				r.add(a);
			}
		}
	}

	@Override
	protected void processClick(GMLShape shape) {
		if (firstChange) {
			saveDestination();
			firstChange = false;
		}	
		boolean changed = false;
		ArrayList<Destination> destinations = editor.getScenario()
				.getDestination();

		for (int i = 0; i < selectedShapes.size(); i++) {
			EntityID id = selectedShapes.get(i);
			for (int j = 0; j < destinations.size(); j++) {
				Destination destination = destinations.get(j);
				if (destination.getStart() == id.getValue()) {
					ArrayList<Integer> ends = new ArrayList<Integer>();
					ends.add(shape.getID());
					destination.setEnds(ends);
					changed = true;
					Logger.debug("new destination from " +destination.getStart()+" to " + shape.getID());
				}
			}
		}
		if (changed) {
			saveDestination();
			editor.addEdit(new AssignDestinationEdit());
			editor.setOperation(getName()+": assign a destination");
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

	private class AssignDestinationEdit extends AbstractUndoableEdit {

		public AssignDestinationEdit() {
			super();
		}

		@Override
		public void undo() {
			super.undo();
			if (index > 0) {
				index--;
				restoreDestination(index);
			}
		}

		@Override
		public void redo() {
			super.redo();
			if (index + 1 < destinationHis.size()) {
				index++;
				restoreDestination(index);
			}
		}
	}

	@Override
	protected void processMouseRealsed() {
		// TODO Auto-generated method stub	
	}
}
