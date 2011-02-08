package iamrescue.communication;

import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.messages.MessageChannelType;

import java.util.ArrayList;
import java.util.List;

import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;


public abstract class ASimulationCommunicationConfiguration implements
		ISimulationCommunicationConfiguration {

	private ArrayList<MessageChannel> radioChannels;
	private ArrayList<MessageChannel> voiceChannels;
	private EntityID id;
	private StandardEntityURN type;

	public ASimulationCommunicationConfiguration(EntityID id,
			StandardEntityURN myType) {
		this.id = id;
		this.type = myType;
	}

	@Override
	public EntityID getEntityID() {
		return id;
	}

	@Override
	public final List<MessageChannel> getRadioChannels() {
		if (radioChannels == null) {
			radioChannels = new ArrayList<MessageChannel>();

			for (MessageChannel messageChannel : getChannels()) {
				if (messageChannel.getType() == MessageChannelType.RADIO) {
					radioChannels.add(messageChannel);
				}
			}
		}

		return radioChannels;
	}

	@Override
	public final List<MessageChannel> getVoiceChannels() {
		if (voiceChannels == null) {
			voiceChannels = new ArrayList<MessageChannel>();

			for (MessageChannel messageChannel : getChannels()) {
				if (messageChannel.getType() == MessageChannelType.VOICE) {
					voiceChannels.add(messageChannel);
				}
			}
		}

		return voiceChannels;
	}

	@Override
	public final List<MessageChannel> getRadioChannels(int... indices) {
		List<MessageChannel> result = new ArrayList<MessageChannel>();

		for (int index : indices) {
			result.add(getRadioChannels().get(index));
		}

		return result;
	}

	@Override
	public MessageChannel getRadioChannel(int i) {
		return getRadioChannels().get(i);
	}

	@Override
	public final int getMaxListenChannelCount() {
		switch (type) {
		case AMBULANCE_TEAM:
		case FIRE_BRIGADE:
		case POLICE_FORCE:
			return getMaxListenChannelCountPlatoon();
		case AMBULANCE_CENTRE:
		case FIRE_STATION:
		case POLICE_OFFICE:
			return getMaxListenChannelCountCentre();
		}

		throw new IllegalArgumentException("Type unknown");
	}

	@Override
	public final StandardEntityURN getAgentType() {
		return type;
	}

	@Override
	public int getRadioChannelCount() {
		return getRadioChannels().size();
	}
}
