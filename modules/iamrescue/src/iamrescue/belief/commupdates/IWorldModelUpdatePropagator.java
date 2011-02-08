/**
 * 
 */
package iamrescue.belief.commupdates;

import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public interface IWorldModelUpdatePropagator {
	public void sendUpdates(ChangeSet changed, EntityID myID);
}
