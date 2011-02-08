package iamrescue.agent;

public interface ISimulationTimer {

	/**
	 * 
	 * @return The current time
	 */
	public int getTime();

	/**
	 * Registers a new listener with this.
	 * 
	 * @param listener
	 *            The new listener
	 */
	public void addTimeStepListener(ITimeStepListener listener);

	/**
	 * Removes a listener
	 * 
	 * @param listener
	 *            The listener to remove
	 */
	public void removeTimeStepListener(ITimeStepListener listener);
}