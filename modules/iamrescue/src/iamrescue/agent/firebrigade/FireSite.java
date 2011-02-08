package iamrescue.agent.firebrigade;

import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.spatial.ISpatialIndex;
import iamrescue.belief.spatial.SpatialIndex;
import iamrescue.belief.spatial.SpatialQuery;
import iamrescue.belief.spatial.SpatialQueryFactory;
import iamrescue.util.PositionXY;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javolution.util.FastSet;

import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

/**
 * This class represent a cluster of buildings on fire, that is a Fire Site, and contains all the properties,
 * that we use in order to represent it, extend it and finally maintain it
 * 
 * @author fmdf08r
 *
 */
public class FireSite {

	private Building center;
	private Collection<Building> buildingsOnFire;
	private Set<FireDistanceArray> fireArrays;
	
	private int NUMBER_OF_ARRAYS = 8; // DEFAULT

	private IAMWorldModel model;
	private int FIRE_DISTANCE = 50000; // mm ???
	private HeatTransferGraph graph;
	private int maxDistance  = 0;
	private int MAX_ALLOWED_DISTANCE;	
	
	/**
	 * Constructor of the class it initialises the center of the firesite, the graph, the world model, and
	 * some other useful parameters
	 *  
	 * @param center building at the center of the FireSite
	 * @param graph connection graph between buildings
	 * @param model world model to consider
	 * 
	 */
	public FireSite(Building center, HeatTransferGraph graph, IAMWorldModel model, int numberOfFireArrays){
		this.center = center;
		this.model = model;
		this.graph = graph;
		
		this.NUMBER_OF_ARRAYS = numberOfFireArrays;
				
		// bounds of the map
		Rectangle2D bounds = model.getBounds();		
		
		// all the buildings within the FireSite that are on fire
		this.buildingsOnFire = new FastSet<Building>();
		this.buildingsOnFire.add(center);
		
		fireArrays = new FastSet<FireDistanceArray>();
		double angle = 0.0; // angle of the fire array
		for(int i = 0; i < NUMBER_OF_ARRAYS; i++){
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
	
			FireDistanceArray fd_array = new FireDistanceArray(center, angle);

			fireArrays.add(fd_array);
			
			angle += (double) 360 / NUMBER_OF_ARRAYS;	
		}
	}

	/**
	 * Constructor with a default number of FireArrays
	 * 
	 * @param center ~ same as other constructor 
	 * @param heatTransferGraph ~ same as other constructor
	 * @param model ~ same as other constructor
	 */
	public FireSite(Building center, HeatTransferGraph graph, IAMWorldModel model) {
		this(center, graph, model, 8);
	}

	/**
	 * this method updates the FireSite with the current information of the
	 * buildings on fire
	 * 
	 * @param time the current time step
	 * @param buildingsOnFire2 new buildingsOnFire
	 * 
	 */
	public void update(int time) {
		// we first update the fireArray
		updateFireArrays(time);
		
		updateBuildingsOnFire();
	}

	/**
	 * update the buildingsOnFire vector 
	 * 
	 */
	private void updateBuildingsOnFire() {
		Set<PositionXY> positionsSet = new FastSet<PositionXY>();
		
		if(maxDistance > MAX_ALLOWED_DISTANCE)
			maxDistance = MAX_ALLOWED_DISTANCE;
		
		Collection<StandardEntity> buildingsToAdd = model.getObjectsInRange(center, maxDistance);
		
		for (StandardEntity standardEntity : buildingsToAdd) {
			if(standardEntity instanceof Building){
				Building building = (Building) standardEntity;
				if(building.isFierynessDefined()){
					if(building.getFieryness() <= 3 && building.getFieryness() >= 1)
						buildingsOnFire.add(building);
					else
						buildingsOnFire.remove(building);					
				}				
			}
		}
	}

	/**
	 * update the fire arrays of the site, with the most up to date information
	 * 
	 * @param time current time
	 */
	public void updateFireArrays(int time){
		for (FireDistanceArray fireArray : fireArrays) 
			updateFireArray(time, fireArray);		
	}
	
