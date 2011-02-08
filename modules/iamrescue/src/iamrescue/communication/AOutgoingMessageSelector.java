package iamrescue.communication;

import iamrescue.communication.failuredetection.SentMessageMemory;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.messages.MessageChannelType;
import iamrescue.communication.messages.filter.IsSentMessageFilter;
import iamrescue.communication.util.ByteArray;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.functors.NotPredicate;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

public abstract class AOutgoingMessageSelector implements
		IOutgoingMessageSelector {

	private IOutgoingMessageService outgoingMessageService;

	private IEncoder encoder;

	private SentMessageMemory memory;

	private static final boolean CONCATENATE_RADIO_MESSAGES = false;

	private static final Logger LOGGER = Logger
			.getLogger(AOutgoingMessageSelector.class);

	public AOutgoingMessageSelector(
			IOutgoingMessageService outgoingMessageService, IEncoder encoder,
			SentMessageMemory memory) {
		this.outgoingMessageService = outgoingMessageService;
		this.encoder = encoder;
		this.memory = memory;
	}

	/**
	 * Will send max 'maximimumMessageCount' messages in the collection to
	 * specified channel
	 * 
	 * @param messages
	 * @param channel
	 * @param maximumMessageCount
	 */
	protected void sendMessages(Collection<Message> messages,
			MessageChannel channel, int maximumMessageCount,
			int maximumMessageSize, int maximumTotalBandwidth,
			int maximumRepetitions, int timeStep) {

		Validate.notNull(messages, "List of messages cannot be null");
		// log.debug("Sending Message: " + messages);
		int messageCount = 0;
		int bandwidthLeft = maximumTotalBandwidth;

		if (messages.size() == 0) {
			return;
		}

		// Always concatenate on voice channel
		boolean voice = channel.getType().equals(MessageChannelType.VOICE);

		boolean concatenate = CONCATENATE_RADIO_MESSAGES || voice;

		// Improve this later!
		boolean failuresPossible = true;// channel.getOverallFailureProbability()
		// > 0;

		// LOGGER.debug("Entering send");

		List<Message> repeated = new FastList<Message>();
		if (failuresPossible && maximumMessageCount > 1) {
			for (Message m : messages) {
				repeated.add(m.copy());
			}
		}
		// List<Message> messageList = messages;

		Map<ByteArray, List<Message>> sentBytes = new FastMap<ByteArray, List<Message>>();

		int repetition = 0;

		LOGGER.info("Repetitions: " + maximumRepetitions + "failures: "
				+ failuresPossible);

		do {
			if (messageCount > 0) {
				// Repeating? Copy from repeated list
				messages = new FastList<Message>();
				for (Message m : repeated) {
					messages.add(m.copy());
				}
			}
			// LOGGER.debug("After do");

			int messageNumber = 0;
			int totalSize = 0;
			StringBuffer sb = null;

			if (LOGGER.isInfoEnabled()) {
				sb = new StringBuffer();
			}

			while (!messages.isEmpty() && messageCount < maximumMessageCount
					&& bandwidthLeft > 0) {

				// LOGGER.debug("In while");
				int available = Math.min(bandwidthLeft, maximumMessageSize);

				// LOGGER.debug("Encode " + messages);
				byte[] encodedMessage;

				Message sentMessage = null;
				if (concatenate) {
					encodedMessage = getEncoder().encodeMessages(messages,
							available);
				} else {
					sentMessage = messages.iterator().next();
					encodedMessage = getEncoder().encodeMessages(
							Collections.singleton(sentMessage), available);
				}

				// LOGGER.debug("Done " + Arrays.toString(encodedMessage));

				if (encodedMessage.length == 0) {
					// no more space
					bandwidthLeft = 0;
				} else {
					getOutgoingMessageService().sendMessage(encodedMessage,
							channel);
					// Remember sent messages
					// if (messageCount == 0) {
					List<Message> sent = new FastList<Message>();
					if (sentMessage != null) {
						sent.add(sentMessage);
						messages.remove(sentMessage);
					} else {
						Iterator<Message> iterator = messages.iterator();
						while (iterator.hasNext()) {
							Message message = iterator.next();
							if (message.isSent()) {
								sent.add(message);
								iterator.remove();
							}
						}
					}
					if (LOGGER.isInfoEnabled()) {
						messageNumber += sent.size();
						if (sb.length() > 0) {
							sb.append(',');
							sb.append(' ');
						}
						sb.append(encodedMessage.length);
						totalSize += encodedMessage.length;
					}
					if (repetition == 0) {
						sentBytes.put(new ByteArray(encodedMessage), sent);
					}
					// } else {
					// filterSentMessages(messages);
					// }
					bandwidthLeft -= encodedMessage.length;
					messageCount++;
				}
			}
			repetition++;
			if (LOGGER.isInfoEnabled()) {
				String prefix = (repetition == 1) ? "" : "(Repeating) ";
				LOGGER.info(prefix + "Encoded " + messageNumber
						+ " messages in " + totalSize + " bytes. Sizes: "
						+ sb.toString());

			}
			// LOGGER.debug("End of loop 1");
		} while (failuresPossible && messageCount < maximumMessageCount
				&& bandwidthLeft > 0 && repetition <= maximumRepetitions);

		if (!voice) {
			memory.setSentMessages(channel, timeStep, sentBytes);
		}
		// LOGGER.debug("End of loop 2");
	}

	protected void filterSentMessages(Collection<Message> messages) {
		CollectionUtils.filter(messages, NotPredicate
				.getInstance(new IsSentMessageFilter()));
	}

	public final void setOutgoingMessageService(
			IOutgoingMessageService outgoingMessageService) {
		this.outgoingMessageService = outgoingMessageService;
	}

	protected IEncoder getEncoder() {
		return encoder;
	}

	protected IOutgoingMessageService getOutgoingMessageService() {
		return outgoingMessageService;
	}
}
