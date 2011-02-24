package gis2.scenario;

import gis2.Destination;
import gis2.RandomScenarioGenerator;
import gis2.Scenario;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;
import maps.gml.GMLShape;

/**
 * randomise civilians' destination
 * delay will be 0
 * @author Bing Shi
 *
 */
public class RandomiseDestination extends AbstractFunction {

	private Random random;
	
	public RandomiseDestination(ScenarioEditor editor) {
		 super(editor);
	        random = new Random();
	}

	@Override
	public String getName() {
		return "Randomise Des. for Civ.";
	}

	@Override
	public void execute() {
		editor.setOperation(getName());
		Iterator<Integer>civs=editor.getScenario().getCivilians().iterator();
		ArrayList<Destination>destinations=editor.getScenario().getDestination();
		List<GMLShape> all = new ArrayList<GMLShape>(editor.getMap().getAllShapes());
		Random random=new Random();
		destinations.clear();
		while (civs.hasNext())
		{
			Destination d=new Destination(civs.next());			
			int des=all.get(random.nextInt(all.size())).getID();
			Logger.debug("civilian from "+d.getStart()+" to "+des+" dely: "+d.getDelay());
			d.getEnds().add(des);
			destinations.add(d);
		}
		editor.setChanged();
		editor.updateOverlays();	
	}
}
