/**
 * 
 */
package iamrescue.belief.commupdates;

import iamrescue.belief.IAMWorldModel;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public interface IChangePropagationFilter {
	public ChangeSet filterUnneededUpdates(ChangeSet changeSet,
			IAMWorldModel worldModel, EntityID myself);
}
