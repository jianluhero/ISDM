package gis2.scenario;

import gis2.Destination;

import java.util.ArrayList;

import rescuecore2.worldmodel.EntityID;

import maps.gml.GMLBuilding;
import maps.gml.GMLShape;

public class AssignDestinationTool extends ShapeTool {

	protected AssignDestinationTool(ScenarioEditor editor) {
		super(editor);
	}

	@Override
	public String getName() {
		return "Assign Destination for Civilians";
	}

	@Override
	protected void processClick(GMLShape shape) {
		ArrayList<Destination>destinations=editor.getScenario().getDestination();
		for(int i=0;i<selectedShapes.size();i++)
		{
			EntityID id=selectedShapes.get(i);
			for(int j=0;j<destinations.size();j++)
			{
				Destination destination=destinations.get(j);
				if(destination.getStart()==id.getValue())
				{
					ArrayList<Integer> ends=new ArrayList<Integer>();
					ends.add(shape.getID());
					destination.setEnds(ends);
				}
			}
		}	        
	   editor.setChanged();
	   editor.updateOverlays();
	   editor.getViewer().clearAllBuildingDecorators();
	   editor.getViewer().clearAllRoadDecorators();
	   editor.getViewer().clearAllSpaceDecorators();
	   selectedShapes.clear();
	   //editor.addEdit(new AssignDestinationEdit(shape.getID()));		
	}

	@Override
	protected boolean shouldHighlight(GMLShape shape) {
		return true;
	}
	
	/*private class AssignDestinationEdit extends AbstractUndoableEdit {
        private int id;

        public AssignDestinationEdit(int id) {
            this.id = id;
        }

        @Override
        public void undo() {
            super.undo();
            editor.getScenario().removeCivilian(id);
            editor.updateOverlays();
        }

        @Override
        public void redo() {
            super.redo();
            editor.getScenario().addCivilian(id);
            editor.updateOverlays();
        }
    }
	*/
}
