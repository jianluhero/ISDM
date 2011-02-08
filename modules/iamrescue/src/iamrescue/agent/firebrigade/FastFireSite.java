package iamrescue.agent.firebrigade;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.routing.costs.BlockCheckerUtil;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

public class FastFireSite{
	private Building center;
	private Map<EntityID, Building> buildingsOnFire;
	private Map<Float, FastFireDistanceArray> fireArrays;

	private int NUMBER_OF_ARRAYS = 8;
	private int FIRE_DISTANCE = 50000; // 50m
	private int MAX_ALLOWED_DISTANCE = 100000; // 100m

	private ISimulationTimer timer;
	private IAMWorldModel model;
	private HeatTransferGraph graph;
	private int maxDistance = 0;
	
	/**
	 * Constructor of the class it initialises the center of the firesite, the graph, the world model, and
	 * some other useful parameters
	 *  
	 * @param center building at the center of the FireSite
	 * @param graph connection graph between buildings
	 * @param model world model to consider
	 * 
	 */
	public FastFireSite(ISimulationTimer timer, Building center, IAMWorldModel model, HeatTransferGraph graph){
		this.center = center;
		this.model = model;
		this.timer = timer;
		this.graph = graph;
		
		// all the buildings within the FireSite that are on fire
		this.buildingsOnFire = new FastMap<EntityID, Building>();
		this.buildingsOnFire.put(center.getID(), center);
		
		// bounds of the map
		Rectangle2D bounds = model.getBounds();		
		
		this.fireArrays = new FastMap<Float, FastFireDistanceArray>();
		float angle = 0f; // angle of the fire array
		for(int i = 0; i < NUMBER_OF_ARRAYS ; i++){
			if(center.getX() == bounds.getMinX() && center.getY() == bounds.getMinY())
				// bottom left corner
				if( angle < 0.0 || angle > 90.0 ) continue;
			
			if(center.getX() == bounds.getMinX() && center.getY() == bounds.getMaxY())
				// top left corner
				if( angle > 270.0 ) continue;

			if(center.getX() == bounds.getMaxX() && center.getY() == bounds.getMaxY())
				// top right corner
				if( angle < 180.0 || angle > 270.0) continue;

			if(center.getX() == bounds.getMaxX() && center.getY() == bounds.getMinY())
				// bottom right corner	
				if( angle < 90.0 || angle > 180.0 ) continue;
	
			FastFireDistanceArray fd_array = new FastFireDistanceArray(center, angle);

			fireArrays.put(angle, fd_array);
			
			angle += (double) 360 / NUMBER_OF_ARRAYS;	
		}

	}

	public boolean containsBuilding(Building building) {
		return buildingsOnFire.containsValue(building);
	}

	public boolean containsBuildingID(Building building) {
		return buildingsOnFire.containsKey(building.getID());
	}

	public void addBuildingOnFire(Building building) {
		buildingsOnFire.put(building.getID(), building);
		
		updateFireArrays(building);
	}
	
	public Collection<Building> getFringe(){
		FastSet<Building> fringe = new FastSet<Building>();

		for (Building building : buildingsOnFire.values()) {
			if(building.isFierynessDefined())
				if(building.getFieryness() == 1 || building.getFieryness() == 2)
					fringe.add(building);
		}
		
		return fringe;
	}

	public Collection<Building> getCenter() {
		FastSet<Building> centers = new FastSet<Building>();
		
		for (Building building : buildingsOnFire.values()) {
			if(building.isFierynessDefined())
				if(building.getFieryness() == 3)
					centers.add(building);
		}
		
		return centers;
	}
			
	private void updateFireArrays(Building building) {
		double buildingAngle = Math.atan((building.getY() - center.getY()) / ((double) building.getX() - center.getX()));
		
		double maxDistance = 0.0;
		
		FastFireDistanceArray closerArray = null;
		
		for (float fireSpreadingAngle : fireArrays.keySet()) {
			double distance = Math.sqrt((fireSpreadingAngle - buildingAngle)*(fireSpreadingAngle - buildingAngle));
			
			if(distance > maxDistance){
				maxDistance = distance;
				
				closerArray = fireArrays.get(fireSpreadingAngle);
			}
		}
		
		//TODO update closerarray
		updateSingleFireArray(closerArray, building);
	}
	
