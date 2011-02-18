package gis2.scenario;

import gis2.Destination;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import maps.gml.GMLShape;

import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;

public class AssignCivilianFromLocationDistribution extends LocationFunction {

	private static final String file = "location";

	private double locationDis[][];
	private int xLength;
	private int yLength;
	private Random random;

	public AssignCivilianFromLocationDistribution(ScenarioEditor editor) {
		super(editor);
		random = new Random();
	}

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
					locationDis = new double[xLength][yLength];
					String dis = reader.readLine();
					if (dis != null) {
						StringTokenizer st1 = new StringTokenizer(dis, ",");
						for (int i = 0; i < xLength; i++) {
							for (int j = 0; j < yLength; j++) {
								locationDis[i][j] = Double.valueOf(st1
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
		if (locationDis.length == 0)
			return;
		Collection<Integer> civLocation = new ArrayList<Integer>();
		ArrayList<EntityID>[][] districts = getDistricts(xLength, yLength);
		for (int i = 0; i < editor.getScenario().getCivilians().size(); i++) {
			double r = random.nextDouble();
			double sum = 0;
			for (int m = 0; m < locationDis.length; m++) {
				for (int n = 0; n < locationDis[m].length; n++) {
					ArrayList<EntityID> temp = districts[m][n];
					if (temp.size() == 0) {
						if (locationDis[m][n] > 0.0000001) {
							Logger.error("civilian is allocated in a region without shape");
						}
					} else {
						sum += locationDis[m][n];
						if (r < sum)// civ location in region m:n
						{
							int location=temp.get(random.nextInt(temp.size())).getValue();
							civLocation.add(location);
							r=Double.MAX_VALUE;
							Logger.debug("assign civ location: "+location);
						}
					}
				}
			}
		}
		editor.getScenario().setCivilians(civLocation);
		randomDestination();
		editor.setChanged();
		editor.updateOverlays();
		editor.setOperation(getName());
	}

	public void randomDestination()
	{
		Iterator<Integer>civs=editor.getScenario().getCivilians().iterator();
		ArrayList<Destination>destinations=editor.getScenario().getDestination();
		List<GMLShape> all = new ArrayList<GMLShape>(editor.getMap().getAllShapes());
		Random random=new Random();
		destinations.clear();
		while (civs.hasNext())
		{
			Destination d=new Destination(civs.next());			
			int des=all.get(random.nextInt(all.size())).getID();
			d.getEnds().add(des);
			destinations.add(d);
		}
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Assign Civilian Destination From Location Distribution File";
	}
}
