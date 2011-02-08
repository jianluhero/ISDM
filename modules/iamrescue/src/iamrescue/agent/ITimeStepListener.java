/**
 * 
 */
package iamrescue.agent;

/**
 * @author Sebastian
 * 
 */
public interface ITimeStepListener {
	/**
	 * Called when a new time step has started. This should be called after all
	 * information has been integrated into the belief base (from sense messages
	 * and basic updates from other agents).
	 * 
	 * @param timeStep
	 *            The time step that has started.
	 */
	public void notifyTimeStepStarted(int timeStep);
}
