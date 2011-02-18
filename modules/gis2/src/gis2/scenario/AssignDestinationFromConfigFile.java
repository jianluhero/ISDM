package gis2.scenario;

import gis2.Destination;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;

import maps.gml.GMLBuilding;
import maps.gml.GMLRoad;
import maps.gml.GMLShape;
import maps.gml.GMLSpace;

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
		int xLength=10;
		int yLength=10;
		
		double prob[][]=new double[xLength][yLength];
		
		//TODO: for region without shapes, assign 0 probability
		//TODO: generate location file and destination file, relate them with time
		
		
		List<GMLShape> all = new ArrayList<GMLShape>(editor.getMap().getAllShapes());
		Random random=new Random();
		//ArrayList<ArrayList<EntityID>> disArrayList=getDistricts(maxX, minX, maxY, minY, xLength, yLength);
		
		for(int i=0;i<xLength;i++)
		{
			for(int j=0;j<yLength;j++)
			{
		//		prob[i][j]
			}
		}
		
	}
	
	public ArrayList<EntityID>[][] getDistricts(double maxX,double minX,double maxY,double minY,int xLength,int yLength)
	{
		ArrayList r[][]=new ArrayList[xLength][yLength];
		double x=Math.round((maxX-minX)/xLength);
		double y=Math.round((maxY-minY)/yLength);
		
		for(int i=0;i<xLength;i++)
		{	
			double x1=minX+x*i;
			double x2=minX+x*(i+1);
			for(int j=0;j<yLength;j++)
			{				
				double y1=minY+y*j;
				double y2=minY+y*(j+1);
				r[i][j]=getShapes(x1,x2,y1,y2);
			}
		}
		return r;
	}
	
	public ArrayList<EntityID> getShapes(double minX,double maxX,double minY,double maxY)
	{
		ArrayList<EntityID> r=new ArrayList<EntityID>();
		Iterator<GMLShape> shapes = editor.getMap().getAllShapes().iterator();
		while (shapes.hasNext()) {
			GMLShape shape = shapes.next();
			if (shape.getCentreX() >= minX && shape.getCentreX() <= maxX
					&& shape.getCentreY() >= minY && shape.getCentreY() <= maxY) {
				r.add(new EntityID(shape.getID()));
				Logger.debug("shape " + shape.getID() + " is in range: "+maxX+" "+minX+" "+maxY+" "+minY);
			}
				
		}
		return r;
	}

}