	/**
	 * update a single fireArray
	 * 
	 * @param time current time
	 * @param fireArray array to update
	 * 
	 */
	private void updateSingleFireArray(FastFireDistanceArray fireArray, Building building) {		
		Line2D fireLine = new Line2D(new Point2D(center.getX(), center.getY()), 
				new Point2D(center.getX() + (int) FIRE_DISTANCE*Math.cos(Math.toRadians(fireArray.getAngle())), 
						center.getY() + (int) FIRE_DISTANCE*Math.sin(Math.toRadians(fireArray.getAngle()))));
			
		if(BlockCheckerUtil.isIntersecting(building, fireLine, true)){
			double distance = Math.sqrt(
					( (double) building.getY() - center.getY())*(building.getY() - center.getY())
				  + ( (double) building.getX() - center.getX())*(building.getX() - center.getX())
				  );
			fireArray.setFrontierBuilding(building);
			fireArray.addDistance(timer.getTime(), (float) distance);
		}
	}
	
	public void removeBuilding(Building building) {
		buildingsOnFire.remove(building);
	}
	
	public Map<EntityID, Building> getBuildingsOnFire() {
		return buildingsOnFire;
	}


	public void setBuildingsOnFire(Map<EntityID, Building> allBuildings) {
		buildingsOnFire = allBuildings;
	}


	public void updateArraysSpeed(List<FastFireSite> containedFireSites) {
		// we build a map with all the average speed for all the angles
		Map<Float, Float> averageSpeeds = new FastMap<Float, Float>();
		
		for (FastFireSite fastFireSite : containedFireSites) {
			// we consider all the fireArrays of the current site
			Map<Float, FastFireDistanceArray> currentSiteArrays = fastFireSite.getFireArrays();
			
			for (Float angle : currentSiteArrays.keySet()) {
				// for each fire site we get the current speed we already store for 
				// the specific angle
				Float currentAngleSpeed = averageSpeeds.get(angle);
				
				// then we get the speed for the current array
				float currentArraySpeed = currentSiteArrays.get(angle).getAverageSpeed();
				
				if(currentAngleSpeed == null){
					averageSpeeds.put(angle, currentArraySpeed);
					continue;
				}
				
				float updatedSpeed = currentAngleSpeed + currentAngleSpeed;
				averageSpeeds.put(angle, updatedSpeed);
			}
		}
		
		for (Float angle : averageSpeeds.keySet()) {
			float newValue = averageSpeeds.get(angle) / containedFireSites.size();
			averageSpeeds.put(angle, newValue);
		}
		
		// we finally update the averageSpeed of each fire array
		for (Float arrayAngle : fireArrays.keySet()) {
			FastFireDistanceArray currentArray = fireArrays.get(arrayAngle);
			
			currentArray.setAverageSpeed(averageSpeeds.get(arrayAngle));
		}
	}
	
