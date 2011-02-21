package gis2.scenario;

import gis2.Destination;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;

import maps.gml.GMLBuilding;
import maps.gml.GMLRoad;
import maps.gml.GMLShape;
import maps.gml.GMLSpace;

public class AssignCivDestinationFromDistribution extends RegionFunction {

	private static final String file = "destination";

	private double destinationDis[][];
	private int xLength;
	private int yLength;
	private Random random;

	public AssignCivDestinationFromDistribution(ScenarioEditor editor) {
		super(editor);
		random = new Random();
	}

	/**
	 * load destination distribution from file
	 */
	public void load() {
		File f = editor.getBaseDir();
		File location = new File(f, file);
		if (location != null) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(
						location));
				String l = reader.readLine();
				if (l != null) {
					StringTokenizer st = new StringTokenizer(l, ",");
					xLength = Integer.valueOf(st.nextToken());
					yLength = Integer.valueOf(st.nextToken());
					destinationDis = new double[xLength][yLength];
					String dis = reader.readLine();
					if (dis != null) {
						StringTokenizer st1 = new StringTokenizer(dis, ",");
						for (int i = 0; i < xLength; i++) {
							for (int j = 0; j < yLength; j++) {
								destinationDis[i][j] = Double.valueOf(st1
										.nextToken());
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void execute() {
		load();
		if (destinationDis.length == 0)
			return;
		ArrayList<EntityID>[][] districts = getDistricts(xLength, yLength);
		Iterator<Integer>civs=editor.getScenario().getCivilians().iterator();
		ArrayList<Destination>destinations=editor.getScenario().getDestination();
		List<GMLShape> all = new ArrayList<GMLShape>(editor.getMap().getAllShapes());
		Random random=new Random();
		destinations.clear();
		while (civs.hasNext())
		{
			Destination d=new Destination(civs.next());			
			double r = random.nextDouble();
			double sum = 0;
			for (int m = 0; m < destinationDis.length; m++) {
				for (int n = 0; n < destinationDis[m].length; n++) {
					ArrayList<EntityID> temp = districts[m][n];
					if (temp.size() == 0) {
						if (destinationDis[m][n] > 0.0000001) {
							Logger.error("civilian's destination in a region without shape");
						}
					} else {
						sum += destinationDis[m][n];
						if (r < sum)// civ destination in region m:n
						{
							int des=temp.get(random.nextInt(temp.size())).getValue();
							d.getEnds().add(des);
							r=Double.MAX_VALUE;
							Logger.debug("assign civ from "+d.getStart()+" to "+des);
						}
					}
				}
			}			
			destinations.add(d);
		}
		
		editor.setChanged();
		editor.updateOverlays();
		editor.setOperation(getName());
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Assign Civ. Des. From Dis.";
	}

}
