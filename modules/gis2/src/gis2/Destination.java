package gis2;

import java.util.ArrayList;

public class Destination {
	private int start;
	
	private ArrayList<Integer> ends;
	
	public Destination(int start)
	{
		this.start=start;
		ends=new ArrayList<Integer>();
	}
	
	public Destination(Destination d)
	{
		this.start=d.start;
		ends=new ArrayList<Integer>();
		for(int i=0;i<d.getEnds().size();i++)
		{
			ends.add(d.getEnds().get(i).intValue());
		}
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public ArrayList<Integer> getEnds() {
		return ends;
	}

	public void setEnds(ArrayList<Integer> ends) {
		this.ends = ends;
	}
}
