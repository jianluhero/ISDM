package iamrescue.communication.messages.updates;

import iamrescue.communication.messages.codec.IMessageCodec;
import iamrescue.communication.messages.codec.updates.FireBrigadeUpdatedMessageCodec;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.worldmodel.Entity;

public class FireBrigadeUpdatedMessage extends AbstractHumanUpdatedMessage {

	public FireBrigadeUpdatedMessage(short timestamp) {
		super(timestamp);
	}

	@Override
	public IMessageCodec getCodec() {
		return new FireBrigadeUpdatedMessageCodec();
	}

	@Override
	protected EntityUpdatedMessage createMessage(short timestamp) {
		return new FireBrigadeUpdatedMessage(timestamp);
	}

	@Override
	protected boolean isCorrectObjectClass(Entity object) {
		return object instanceof FireBrigade;
	}

	@Override
	public String getMessageName() {
		return "FireBrigadeUpdatedMessage";
	}
}
