package iamrescue.communication.messages;

import java.util.Map;

import javolution.util.FastMap;

import org.apache.log4j.Logger;

public class MessageChannelConfiguration {
	private static final Logger LOGGER = Logger
			.getLogger(MessageChannelConfiguration.class);
	private Map<Integer, MessageChannel> channels = new FastMap<Integer, MessageChannel>();

	public void put(MessageChannel channel) {
		if (channels.containsKey(channel.getChannelNumber())) {
			LOGGER.warn("Overwriting existing channel "
					+ channels.get(channel.getChannelNumber()) + " with "
					+ channel);
		}
		channels.put(channel.getChannelNumber(), channel);
	}

	public MessageChannel get(int number) {
		return channels.get(number);
	}
}