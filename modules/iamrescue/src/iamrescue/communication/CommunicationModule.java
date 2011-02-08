package iamrescue.communication;

import iamrescue.agent.ISimulationTimer;
import iamrescue.communication.compression.NullCompressor;
import iamrescue.communication.failuredetection.SentMessageMemory;
import iamrescue.communication.failuredetection.SentMessageMemory.SentMessages;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.messages.MessageChannelType;
import iamrescue.communication.messages.codec.ICommunicationBeliefBaseAdapter;
import iamrescue.communication.messages.codec.UnknownMessageFormatException;
import iamrescue.communication.messages.updates.EntityUpdatedMessage;
import iamrescue.communication.util.ByteArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import rescuecore2.connection.Connection;
import rescuecore2.messages.Command;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

/**
 * This class is a container for the sender and the receiver
 * 
 */
public class CommunicationModule implements ICommunicationModule {

	private int maxOutboxSizePerChannel = -1;

	// a list of decoders, in the order in which they are used to attempt to
	// decode a message
	private List<IDecoder> decoders = new ArrayList<IDecoder>();

	private IEncoder encoder;

	private Outbox outbox;

	private Inbox inbox = new Inbox();

	private IOutgoingMessageSelector outgoingMessageSelector;

	private static final Logger LOGGER = Logger
			.getLogger(CommunicationModule.class);

	private IIncomingMessageService incomingMessageService;

	private boolean receivedRadioTransmission = false;

	private ISimulationTimer timer;

	private EntityID id;

	private ICommunicationBeliefBaseAdapter beliefBase;

	private SentMessageMemory sentMessageMemory;

	private int lastTime = -1;
	private int lastChannel = -1;
	private SentMessages lastSentMessages = null;

	public CommunicationModule(EntityID id, ISimulationTimer timer,
			ICommunicationBeliefBaseAdapter beliefBase,
			final ISimulationCommunicationConfiguration configuration,
			Connection connection, IMessagingSchedule messageScheduler) {
		this.id = id;
		this.timer = timer;
		// the beliefbase connection is necessary to load entities from the
		// belief base, such that their id's are encoded more efficiently
		this.beliefBase = beliefBase;
		sentMessageMemory = new SentMessageMemory();

		outbox = new Outbox(configuration.getChannels());

		encoder = new Encoder(new NullCompressor(), beliefBase);

		// The InterAgentMessageDecoder decodes messages exchanged between *our*
		// agents
		addDecoder(InterAgentMessageDecoderFactory.getInstance().create());

		// This decoder decodes messages received from entities other than our
		// agents
		addDecoder(new ExternalMessageDecoder());

		// The IncomingMessageService is the 'physical' wire along which
		// messages are received. The incoming message service calls the
		// processIncomingMessage method for processing the raw data
		incomingMessageService = new RescueCore2IncomingMessageService(id,
				timer, this, connection, configuration);

		// The OutgoingMessageService is the physical outgoing wire.
		IOutgoingMessageService outgoingMessageService = new RescueCore2OutgoingMessageService(
				id, connection, timer);

		// The outgoing message selector selects which messages are sent along
		// which channel. It uses the encoder to encode the messages into a byte
		// stream.
		outgoingMessageSelector = new ScheduledOutgoingMessageService(timer,
				outgoingMessageService, encoder, messageScheduler,
				sentMessageMemory);
	}

	private void addDecoder(IDecoder decoder) {
		Validate.notNull(decoder);
		decoders.add(decoder);
	}

	public void setMaxOutboxSizePerChannel(int maxOutboxSizePerChannel) {
		this.maxOutboxSizePerChannel = maxOutboxSizePerChannel;
	}

	public void enqueueMessage(Message message, MessageChannel channel) {
		outbox.enqueueMessage(message, channel);
	}

	public Set<MessageChannel> getChannels() {
		return outbox.getChannels();
	}

	public MessageChannel getChannels(int channelNumber) {
		return outbox.getChannel(channelNumber);
	}

