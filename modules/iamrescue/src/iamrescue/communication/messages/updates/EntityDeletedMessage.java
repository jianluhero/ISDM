/**
 * 
 */
package iamrescue.communication.messages.updates;

import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.codec.IMessageCodec;
import iamrescue.communication.messages.codec.updates.EntityDeletedCodec;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class EntityDeletedMessage extends Message {

	private EntityID id;

	private short timeStamp;

	public EntityDeletedMessage(EntityID id, short timeStamp) {
		this.id = id;
		this.timeStamp = timeStamp;
	}

	public String toShortString() {
		return getClass().getSimpleName() + "[s:" + getSenderAgentID() + ",t:"
				+ timeStamp + "]";
	}

	/**
	 * @return the id
	 */
	public EntityID getId() {
		return id;
	}

	/**
	 * @return the timeStamp
	 */
	public short getTimeStamp() {
		return timeStamp;
	}

	@Override
	public Message copy() {
		return new EntityDeletedMessage(id, timeStamp);
	}

	@Override
	public IMessageCodec<EntityDeletedMessage> getCodec() {
		return new EntityDeletedCodec();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.Message#getMessageContentsAsString()
	 */
	@Override
	public String getMessageContentsAsString() {
		return "ID=" + getId() + ",timeStamp=" + getTimeStamp();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.communication.messages.Message#getMessageName()
	 */
	@Override
	public String getMessageName() {
		return "EntityDeletedMessage";
	}

}
