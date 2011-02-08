package iamrescue.communication.messages.updates;

import iamrescue.communication.messages.codec.IMessageCodec;
import iamrescue.communication.messages.codec.updates.PoliceForceUpdatedMessageCodec;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.worldmodel.Entity;

public class PoliceForceUpdatedMessage extends AbstractHumanUpdatedMessage {

	public PoliceForceUpdatedMessage(short timestamp) {
		super(timestamp);
	}

	@Override
	public IMessageCodec getCodec() {
		return PoliceForceUpdatedMessageCodec.INSTANCE;
	}

	@Override
	protected EntityUpdatedMessage createMessage(short timestamp) {
		return new PoliceForceUpdatedMessage(timestamp);
	}

	@Override
	protected boolean isCorrectObjectClass(Entity object) {
		return object instanceof PoliceForce;
	}

	@Override
	public String getMessageName() {
		return "PoliceForceUpdatedMessage";
	}
}
