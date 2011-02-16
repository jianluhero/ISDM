package gis2.scenario;

import gis2.Destination;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rescuecore2.worldmodel.EntityID;

import maps.gml.GMLShape;

public class AssignDestinationFromConfigFile extends AbstractFunction {

	protected AssignDestinationFromConfigFile(ScenarioEditor editor) {
		super(editor);
	}

	@Override
	public String getName() {
		return "Assign Destination from Config File";
	}

	/**
	 * from config file, configure the destination
	 */
	@Override
	public void execute() {
		ArrayList<Destination>destinations=editor.getScenario().getDestination();
		
		double maxX=editor.getMap().getMaxX();
		double maxY=editor.getMap().getMaxY();
		double minX=editor.getMap().getMinX();
		double minY=editor.getMap().getMinY();
		
		//TODO: how many districts we get according to config file
		double xLength=10.0;
		double yLength=10.0;
		
		
		List<GMLShape> all = new ArrayList<GMLShape>(editor.getMap().getAllShapes());
		Random random=new Random();
		for(int i=0;i<destinations.size();i++)
		{
			Destination d=destinations.get(i);
			d.getEnds().clear();			
			int des=all.get(random.nextInt(all.size())).getID();
			d.getEnds().add(des);
		}	
	}
	
	public ArrayList<ArrayList<EntityID>> getDistricts(int maxX,int minX,int maxY,int minY)
	{
		return null;
	}

}
