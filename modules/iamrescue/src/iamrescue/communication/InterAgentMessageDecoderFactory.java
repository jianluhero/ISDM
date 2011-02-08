package iamrescue.communication;

import iamrescue.agent.ambulanceteam.ambulancetools.AllocationCodec;
import iamrescue.communication.compression.NullCompressor;
import iamrescue.communication.messages.AgentStuckMessage;
import iamrescue.communication.messages.PingMessage;
import iamrescue.communication.messages.codec.ExampleMessageCodec;
import iamrescue.communication.messages.codec.IMessageCodec;
import iamrescue.communication.messages.codec.updates.EntityDeletedCodec;
import iamrescue.communication.messages.updates.AmbulanceTeamUpdatedMessage;
import iamrescue.communication.messages.updates.BlockadeUpdatedMessage;
import iamrescue.communication.messages.updates.BuildingUpdatedMessage;
import iamrescue.communication.messages.updates.CivilianUpdatedMessage;
import iamrescue.communication.messages.updates.FireBrigadeUpdatedMessage;
import iamrescue.communication.messages.updates.PoliceForceUpdatedMessage;
import iamrescue.communication.messages.updates.RoadUpdatedMessage;

import java.util.ArrayList;
import java.util.Collection;

public class InterAgentMessageDecoderFactory {

	private static InterAgentMessageDecoderFactory instance;

	public static InterAgentMessageDecoderFactory getInstance() {
		if (instance == null) {
			instance = new InterAgentMessageDecoderFactory();
		}

		return instance;
	}

	public InterAgentMessageDecoder create() {
		InterAgentMessageDecoder decoder = new InterAgentMessageDecoder();
		decoder.setCompressor(new NullCompressor());

		Collection<IMessageCodec<?>> standardCodecs = getStandardMessageCodecs();

		for (IMessageCodec<?> iMessageCodec : standardCodecs) {
			decoder.registerCodec(iMessageCodec);
		}

		return decoder;
	}

	private Collection<IMessageCodec<?>> getStandardMessageCodecs() {
		Collection<IMessageCodec<?>> result = new ArrayList<IMessageCodec<?>>();

		result.add(new ExampleMessageCodec());
		result.add(new CivilianUpdatedMessage((short) 1).getCodec());
		result.add(new FireBrigadeUpdatedMessage((short) 1).getCodec());
		result.add(new PoliceForceUpdatedMessage((short) 1).getCodec());
		result.add(new AmbulanceTeamUpdatedMessage((short) 1).getCodec());
		result.add(new BuildingUpdatedMessage((short) 1).getCodec());
		result.add(new RoadUpdatedMessage((short) 1).getCodec());
		result.add(new BlockadeUpdatedMessage((short) 1).getCodec());
		result.add(new AllocationCodec());
		result.add(new EntityDeletedCodec());
		result.add(new PingMessage().getCodec());
		result.add(new AgentStuckMessage(null, null).getCodec());

		return result;
	}
}
