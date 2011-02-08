package iamrescue.agent.firebrigade;

import java.util.Map;

import javolution.util.FastMap;
import rescuecore2.standard.entities.Building;

public class FastFireDistanceArray {
	private Map<Integer, Float> distances; // maps distance with time
	private float angle; // direction of fire spreading

	private float averageSpeed;
	private Building frontier;
	private Building previousFrontier;

	/**
	 * Constructor of the class
	 * 
	 * @param center initial frontier (commonly the centre of the fire site)
	 * @param angle direction of the fire
	 * 
	 */
	public FastFireDistanceArray(Building center, float angle){
		this.angle = angle;
		this.frontier = center;
		this.previousFrontier = center;
		
		
		distances = new FastMap<Integer, Float>();
	}
		
	/**
	 * 
	 * @return current angle ~ direction of the Fire
	 * 
	 */
	public double getAngle(){
		return angle;
	}

	
	public float getAverageSpeed(){
		return averageSpeed;
	}
	
	/**
	 * the following method add a new distance to the estimate
	 * 
	 * @param time current time step
	 * @param distance to memorise
	 */
	public void addDistance(int time, float distance) {
		Float previousDistance = this.distances.get(time);
		
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
		return (averageSpeed / distances.size())*time;

	}
	
	/**
	 * 
	 * @return distances vector
	 */
	public Map<Integer, Float> getDistances(){
		return distances;
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
		if(distances.equals(compareArray.getDistances())) distancesAreEquals = true;

		
		return anglesAreEquals && frontiersAreEquals && distancesAreEquals;
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		FastFireDistanceArray clone = new FastFireDistanceArray(this.frontier, this.angle);
		
		clone.averageSpeed = this.averageSpeed;
		
		for (Integer time : this.distances.keySet())
			clone.addDistance(time, this.distances.get(time));			

		return clone;
	}

	public void setAverageSpeed(Float speed) {
		averageSpeed = speed;
	}

	public void setFrontierBuilding(Building building) {
		this.frontier = building;
	}

	public Building getFrontierBuilding() {
		return frontier;
	}

	public Building getPreviousFrontier() {
		return previousFrontier;
	}

	public void setPreviousFrontier(Building previousFrontier) {
		this.previousFrontier = previousFrontier;	
	}
}
