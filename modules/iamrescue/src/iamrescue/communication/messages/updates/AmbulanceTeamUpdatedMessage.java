package iamrescue.communication.messages.updates;

import iamrescue.communication.messages.codec.IMessageCodec;
import iamrescue.communication.messages.codec.updates.AmbulanceTeamUpdatedMessageCodec;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.worldmodel.Entity;

public class AmbulanceTeamUpdatedMessage extends AbstractHumanUpdatedMessage {

	public AmbulanceTeamUpdatedMessage(short timestamp) {
		super(timestamp);
	}

	@Override
	public IMessageCodec getCodec() {
		return AmbulanceTeamUpdatedMessageCodec.INSTANCE;
	}

	@Override
	protected EntityUpdatedMessage createMessage(short timestamp) {
		return new AmbulanceTeamUpdatedMessage(timestamp);
	}

	@Override
	protected boolean isCorrectObjectClass(Entity object) {
		return object instanceof AmbulanceTeam;
	}

	@Override
	public String getMessageName() {
		return "AmbulanceTeamUpdatedMessage";
	}
}
