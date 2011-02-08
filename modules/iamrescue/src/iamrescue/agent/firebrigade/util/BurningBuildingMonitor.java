package iamrescue.agent.firebrigade.util;

import iamrescue.belief.IAMWorldModel;

import java.util.Collection;
import java.util.Set;

import javolution.util.FastSet;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityListener;
import rescuecore2.worldmodel.Property;

public class BurningBuildingMonitor implements EntityListener {

	Set<Building> burningbuildings;
	
	private IAMWorldModel world_model;

	public BurningBuildingMonitor(IAMWorldModel model){
		world_model = model;

		burningbuildings = new FastSet<Building>();
		
		Collection<StandardEntity> buildings = model.getEntitiesOfType(StandardEntityURN.BUILDING, 
				StandardEntityURN.REFUGE, 
				StandardEntityURN.POLICE_OFFICE,
				StandardEntityURN.AMBULANCE_CENTRE,
				StandardEntityURN.FIRE_STATION);
		
		for (StandardEntity building : buildings) {
			Building b = (Building) building;
			
			building.addEntityListener(this);
			if(b.isFierynessDefined() && b.getFieryness() > 0) burningbuildings.add(b);
		}

	}

	/**
	 * 
	 * check the fieryness values
	 */
	@Override
	public void propertyChanged(Entity e, Property p, Object oldValue,
			Object newValue) {
		if(p.getURN().equals(StandardPropertyURN.FIERYNESS) && p.isDefined()){
			if((Integer) newValue > 0 && (Integer) newValue < 4) burningbuildings.add((Building) e);
			else burningbuildings.remove((Building) e);
		}	
		
	}
	
	public Set<Building> getBurningbuildings() {
		return burningbuildings;
	}
}
