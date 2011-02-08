package iamrescue.communication;

import iamrescue.communication.messages.MessageChannel;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.Validate;


public abstract class AIncomingMessageService implements
		IIncomingMessageService {

	private Set<MessageChannel> subscribedTo = new HashSet<MessageChannel>();

	private int numberOfChannels;

	private ISimulationCommunicationConfiguration configuration;

	public AIncomingMessageService(
			ISimulationCommunicationConfiguration configuration) {
		Validate.notNull(configuration);
		this.numberOfChannels = configuration.getChannelCount();
		this.configuration = configuration;
	}

	public void startListeningToChannel(MessageChannel channel) {
		subscribedTo.add(channel);
	}

	public void stopListeningToChannel(MessageChannel channel) {
		subscribedTo.remove(channel);
	}

	public int getNumberOfChannels() {
		return numberOfChannels;
	}

	public int[] getChannels() {
		int[] channels = new int[subscribedTo.size()];

		int currentIndex = 0;
		for (MessageChannel channel : subscribedTo) {
			channels[currentIndex++] = channel.getChannelNumber();
		}

		return channels;
	}

	@Override
	public Set<MessageChannel> getSubscribedChannels() {
		return subscribedTo;
	}

	@Override
	public int getMaximumNumberofSubscribedChannels() {
		return configuration.getMaxListenChannelCount();
	}
	
	public int getNumberOfVoiceChannels() {
		return configuration.getVoiceChannels().size();
	}
}
