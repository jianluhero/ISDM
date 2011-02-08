package iamrescue.routing.util;

import iamrescue.execution.command.IPath;

public interface ISpeedInfo {

	/**
	 * 
	 * @return Distance (in mm) that agent moved during last time step
	 */
	public int getLastTimeStepDistance();

	/**
	 * 
	 * @return Average distance (in mm) the agent moves during each time step.
	 */
	public double getDistancePerTimeStep();

	/**
	 * 
	 * @param distance
	 *            A given distance (in mm)
	 * @return The time it will take the agent to travel this (result is a
	 *         fraction - round up for a realistic estimate)
	 */
	public double getTimeToTravelDistance(double distance);

	/**
	 * 
	 * @param path
	 *            A given path
	 * @return The approximate time it will take to travel this path (round up
	 *         for a realistic estimate).
	 */
	public double getTimeToTravelPath(IPath path);
}