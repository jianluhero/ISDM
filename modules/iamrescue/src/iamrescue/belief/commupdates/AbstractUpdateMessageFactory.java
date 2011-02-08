/**
 * 
 */
package iamrescue.belief.commupdates;

import iamrescue.communication.messages.updates.EntityUpdatedMessage;

import java.util.Set;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.Property;


/**
 * @author Sebastian
 * 
 */
public abstract class AbstractUpdateMessageFactory implements
		IUpdateMessageFactory {

	protected abstract EntityUpdatedMessage createMessage();

	public EntityUpdatedMessage createUpdateMessage(StandardEntity entity,
			Set<Property> properties) {
		EntityUpdatedMessage message = createMessage();
		message.setObject(entity);
		for (Property property : properties) {
			message.addUpdatedProperty(property);
		}
		return message;
	}

}
