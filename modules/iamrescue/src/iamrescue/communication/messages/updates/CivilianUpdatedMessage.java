package iamrescue.communication.messages.updates;

import iamrescue.communication.messages.codec.IMessageCodec;
import iamrescue.communication.messages.codec.updates.CivilianUpdatedMessageCodec;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.worldmodel.Entity;

public class CivilianUpdatedMessage extends AbstractHumanUpdatedMessage {

	public CivilianUpdatedMessage(short timestamp) {
		super(timestamp);
	}

	@Override
	public IMessageCodec getCodec() {
		return CivilianUpdatedMessageCodec.INSTANCE;
	}

	@Override
	protected EntityUpdatedMessage createMessage(short timestamp) {
		return new CivilianUpdatedMessage(timestamp);
	}

	@Override
	protected boolean isCorrectObjectClass(Entity object) {
		return object instanceof Civilian;
	}

	@Override
	public String getMessageName() {
		return "CivilianUpdatedMessage";
	}
}
