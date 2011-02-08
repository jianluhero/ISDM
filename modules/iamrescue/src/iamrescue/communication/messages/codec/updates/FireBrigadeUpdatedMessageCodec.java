package iamrescue.communication.messages.codec.updates;

import iamrescue.communication.messages.MessagePrefixes;
import iamrescue.communication.messages.updates.EntityUpdatedMessage;
import iamrescue.communication.messages.updates.FireBrigadeUpdatedMessage;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

public class FireBrigadeUpdatedMessageCodec extends EntityUpdatedMessageCodec {

	// public static final FireBrigadeUpdatedMessageCodec INSTANCE = new
	// FireBrigadeUpdatedMessageCodec();

	public FireBrigadeUpdatedMessageCodec() {
		super(FireBrigadeUpdatedMessage.relevantProperties);
	}

	@Override
	public byte getMessagePrefix() {
		return MessagePrefixes.FIRE_BRIGADE_UPDATE_PREFIX;
	}

	@Override
	protected Class<? extends Entity> getObjectClass() {
		return FireBrigade.class;
	}

	@Override
	protected Entity createObject(EntityID id) {
		return Registry.getCurrentRegistry().createEntity(
				StandardEntityURN.FIRE_BRIGADE.toString(), id);
	}

	@Override
	protected EntityUpdatedMessage createMessage(short timeStamp) {
		return new FireBrigadeUpdatedMessage(timeStamp);
	}
}