	/**
	 * update a single fireArray
	 * 
	 * @param time current time
	 * @param fireArray array to update
	 * 
	 */
	private void updateFireArray(int time, FireDistanceArray fireArray) {		
		FastSet<Building> visitedBuildings = new FastSet<Building>();
		
		boolean frontierHasChanged = true;
		
		Building currentFrontier = fireArray.getFrontierBuilding();
				
		while(frontierHasChanged){
			Line2D fireLine = new Line2D(new Point2D(currentFrontier.getX(), currentFrontier.getY()), 
											new Point2D(currentFrontier.getX() + (int) FIRE_DISTANCE*Math.cos(Math.toRadians(fireArray.getAngle())), 
													currentFrontier.getY() + (int) FIRE_DISTANCE*Math.sin(Math.toRadians(fireArray.getAngle()))));
			
			// we visited the frontier
			visitedBuildings.add(currentFrontier);
			// and the previous frontier
			visitedBuildings.add(fireArray.getPreviousFrontier());

			Collection<Building> neighbours = graph.getNeighbouringBuildings(currentFrontier);

			//un-set the flag
			frontierHasChanged = false;
			
			for (Building building : neighbours) {
				double maxNeighbourDistance = 0.0;

				if(visitedBuildings.contains(building))
					// if we already visited the building then we move on
					continue;

				if(building.isFierynessDefined()){
					// the building is on fire
					if(building.getFieryness() <= 3 || building.getFieryness() >= 1){
						
						// get current neighbours lines
						int[] apexes = ((Building) building).getApexList();
						List<Point2D> points = GeometryTools2D.vertexArrayToPoints(apexes);
						List<Line2D> lines = GeometryTools2D.pointsToLines(points, true);
					
						boolean intersects = false;
						for (Line2D line : lines) {

							double d1 = line.getIntersection(fireLine);
							double d2 = fireLine.getIntersection(line);
							
							if (d2 >= 0 && d2 <= 1 && d1 >= 0 && d1 <= 1) {
								intersects = true;
								break;
								}

						}
						
						if(intersects){							
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
					}					
				}
				
				// the building has now been visited
				visitedBuildings.add(building);
			}	
			
			if(frontierHasChanged == true) {
				// update the current frontier
				fireArray.setPreviousFrontier(fireArray.getFrontierBuilding());

				// update the fireSite
				fireArray.setFrontierBuilding(currentFrontier);	

				// memorise the distance
				double distance = Math.sqrt(
						( (double) currentFrontier.getY() - center.getY())*(currentFrontier.getY() - center.getY())
					  + ( (double) currentFrontier.getX() - center.getX())*(currentFrontier.getX() - center.getX())
					  );

				if(distance > maxDistance) maxDistance = (int) (distance + 0.5);

				fireArray.addDistance(time, distance);
			}
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
	public FireSite predict(int time){
	
		// we make a copy
		FireSite copy = null;
		try {
			 copy = (FireSite) this.clone();
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
		for (FireDistanceArray fireArray : fireArrays) {
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
	private void predictFireArray(int time, FireDistanceArray fireArray) {
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
				int[] apexes = ((Building) building).getApexList();
				List<Point2D> points = GeometryTools2D.vertexArrayToPoints(apexes);
				List<Line2D> lines = GeometryTools2D.pointsToLines(points, true);
				
				boolean intersects = false;
				for (Line2D line : lines) {
					double d1 = line.getIntersection(fireLine);
					double d2 = fireLine.getIntersection(line);
						
					if (d2 >= 0 && d2 <= 1 && d1 >= 0 && d1 <= 1) {
						intersects = true;
						break;
						}
				}
					
				if(intersects){ 						
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
			
//			System.out.println("exiting the WHILE LOOP");
		}
//		System.out.println("ENDING THE PREDICTFIREARRAYMETHOD");
	}
	
	/**
	 * same as method updateBuildingsOnFire
	 */
	public void predictBuildingsOnFire(){
		if(maxDistance > MAX_ALLOWED_DISTANCE)
			maxDistance = MAX_ALLOWED_DISTANCE;
//		System.out.println("maxDistance is: " +  maxDistance);
		Collection<StandardEntity> buildingsToAdd = model.getObjectsInRange(center, maxDistance);
		
//		System.out.println("Buildings to add are: ");
		for (StandardEntity standardEntity : buildingsToAdd) {
			if(standardEntity instanceof Building){
				Building building = (Building) standardEntity;
//				System.out.println(building);
				if(building.isFierynessDefined()){
					if(building.getFieryness() == 8)
						buildingsOnFire.remove(building);									
				}
				else
					buildingsOnFire.add(building);
			}
		}
	}

	/**
	 * this method checks if the current fireSite has AT LEAST one building that is contained in 
	 * another fireSite
	 * 
	 * @param otherSite 
	 * @return true if the 2 fireSites intersects
	 */
	public boolean intersects(FireSite otherSite) {
		for (Building building : buildingsOnFire)
			if(otherSite.containsBuilding(building)) return true;
				
		return false;
	}

	public boolean containsBuilding(Building building) {
		return buildingsOnFire.contains(building);
	}

	public Building getCenter() {
		return center;
	}
	
	public Set<FireDistanceArray> getFireArrays(){
		return fireArrays;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		FireSite copy = new FireSite(this.center, this.graph, this.model, this.NUMBER_OF_ARRAYS);
		copy.buildingsOnFire.addAll(this.buildingsOnFire);
		copy.maxDistance = this.maxDistance;
		copy.fireArrays = new FastSet<FireDistanceArray>();
		
		for (FireDistanceArray fireArray : this.fireArrays)
			copy.fireArrays.add((FireDistanceArray) fireArray.clone());
		
		return copy;
	}

	public List<Building> getListOfBuildingsOnFire(){
		ArrayList<Building> list = new ArrayList<Building>();
		list.addAll(buildingsOnFire);
		
		return list;
	}
	
	public Collection<Building> getBuildingsOnFire() {
		// TODO Auto-generated method stub
		return buildingsOnFire;
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return super.equals(obj);
	}
}