	public void flushOutbox() {
		// outgoingMessageSelector.sendShoutMessages(outbox.getShoutMessageQ());
		reenqueueFailedMessages();
		filterOutInconsistentUpdates();
		consolidateUpdates();

		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Messages before sending:");
			LOGGER.debug(outbox.getMessagesToString(true));
		} else if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Messages before sending:");
			LOGGER.info(outbox.getMessagesToString(false));
		}

		outgoingMessageSelector.sendMessages(outbox.getMessageQs());

		inbox.removeReadMessages();
		outbox.removeSentAndStaleMessages();

		// LOGGER.info("Messages after sending");
		// LOGGER.info(outbox.getMessagesToString());

		// flush all subscribe commands that ensure we listen to the correct
		// channels
		incomingMessageService.flushChannelCommands();

		// Now prune messages
		if (maxOutboxSizePerChannel >= 0) {
			Map<MessageChannel, List<Message>> messageQs = outbox
					.getMessageQs();
			for (Entry<MessageChannel, List<Message>> entry : messageQs
					.entrySet()) {
				List<Message> list = entry.getValue();
				while (list.size() > maxOutboxSizePerChannel) {
					list.remove(list.size() - 1);
				}
			}
		}
	}

	// TODO: This should probably be moved elsewhere, since it's inference.
	private void consolidateUpdates() {
		Map<MessageChannel, List<Message>> messageQs = outbox.getMessageQs();
		Set<MessageChannel> channels = outbox.getChannels();

		for (MessageChannel channel : channels) {
			Map<Pair<EntityID, Integer>, EntityUpdatedMessage> messageMap = new FastMap<Pair<EntityID, Integer>, EntityUpdatedMessage>();
			List<Message> msgList = messageQs.get(channel);
			Iterator<Message> messageIterator = msgList.iterator();
			while (messageIterator.hasNext()) {
				Message m = messageIterator.next();
				if (m instanceof EntityUpdatedMessage) {
					messageIterator.remove();
					EntityUpdatedMessage update = (EntityUpdatedMessage) m;
					Pair<EntityID, Integer> index = new Pair<EntityID, Integer>(
							update.getObjectID(), (int) update.getTimestamp());
					EntityUpdatedMessage existingMessage = messageMap
							.get(index);
					if (existingMessage != null) {
						Set<String> changedProperties = update
								.getChangedProperties();
						for (String propertyURN : changedProperties) {
							if (!existingMessage.getChangedProperties()
									.contains(propertyURN)) {
								existingMessage.addUpdatedProperty(update
										.getProperty(propertyURN));
							}
						}
					} else {
						// Need to add this message
						messageMap.put(index, update);
					}
				}
			} // End of while through messages
			Set<Entry<Pair<EntityID, Integer>, EntityUpdatedMessage>> entrySet = messageMap
					.entrySet();
			// Now re-enqueue consolidated messages
			for (Entry<Pair<EntityID, Integer>, EntityUpdatedMessage> entry : entrySet) {
				msgList.add(entry.getValue());
			}
		}
	}

	// TODO: This should probably be moved elsewhere, since it's inference.
	private void filterOutInconsistentUpdates() {
		Map<MessageChannel, List<Message>> messageQs = outbox.getMessageQs();

		Set<MessageChannel> channels = outbox.getChannels();

		for (MessageChannel channel : channels) {
			List<Message> msgList = messageQs.get(channel);
			Iterator<Message> messageIterator = msgList.iterator();
			while (messageIterator.hasNext()) {
				Message m = messageIterator.next();
				if (m instanceof EntityUpdatedMessage) {
					EntityUpdatedMessage message = (EntityUpdatedMessage) m;

					short timestamp = message.getTimestamp();
					if (timestamp < timer.getTime()) {
						Entity entity = beliefBase.getObjectByID(message
								.getObjectID().getValue());

						if (entity == null) {
							// This entity has been deleted!
							messageIterator.remove();
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Message " + message
										+ " is about a deleted object "
										+ "and has been removed.");
							}
						} else {

							// This is potentially out of date
							Collection<Property> properties = message
									.getProperties();
							List<String> toRemove = new FastList<String>();
							for (Property p : properties) {
								Property currentProperty = entity.getProperty(p
										.getURN());
								boolean same;
								if (!currentProperty.isDefined()) {
									if (!p.isDefined()) {
										same = true;
									} else {
										same = false;
									}
								} else {
									if (!p.isDefined()) {
										same = false;
									} else {
										if (currentProperty.getValue() instanceof int[]) {
											same = Arrays.equals(
													(int[]) currentProperty
															.getValue(),
													(int[]) p.getValue());
										} else {
											same = currentProperty.getValue()
													.equals(p.getValue());
										}
									}
								}

								if (!same) {
									// Not the same!
									toRemove.add(p.getURN());
									if (LOGGER.isTraceEnabled()) {
										LOGGER
												.trace("Removing property "
														+ p
														+ ", because current property is "
														+ currentProperty);
									}
								} else {
									// Also check if hp is needed
									if (p.isDefined()
											&& p.getURN().equals(
													StandardPropertyURN.HP)) {
										// We're only interested in HP if agent
										// is buried
										if (entity instanceof Human) {
											Human h = (Human) entity;
											if (!h.isBuriednessDefined()
													|| !(h.getBuriedness() > 0)) {
												toRemove.add(p.getURN());
											}
										} else {
											toRemove.add(p.getURN());
										}
									}
								}
							}
							for (String removeURN : toRemove) {
								message.removeUpdatedProperty(removeURN);
							}
							// Is message empty now?
							if (message.getProperties().size() == 0) {
								messageIterator.remove();
								if (LOGGER.isTraceEnabled()) {
									LOGGER.trace("Message " + message
											+ " is now empty "
											+ "and has been removed.");
								}
							}
						}
					}
				}
			}
		}
	}

	public Collection<Message> getUnsentMessages(MessageChannel channel) {
		return outbox.getMessageQs().get(channel);
	}

	public Collection<Message> getUnreadMessages() {
		return inbox.getUnreadMessages();
	}

	protected void processIncomingMessage(EntityID senderAgentID,
			MessageChannel channel, int timestep, byte[] messageContents) {
		Validate.isTrue(!decoders.isEmpty(),
				"No decoders registered, cannot decode messages");

		if (lastTime != timestep || channel.getChannelNumber() != lastChannel
				|| lastSentMessages == null) {
			lastSentMessages = sentMessageMemory.getSentMessages(channel);
			lastTime = timestep;
			lastChannel = channel.getChannelNumber();
		}

		if (channel.getType() == MessageChannelType.RADIO) {
			receivedRadioTransmission = true;
		}

		if (messageContents.length == 0) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Drop-out message found from " + senderAgentID
						+ " on channel " + channel);
			}
			// if (senderAgentID.equals(id)) {
			// My own message dropped out
			// if (channel.getInputDropoutProbability() == 0) {
			// No input drop-out possible - must have dropped out on
			// output. This is ok. Means message was sent.
			// sentMessageMemory.clear(channel.getChannelNumber());
			// }
			// }
			if (beliefBase.isRescueEntity(senderAgentID)) {
				// No need to do anything else
				return;
			}
		}

		boolean decoded = false;
		List<UnknownMessageFormatException> errors = new ArrayList<UnknownMessageFormatException>();

		// try to decode the message with each decoder
		for (IDecoder decoder : decoders) {
			if (!decoder.canDecode(senderAgentID, channel, timestep,
					messageContents, beliefBase)) {
				continue;
			}

			try {

				List<Message> messages = decoder.decode(senderAgentID, channel,
						timestep, messageContents, beliefBase);

				for (Message message : messages) {

					// communicatedWithAgents.add(message.getSenderAgentID());

					// if this message has not been received from myself
					if (!message.getSenderAgentID().equals(id)) {
						inbox.addMessage(message);
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Added message to inbox: " + message);
						}
					} else {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Ignored message from myself: "
									+ message);
						}
						if (lastSentMessages != null) {
							lastSentMessages.received(new ByteArray(
									messageContents));
						}
						// sentMessageMemory.clear(channel.getChannelNumber());

						// if (log.isInfoEnabled())
						// log.info("Received message from "
						// + message.getSenderAgentID());
					}
				}

				decoded = true;
				break;
			} catch (UnknownMessageFormatException e) {
				if (LOGGER.isTraceEnabled())
					LOGGER.trace("Could not decode the message with decoder "
							+ decoder.getClass());

				errors.add(e);
			}
		}

		if (!decoded) {
			LOGGER.error("Message could not be decoded with any available"
					+ " message decoder. Contents: "
					+ ArrayUtils.toString(messageContents));
			LOGGER.error("Errors: ");
			for (UnknownMessageFormatException error : errors) {
				LOGGER.error(error);
			}
			LOGGER.error("Message origin: " + senderAgentID);
			LOGGER.error("Channel       : " + channel);
			LOGGER.error("Time step     : " + timestep);
		}
	}

	public Collection<Message> getUnreadMessages(IMessageFilter messageFilter) {
		return inbox.getUnreadMessages(messageFilter);
	}

	public Outbox getOutbox() {
		return outbox;
	}

	public boolean isRadioCommunicationPossible() {
		// TODO check
		return receivedRadioTransmission || timer.getTime() <= 8;
	}

	@Override
	public void hear(Collection<Command> heard) {
		incomingMessageService.hear(heard);
	}

	@Override
	public void enqueueMessage(Message message, List<MessageChannel> channels) {
		for (MessageChannel channel : channels) {
			enqueueMessage(message, channel);
		}
	}

	public void reenqueueFailedMessages() {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Reenqueuing failed messages");
		}
		Set<Integer> sentChannels = sentMessageMemory.getSentChannels();
		for (Integer channel : sentChannels) {
			SentMessages sentMessages = sentMessageMemory
					.getSentMessages(channel);
			Collection<Message> messages = sentMessages.getMessages();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Failed messages for channel " + channel + ": "
						+ messages);
			}
			if (incomingMessageService.getSubscribedChannels().contains(
					sentMessages.getChannel())) {
				for (Message message : messages) {
					// Reduce TTL, because it was sent last time step
					message.setTTL(message.getTTL() - 1);
					if (message.getTTL() > 0) {
						if (message.getRepeats() < 0) {
							message.setRepeats(1);
						} else {
							message.setRepeats(message.getRepeats() + 1);
						}
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("Re-sending failed message: "
									+ message);
							enqueueMessage(message, sentMessages.getChannel());
						}
					}
				}
			} else {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Ignoring these because "
							+ "not subscribed to channel.");
				}
			}
		}
		sentMessageMemory.clearAll();
	}

	public void subscribeToChannels(List<MessageChannel> channels) {
		for (MessageChannel channel : channels) {
			incomingMessageService.startListeningToChannel(channel);
		}
	}

	/*
	 * public static void main(String[] args) { List<Message> messages = new
	 * ArrayList<Message>(); messages.add(new PingMessage()); messages.add(new
	 * PingMessage()); messages.add(new PingMessage()); messages.add(new
	 * PingMessage()); messages.add(new PingMessage()); messages.add(new
	 * PingMessage()); messages.get(0).setPriority(MessagePriority.HIGH);
	 * messages.get(1).setPriority(MessagePriority.VERY_HIGH);
	 * messages.get(2).setPriority(MessagePriority.CRITICAL);
	 * messages.get(3).setPriority(MessagePriority.VERY_LOW);
	 * messages.get(4).setPriority(MessagePriority.LOW);
	 * messages.get(5).setPriority(MessagePriority.NORMAL);
	 * System.out.println("Before: " + messages);
	 * Collections.sort(messages,Outbox.messagePriorityComparator);
	 * System.out.println("After: " + messages); }
	 */
}
