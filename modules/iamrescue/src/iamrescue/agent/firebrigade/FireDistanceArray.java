package iamrescue.agent.firebrigade;

import java.util.Map;

import javolution.util.FastMap;

import rescuecore2.standard.entities.Building;

/**
 * This class models fire spreading in a specific direction ( @param angle ) by keeping track 
 * of it's speed ( @param distances ) during time;
 *   
 * @author fmdf08r
 *
 */
public class FireDistanceArray {
	
	private Map<Integer, Double> distances; // maps distance with time
	private double angle; // direction of fire spreading

	private Building frontierBuilding; // frontier of the fire in a specific direction
//	private int lastUpdatedTime; // previous update
	private double averageSpeed;
	private Building previousFrontier;

	/**
	 * Constructor of the class
	 * 
	 * @param center initial frontier (commonly the centre of the fire site)
	 * @param angle direction of the fire
	 * 
	 */
	public FireDistanceArray(Building center, double angle){
		this.angle = angle;
		this.frontierBuilding = center;
		this.previousFrontier = center;
		
		distances = new FastMap<Integer, Double>();
	}
	
	/**
	 * 
	 * @return frontier building of the Fire in the current Direction
	 * 
	 */
	public Building getFrontierBuilding(){
		return frontierBuilding;
	}
	
	/**
	 * 
	 * @return current angle ~ direction of the Fire
	 * 
	 */
	public double getAngle(){
		return angle;
	}

	/**
	 * set the new frontier building of the fire direction
	 * 
	 * @param building
	 */
	public void setFrontierBuilding(Building building) {
		frontierBuilding = building;
	}

	/**
	 * the following method add a new distance to the estimate
	 * 
	 * @param time current time step
	 * @param distance to memorise
	 */
	public void addDistance(int time, double distance) {
		Double previousDistance = this.distances.get(time);
		
		this.distances.put(new Integer(time), distance);
		
		if(previousDistance == null){
//			System.out.println(frontierBuilding + " the distance to be added is: " + distance);
			this.averageSpeed += distance / time;
		}
		else
			this.averageSpeed = averageSpeed + (distance - previousDistance) / time; 
		
//		this.lastUpdatedTime = time;
	}

	/**
	 * calculate the estimated average of the speed of fire in a certain direction
	 * 
	 * @param time at which we want the estimate to work
	 * 
	 * @return the estimated distance of where the fire is going to be after @param time  time-steps
	 * 
	 */
	public double getPredictedDistance(int time) {
//		return (averageSpeed / distances.size())*(time - lastUpdatedTime);
		//TODO prediction can be ameliorated by considering the building that can slow the prediction down
		
		return (averageSpeed / distances.size())*time;

	}
	
	/**
	 * 
	 * @return distances vector
	 */
	public Map<Integer, Double> getDistances(){
		return distances;
	}
	
	/**
	 * update the PreviousFrontier of this fireArray
	 * @param frontier
	 */
	public void setPreviousFrontier(Building frontier) {
		previousFrontier = frontier;		
	}
	
	/**
	 * 
	 * @return previousFrontier parameter
	 * 
	 */
	public Building getPreviousFrontier(){
		return previousFrontier;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		FireDistanceArray compareArray = (FireDistanceArray) obj;
		
		boolean anglesAreEquals = false;
		boolean frontiersAreEquals = false;
		boolean distancesAreEquals = false;
		
		if(angle == compareArray.getAngle()){
//			System.out.println("angles are: " + angle + " " + compareArray.getAngle());
			anglesAreEquals = true;
		}
		if(frontierBuilding.equals(compareArray.getFrontierBuilding())) frontiersAreEquals = true;
		if(distances.equals(compareArray.getDistances())) distancesAreEquals = true;

		
		return anglesAreEquals && frontiersAreEquals && distancesAreEquals;
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		FireDistanceArray clone = new FireDistanceArray(this.frontierBuilding, this.angle);
		
		clone.previousFrontier = this.previousFrontier;
		clone.averageSpeed = this.averageSpeed;
		
		for (Integer time : this.distances.keySet())
			clone.addDistance(time, this.distances.get(time));			

		return clone;
	}
}
