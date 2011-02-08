package iamrescue.communication.messages.codec.updates;

import iamrescue.communication.messages.MessagePrefixes;
import iamrescue.communication.messages.updates.CivilianUpdatedMessage;
import iamrescue.communication.messages.updates.EntityUpdatedMessage;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

public class CivilianUpdatedMessageCodec extends EntityUpdatedMessageCodec {

	public static final CivilianUpdatedMessageCodec INSTANCE = new CivilianUpdatedMessageCodec();

	public CivilianUpdatedMessageCodec() {
		super(CivilianUpdatedMessage.relevantProperties);
	}

	@Override
	public byte getMessagePrefix() {
		return MessagePrefixes.CIVILIAN_UPDATE_PREFIX;
	}

	@Override
	protected Class<? extends Entity> getObjectClass() {
		return Civilian.class;
	}

	@Override
	protected Entity createObject(EntityID id) {
		return Registry.getCurrentRegistry().createEntity(
				StandardEntityURN.CIVILIAN.toString(), id);
	}

	@Override
	protected EntityUpdatedMessage createMessage(short timeStamp) {
		return new CivilianUpdatedMessage(timeStamp);
	}
}
