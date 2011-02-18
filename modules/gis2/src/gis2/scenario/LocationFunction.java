package gis2.scenario;

import java.util.ArrayList;
import java.util.Iterator;

import maps.gml.GMLShape;
import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;

public abstract class LocationFunction extends AbstractFunction {

	protected LocationFunction(ScenarioEditor editor) {
		super(editor);
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected ArrayList<EntityID>[][] getDistricts(int xLength,int yLength)
	{
		double maxX=editor.getMap().getMaxX();
		double maxY=editor.getMap().getMaxY();
		double minX=editor.getMap().getMinX();
		double minY=editor.getMap().getMinY();
		ArrayList<EntityID> r[][]=new ArrayList[xLength][yLength];
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
	
	protected ArrayList<EntityID> getShapes(double minX,double maxX,double minY,double maxY)
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
