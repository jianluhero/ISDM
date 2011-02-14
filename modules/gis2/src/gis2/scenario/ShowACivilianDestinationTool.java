package gis2.scenario;

import maps.gml.GMLShape;

import java.util.HashMap;
import java.util.Iterator;

public class ShowACivilianDestinationTool extends ShapeTool {
	
	 public ShowACivilianDestinationTool(ScenarioEditor editor) {
	        super(editor);
	    }

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void processClick(GMLShape shape) {
		// TODO Auto-generated method stub
		HashMap<Integer, Integer> des=editor.getScenario().getDestination();
		Iterator<Integer>src=des.keySet().iterator();
		int count=0;
		while(src.hasNext())
		{
			Integer start=src.next().intValue();
		}

	}

	@Override
	protected boolean shouldHighlight(GMLShape shape) {
		// TODO Auto-generated method stub
		return false;
	}

}