	/**
	 * This method predict the construct estimates the fireSite @param time
	 * time steps in the future
	 * 
	 * @param time the time at which we want the prediction
	 * @return a predicted copy of the current fireSite
	 * 
	 */
	public FastFireSite predict(int time){
	
		// we make a copy
		FastFireSite copy = null;
		try {
			 copy = (FastFireSite) this.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		copy.predictFireArrays(time);
		copy.predictBuildingsOnFire();
		
		return copy;	
	}	
	
	/**
	 * extend the fireArrays of the fireSite, considering the current prediction model
	 * 
	 * @param time time for which the prediction must be considered
	 * 
	 */
	public void predictFireArrays(int time) {
		for (FastFireDistanceArray fireArray : fireArrays.values()) {
			predictFireArray(time, fireArray);
		}	
	}

	
	/**
	 * this method extends the specified @param fireArray to the given @param time
	 *  
	 * @param time time of prediction
	 * @param fireArray fireArray to be predicted
	 * 
	 */
	private void predictFireArray(int time, FastFireDistanceArray fireArray) {
		FastSet<Building> visitedBuildings = new FastSet<Building>();
		
		boolean frontierHasChanged = true;
		
		while(frontierHasChanged){			
			Building currentFrontier = fireArray.getFrontierBuilding();
			
			// we visited the frontier
			visitedBuildings.add(currentFrontier);
			// and the previous frontier
			visitedBuildings.add(fireArray.getPreviousFrontier());
			
			double predictedDistance = fireArray.getPredictedDistance(time);
			
			Line2D fireLine = new Line2D(new Point2D(currentFrontier.getX(), currentFrontier.getY()), 
					new Point2D(currentFrontier.getX() + (int) predictedDistance*Math.cos(Math.toRadians(fireArray.getAngle())),
							currentFrontier.getY() + (int) predictedDistance*Math.sin(Math.toRadians(fireArray.getAngle()))));
				
			Collection<Building> neighbours = graph.getNeighbouringBuildings(currentFrontier);

			//un-set the flag

			frontierHasChanged = false;
			
			for (Building building : neighbours) { // TODO this portion of code might be BUGGED
				// for each neighbour we consider the FAREST ONE
				double maxNeighbourDistance = 0.0;
				
//				System.out.println("entering the NEIGHBOOURS loop");
				
				if(visitedBuildings.contains(building))
					// if we already visited the building then we move on
					continue;
								
				// get current neighbours lines
				if(BlockCheckerUtil.isIntersecting(building, fireLine, true)){ 						
					// building intersects the line and is on fire
					
					double distanceToCurrentFrontier = Math.sqrt(
										((double) building.getY() - currentFrontier.getY())*(building.getY() - currentFrontier.getY())
									+ ((double) building.getX() - currentFrontier.getX())*(building.getX() - currentFrontier.getX())
									);
					
					if(distanceToCurrentFrontier > maxNeighbourDistance){
						maxNeighbourDistance = distanceToCurrentFrontier;
						
						currentFrontier = building;						

						frontierHasChanged = true;	
					}					
				}
			// the building has now been visited
			visitedBuildings.add(building);
			}
		
			if(frontierHasChanged) {
			// update the current frontier
				fireArray.setPreviousFrontier(fireArray.getFrontierBuilding());

				// update the fireSite
				fireArray.setFrontierBuilding(currentFrontier);	

				// memorize the distance
				double distance = Math.sqrt(
						((double) currentFrontier.getY() - center.getY())*(currentFrontier.getY() - center.getY())
					+ ((double) currentFrontier.getX() - center.getX())*(currentFrontier.getX() - center.getX())
					);
	
				if(distance > maxDistance) maxDistance = (int) (distance + 0.5);
	
			}		
		}
	}
	
	/**
	 * same as method updateBuildingsOnFire
	 */
	private void predictBuildingsOnFire(){
		// we limit the size of predictions
		if(maxDistance > MAX_ALLOWED_DISTANCE)
			maxDistance = MAX_ALLOWED_DISTANCE;

		Collection<StandardEntity> buildingsToAdd = model.getObjectsInRange(center, maxDistance);
		
		for (StandardEntity standardEntity : buildingsToAdd) {
			if(standardEntity instanceof Building){
				Building building = (Building) standardEntity;
				if(building.isFierynessDefined()){
					if(building.getFieryness() == 8)
						buildingsOnFire.remove(building);									
				}
				else
					buildingsOnFire.put(building.getID(), building);
			}
		}
	}


	public Map<Float, FastFireDistanceArray> getFireArrays(){
		return fireArrays;
	}
	
	public Collection<Building> getSetOfBuildingsOnFire(){
		return buildingsOnFire.values();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		FastFireSite copy = new FastFireSite(this.timer, this.center, this.model, this.graph);
		copy.buildingsOnFire.putAll(this.buildingsOnFire);
		copy.fireArrays = new FastMap<Float, FastFireDistanceArray>();
		
		for (Float fireArrayAngle : this.fireArrays.keySet())
			copy.fireArrays.put(fireArrayAngle, (FastFireDistanceArray) this.fireArrays.get(fireArrayAngle).clone());
		
		return copy;
	}

	

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return super.equals(obj);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

	
//	/**
//	 * this method updates the FireSite with the current information of the
//	 * buildings on fire
//	 * 
//	 * @param time the current time step
//	 * @param buildingsOnFire2 new buildingsOnFire
//	 * 
//	 */
//	public void update(int time) {
//	}
//
//	/**
//	 * update the buildingsOnFire vector 
//	 * 
//	 */
//	private void updateBuildingsOnFire() {
//		Set<PositionXY> positionsSet = new FastSet<PositionXY>();
//		
//		if(maxDistance > MAX_ALLOWED_DISTANCE)
//			maxDistance = MAX_ALLOWED_DISTANCE;
//		
//		Collection<StandardEntity> buildingsToAdd = model.getObjectsInRange(center, maxDistance);
//		
//		for (StandardEntity standardEntity : buildingsToAdd) {
//			if(standardEntity instanceof Building){
//				Building building = (Building) standardEntity;
//				if(building.isFierynessDefined()){
//					if(building.getFieryness() <= 3 && building.getFieryness() >= 1)
//						buildingsOnFire.add(building);
//					else
//						buildingsOnFire.remove(building);					
//				}				
//			}
//		}
//	}
//
//	/**
//	 * update the fire arrays of the site, with the most up to date information
//	 * 
//	 * @param time current time
//	 */
//	public void updateFireArrays(int time){
//		for (FireDistanceArray fireArray : fireArrays) 
//			updateFireArray(time, fireArray);		
//	}
//	

//	
//								
//	
//
//	/**
//	 * this method checks if the current fireSite has AT LEAST one building that is contained in 
//	 * another fireSite
//	 * 
//	 * @param otherSite 
//	 * @return true if the 2 fireSites intersects
//	 */
//	public boolean intersects(FireSite otherSite) {
//		for (Building building : buildingsOnFire)
//			if(otherSite.containsBuilding(building)) return true;
//				
//		return false;
//	}
//
//	public Building getCenter() {
//		return center;
//	}
//	
//
}
