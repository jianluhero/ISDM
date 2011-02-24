package gis2.scenario;

import java.util.ArrayList;
import java.util.Iterator;

import maps.gml.GMLShape;
import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;

/**
 * this class help to get shapes in a specific region
 * @author Bing Shi
 *
 */
public abstract class RegionFunction extends AbstractFunction {

	protected RegionFunction(ScenarioEditor editor) {
		super(editor);
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * get shapes in each region when divide the map to xLength X yLength regions equally
	 * @param xLength
	 * @param yLength
	 * @return
	 */
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
	
	/**
	 * get shapes under the specific region
	 * @param minX
	 * @param maxX
	 * @param minY
	 * @param maxY
	 * @return
	 */
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
	
	/**
	 * check whether the sum of distribution is 1
	 * @return
	 */
	public boolean isOne(double dis[][])
	{
		double sum=0;
		for(int i=0;i<dis.length;i++)
			for(int j=0;j<dis[i].length;j++)
				sum+=dis[i][j];
		if(Math.abs(sum-1)<0.000001)
			return true;
		else {
			return false;
		}
	}
}
