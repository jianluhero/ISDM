package gis2.scenario;

import gis2.Destination;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.undo.AbstractUndoableEdit;

import maps.gml.GMLShape;

/**
   Tool for placing civilians.
*/
public class PlaceCivilianTool extends ShapeTool {
    /**
       Construct a PlaceCivilianTool.
       @param editor The editor instance.
    */
    public PlaceCivilianTool(ScenarioEditor editor) {
        super(editor);
    }

    @Override
    public String getName() {
        return "Place civilian";
    }

    @Override
    protected boolean shouldHighlight(GMLShape shape) {
        return true;
    }

    @Override
    protected void processClick(GMLShape shape) {
    	editor.setOperation(getName());
    	
        editor.getScenario().addCivilian(shape.getID());
        //set refuge as default destination bing
        ArrayList<Destination>des=editor.getScenario().getDestination();
        ArrayList<Integer> refuge=new ArrayList<Integer>();
        Iterator<Integer> temp=editor.getScenario().getRefuges().iterator();
        while(temp.hasNext())
        {
        	refuge.add(temp.next());
        }
        if(refuge.size()==0)//no refuge, no move
        {
        	refuge.add(new Integer(shape.getID()));
        }
        Destination d=new Destination(shape.getID());
        d.setEnds(refuge);
        des.add(d);
        d.setDelay(0);
        
        editor.setChanged();
        editor.updateOverlays();
        editor.addEdit(new AddCivilianEdit(shape.getID()));
    }

    private class AddCivilianEdit extends AbstractUndoableEdit {
        private int id;

        public AddCivilianEdit(int id) {
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
}