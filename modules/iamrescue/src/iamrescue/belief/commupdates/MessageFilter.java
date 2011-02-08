/**
 * 
 */
package iamrescue.belief.commupdates;

import java.util.Set;

import iamrescue.belief.IAMWorldModel;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

/**
 * @author Sebastian
 * 
 */
public class MessageFilter implements IChangePropagationFilter {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.belief.commupdates.IMessageFilter#filterUnneededUpdates(rescuecore2
	 * .worldmodel.ChangeSet, iamrescue.belief.IAMWorldModel,
	 * rescuecore2.worldmodel.EntityID)
	 */
	@Override
	public ChangeSet filterUnneededUpdates(ChangeSet changeSet,
			IAMWorldModel worldModel, EntityID myself) {
		/*ChangeSet filteredChanges = new ChangeSet();
		Set<EntityID> changes = changeSet.getChangedEntities();
		for (EntityID id : changes) {
			Set<Property> changedProperties = changeSet.getChangedProperties(id);
			for (Property)
		}*/

		
		return null;
	}

}
