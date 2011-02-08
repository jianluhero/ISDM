package iamrescue.communication.messages.codec.updates;

import iamrescue.communication.messages.MessagePrefixes;
import iamrescue.communication.messages.updates.AmbulanceTeamUpdatedMessage;
import iamrescue.communication.messages.updates.EntityUpdatedMessage;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

public class AmbulanceTeamUpdatedMessageCodec extends EntityUpdatedMessageCodec {

	public static final AmbulanceTeamUpdatedMessageCodec INSTANCE = new AmbulanceTeamUpdatedMessageCodec();

	public AmbulanceTeamUpdatedMessageCodec() {
		super(AmbulanceTeamUpdatedMessage.relevantProperties);
	}

	@Override
	public byte getMessagePrefix() {
		return MessagePrefixes.AMBULANCE_TEAM_UPDATE_PREFIX;
	}

	@Override
	protected Class<? extends Entity> getObjectClass() {
		return AmbulanceTeam.class;
	}

	@Override
	protected Entity createObject(EntityID id) {
		return Registry.getCurrentRegistry().createEntity(
				StandardEntityURN.AMBULANCE_TEAM.toString(), id);
	}

	@Override
	protected EntityUpdatedMessage createMessage(short timeStamp) {
		return new AmbulanceTeamUpdatedMessage(timeStamp);
	}
}
