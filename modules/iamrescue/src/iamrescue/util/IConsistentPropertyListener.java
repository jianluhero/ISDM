/**
 * 
 */
package iamrescue.util;

import iamrescue.util.ConsistentPropertyChangeNotifier.PropertyUpdate;
import rescuecore2.worldmodel.Entity;

/**
 * @author Sebastian
 * 
 */
public interface IConsistentPropertyListener {
	/**
	 * Called when all relevant properties have changed in a single time step.
	 * 
	 * @param entity
	 *            The entity that has changed.
	 * @param updates
	 *            all relevant updates received this time step
	 */
	public void allPropertiesChanged(Entity entity, PropertyUpdate[] updates);
}
