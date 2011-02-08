package iamrescue.communication;

import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.messages.filter.IsSentMessageFilter;
import iamrescue.communication.messages.filter.NonZeroTTLFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.functors.AndPredicate;
import org.apache.commons.collections15.functors.NotPredicate;

/**
 * A class that is responsible for storing outgoing messages, without any
 * intelligence.
 * 
 * @author Ruben Stranders
 * 
 */
public class Outbox {

	// /**
	// * A queue of messages to be shouted
	// */
	// private List<Message> shoutMessageQ = new ArrayList<Message>();

	/**
	 * For each channel, a queue of messages to be sent over radio
	 */
	private Map<MessageChannel, List<Message>> messageQs = new HashMap<MessageChannel, List<Message>>();

	private Map<Integer, MessageChannel> channelNumbers = new HashMap<Integer, MessageChannel>();

	private boolean sorted = false;

	// private static Log log = LogFactory.getLog(Outbox.class);

	/**
	 * Sorts messages based on priority. Messages with high priority go at the
	 * start of the list
	 */
	private static Comparator<Message> messagePriorityComparator = new Comparator<Message>() {
		public int compare(Message o1, Message o2) {
			return o2.getPriority().compareTo(o1.getPriority());
		}
	};

	private static Predicate<Message> retentionFilter = AndPredicate
			.getInstance(new NonZeroTTLFilter(), NotPredicate
					.getInstance(new IsSentMessageFilter()));

	public Outbox(List<MessageChannel> channels) {
		initializeChannels(channels);
	}

	/**
	 * Enqueues a message for transmission
	 */
	public void enqueueMessage(Message message, MessageChannel channel) {
		sorted = false;
		messageQs.get(channel).add(message);
	}

	public Collection<Message> getMessageQ(MessageChannel channel) {
		List<Message> list = messageQs.get(channel);
		if (!sorted) {
			Collections.sort(list, messagePriorityComparator);
		}
		// Mark as dirty, since other parts of the code modify this.
		sorted = false;
		return list;
	}

	public void removeSentAndStaleMessages() {
		// decrease TTL for all remaining messages by 1
		for (List<Message> messageQ : messageQs.values()) {
			for (Message message : messageQ) {
				message.setTTL(message.getTTL() - 1);
			}
		}

		for (List<Message> messageQ : messageQs.values()) {
			Iterator<Message> iterator = messageQ.iterator();
			while (iterator.hasNext()) {
				Message message = iterator.next();
				if (message.getTTL() <= 0 || message.isSent()) {
					iterator.remove();
				}
			}
		}
	}

	private void initializeChannels(List<MessageChannel> channels) {
		for (MessageChannel channel : channels) {
			messageQs.put(channel, new ArrayList<Message>());
			channelNumbers.put(channel.getChannelNumber(), channel);
		}
	}

	public Map<MessageChannel, List<Message>> getMessageQs() {
		sortByPriority();
		// Mark as dirty, since other parts of the code modify this.
		sorted = false;
		return messageQs;
	}

	public Set<MessageChannel> getChannels() {
		return messageQs.keySet();
	}

	public Object getMessagesToString(boolean verbose) {

		sortByPriority();

		StringBuilder builder = new StringBuilder();

		for (MessageChannel queue : messageQs.keySet()) {
			builder.append("\nQueue: ");
			builder.append(queue.getChannelNumber());
			builder.append(':');
			if (verbose) {
				builder.append('\n');
			}
			
			for (Message message : messageQs.get(queue)) {
				builder.append("    ");
				if (verbose) {
					builder.append(message.toString());
					builder.append('\n');
				} else {
					builder.append(message.toShortString());
					builder.append(',');
				}
			}
		}

		return builder.toString();
	}

	public MessageChannel getChannel(int channelNumber) {
		return channelNumbers.get(channelNumber);
	}

	public void sortByPriority() {
		if (!sorted) {
			for (List<Message> radioMessageQ : messageQs.values()) {
				Collections.sort(radioMessageQ, messagePriorityComparator);
			}
			sorted = true;
		}
	}
}
