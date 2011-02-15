package gis2.scenario;

import gis2.Destination;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.undo.AbstractUndoableEdit;

import maps.gml.GMLShape;

/**
   Tool for removing civilians.
*/
public class RemoveCivilianTool extends ShapeTool {
    /**
       Construct a RemoveCivilianTool.
       @param editor The editor instance.
    */
    public RemoveCivilianTool(ScenarioEditor editor) {
        super(editor);
    }

    @Override
    public String getName() {
        return "Remove civilian";
    }

    @Override
    protected boolean shouldHighlight(GMLShape shape) {
        return true;
    }

    @Override
    protected void processClick(GMLShape shape) {
        editor.getScenario().removeCivilian(shape.getID());
        
        //remove the corresponding destination as well bing
        ArrayList<Destination>des=editor.getScenario().getDestination();
        for(int i=0;i<des.size();i++)
        {
        	if(des.get(i).getStart()==shape.getID())
        	{
        		des.remove(i);
        		break;
        	}
        }
        
        editor.setChanged();
        editor.updateOverlays();
        editor.addEdit(new RemoveCivilianEdit(shape.getID()));
    }

    private class RemoveCivilianEdit extends AbstractUndoableEdit {
        private int id;

        public RemoveCivilianEdit(int id) {
            this.id = id;
        }

        @Override
        public void undo() {
            super.undo();
            editor.getScenario().addCivilian(id);
            editor.updateOverlays();
        }

        @Override
        public void redo() {
            super.redo();
            editor.getScenario().removeCivilian(id);
            editor.updateOverlays();
        }
    }
}