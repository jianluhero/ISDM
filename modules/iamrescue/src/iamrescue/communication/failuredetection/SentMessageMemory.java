package iamrescue.communication.failuredetection;

import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.util.ByteArray;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;
import javolution.util.FastSet;

import org.apache.log4j.Logger;

public class SentMessageMemory {
	// private static final List<Message> EMPTY_LIST = new FastList<Message>();
	private Map<Integer, SentMessages> sentMessages = new FastMap<Integer, SentMessages>();

	// private Map<Integer, Integer> timeStepSent = new FastMap<Integer,
	// Integer>();

	private static final Logger LOGGER = Logger
			.getLogger(SentMessageMemory.class);

	public void setSentMessages(MessageChannel channel, int timeStep,
			Map<ByteArray, List<Message>> messages) {
		sentMessages.put(channel.getChannelNumber(), new SentMessages(channel,
				timeStep, messages));
	}

	public SentMessages getSentMessages(MessageChannel channel) {
		return sentMessages.get(channel.getChannelNumber());
	}

	public SentMessages getSentMessages(int channelID) {
		return sentMessages.get(channelID);
	}

	public static class SentMessages {
		private MessageChannel channel;
		private int timeStep;
		private Map<ByteArray, List<Message>> messages;

		public SentMessages(MessageChannel channel, int timeStep,
				Map<ByteArray, List<Message>> messages) {
			super();
			this.channel = channel;
			this.timeStep = timeStep;
			this.messages = messages;
		}

		public MessageChannel getChannel() {
			return channel;
		}

		public int getTimeStep() {
			return timeStep;
		}

		public Collection<Message> getMessages() {
			Set<Message> messageSet = new FastSet<Message>();
			for (List<Message> messageList : messages.values()) {
				for (Message message : messageList) {
					messageSet.add(message);
				}
			}
			return messageSet;
		}

		public void received(ByteArray byteArray) {
			messages.remove(byteArray);
		}
	}

	public void clear(int channelNumber) {
		sentMessages.remove(channelNumber);
	}

	public Set<Integer> getSentChannels() {
		return sentMessages.keySet();
	}

	public void clearAll() {
		sentMessages.clear();
	}
}