/**
 * 
 */
package iamrescue.belief.inference;

import rescuecore2.worldmodel.ChangeSet;

/**
 * 
 * This handles negative sense updates. Checks the world model and removes
 * locations of entities that are expected near the agent, but have not been
 * seen.
 * 
 * @author Sebastian
 * 
 */
public interface INegativeSenseUpdater {
	/**
	 * Removes locations of unseen (but expected) entities from world model
	 * 
	 * @param seen
	 *            The entities seen during this time step
	 * @return The entities that have been changed as a result of this method
	 *         (useful for communicating this).
	 */
	public ChangeSet removeUnseenEntities(ChangeSet seen);
}
