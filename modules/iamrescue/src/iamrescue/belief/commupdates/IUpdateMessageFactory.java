/**
 * 
 */
package iamrescue.belief.commupdates;

import iamrescue.communication.messages.updates.EntityUpdatedMessage;

import java.util.Set;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.Property;

/**
 * 
 * A generic factory to create update messages.
 * 
 * @author Sebastian
 * 
 */
public interface IUpdateMessageFactory {
	/**
	 * Creates an update message based on an entity and a set of updated
	 * properties.
	 * 
	 * @param entity
	 *            THe entity that has been updated.
	 * @param properties
	 *            The properties to include in the message.
	 * @return The constructed message.
	 */
	public EntityUpdatedMessage createUpdateMessage(StandardEntity entity,
			Set<Property> properties);
}
