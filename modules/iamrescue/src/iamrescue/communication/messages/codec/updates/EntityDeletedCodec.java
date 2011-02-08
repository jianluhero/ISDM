/**
 * 
 */
package iamrescue.communication.messages.codec.updates;

import iamrescue.communication.messages.MessagePrefixes;
import iamrescue.communication.messages.codec.AbstractMessageCodec;
import iamrescue.communication.messages.codec.BitStreamDecoder;
import iamrescue.communication.messages.codec.BitStreamEncoder;
import iamrescue.communication.messages.updates.EntityDeletedMessage;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class EntityDeletedCodec extends
		AbstractMessageCodec<EntityDeletedMessage> {

	@Override
	protected EntityDeletedMessage decodeMessage(BitStreamDecoder decoder) {
		int id = decoder.readInt();
		short timeStamp = decoder.readShort();
		return new EntityDeletedMessage(new EntityID(id), timeStamp);
	}

	protected void encodeMessage(EntityDeletedMessage message,
			BitStreamEncoder encoder) {
		EntityID id = message.getId();
		short timeStamp = message.getTimeStamp();
		encoder.appendInt(id.getValue());
		encoder.appendShort(timeStamp);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.codec.IMessageCodec#getMessagePrefix()
	 */
	@Override
	public byte getMessagePrefix() {
		return MessagePrefixes.ENTITY_DELETED_PREFIX;
	}

}
