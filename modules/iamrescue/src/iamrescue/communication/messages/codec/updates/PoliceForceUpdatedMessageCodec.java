package iamrescue.communication.messages.codec.updates;

import iamrescue.communication.messages.MessagePrefixes;
import iamrescue.communication.messages.updates.EntityUpdatedMessage;
import iamrescue.communication.messages.updates.PoliceForceUpdatedMessage;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

public class PoliceForceUpdatedMessageCodec extends EntityUpdatedMessageCodec {

	public static final PoliceForceUpdatedMessageCodec INSTANCE = new PoliceForceUpdatedMessageCodec();

	public PoliceForceUpdatedMessageCodec() {
		super(PoliceForceUpdatedMessage.relevantProperties);
	}

	@Override
	public byte getMessagePrefix() {
		return MessagePrefixes.POLICE_FORCE_UPDATE_PREFIX;
	}

	@Override
	protected Class<? extends Entity> getObjectClass() {
		return PoliceForce.class;
	}

	@Override
	protected Entity createObject(EntityID id) {
		return Registry.getCurrentRegistry().createEntity(
				StandardEntityURN.POLICE_FORCE.toString(), id);
	}

	@Override
	protected EntityUpdatedMessage createMessage(short timeStamp) {
		return new PoliceForceUpdatedMessage(timeStamp);
	}
}
