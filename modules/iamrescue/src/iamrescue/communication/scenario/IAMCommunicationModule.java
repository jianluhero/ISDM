package iamrescue.communication.scenario;

import iamrescue.agent.ISimulationTimer;
import iamrescue.communication.CommunicationModule;
import iamrescue.communication.IAMMessagePrioritiser;
import iamrescue.communication.ICommunicationModule;
import iamrescue.communication.IMessagingSchedule;
import iamrescue.communication.ISimulationCommunicationConfiguration;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.messages.MessageChannelType;
import iamrescue.communication.messages.codec.ICommunicationBeliefBaseAdapter;
import iamrescue.communication.scenario.scenarios.DefaultCommunicationScenarioDetector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.connection.Connection;
import rescuecore2.messages.Command;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;

public class IAMCommunicationModule implements ICommunicationModule {

	// Automatically delete all messages beyond this length in each
	// channel (starting with
	// lowest priority messages)
	private static final int MAX_OUTBOX_LENGTH = 50;

	private static final Logger LOGGER = Logger
			.getLogger(IAMCommunicationModule.class);

	private ICommunicationScenarioDetector detector;

	private ICommunicationModule communicationModule;

	private Collection<MessageChannel> vocalChannels;

	private ISimulationTimer timer;

	private ISimulationCommunicationConfiguration configuration;

	private static final String IGNORE_KEY = "kernel.agents.ignoreuntil";

	private List<Message> messagesToOtherTeams = new FastList<Message>();
	private List<Message> messagesToOwnTeam = new FastList<Message>();

	private int ignoreUntil;

	private static final boolean DO_FAULTY_CHANNEL_DETECTION = false;
	private static final int FAULTY_CHANNEL_DETECTION_TIME = 4;
	//private Set<Integer> silentChannels = new FastSet<Integer>();
	// private Set<Integer> previouslySubscribedChannels = new
	// FastSet<Integer>();

	private static final int UNHEARD_CENTRE_REMOVAL_THRESHOLD = 6;

	private Map<EntityID, Integer> unheard = new FastMap<EntityID, Integer>();

	private Set<EntityID> centresToIgnore = new FastSet<EntityID>();

	private IAMMessagePrioritiser messagePrioritiser;

	public IAMCommunicationModule(EntityID id, ISimulationTimer timer,
			ICommunicationBeliefBaseAdapter beliefBase,
			final ISimulationCommunicationConfiguration configuration,
			Connection connection) {
		this.detector = new DefaultCommunicationScenarioDetector(configuration);
		this.timer = timer;
		this.configuration = configuration;
		IMessagingSchedule scheduler = detector.getScenario().getScheduler();
		CommunicationModule commModule = new CommunicationModule(id, timer,
				beliefBase, configuration, connection, scheduler);
		commModule.setMaxOutboxSizePerChannel(MAX_OUTBOX_LENGTH);
		this.communicationModule = commModule;
		this.ignoreUntil = configuration.getConfig().getIntValue(IGNORE_KEY, 3);
		this.messagePrioritiser = new IAMMessagePrioritiser();
		List<MessageChannel> channels = configuration.getChannels();
		/*for (MessageChannel messageChannel : channels) {
			silentChannels.add(messageChannel.getChannelNumber());
		}*/
	}

	/**
	 * @return the configuration
	 */
	public ISimulationCommunicationConfiguration getConfiguration() {
		return configuration;
	}

	public void enqueueVocalMessage(Message message) {
		MessageChannel selected;
		if (getVocalChannels().size() > 1) {
			LOGGER.warn("More than one vocal channel exist. "
					+ "Selecting a random one");
			MessageChannel[] channels = getVocalChannels().toArray(
					new MessageChannel[0]);
			selected = channels[(int) (Math.random() * channels.length)];
		} else {
			selected = getVocalChannels().iterator().next();
		}
		communicationModule.enqueueMessage(message, selected);
	}

	private Collection<MessageChannel> getVocalChannels() {
		if (vocalChannels == null) {
			vocalChannels = new ArrayList<MessageChannel>();

			for (MessageChannel messageChannel : communicationModule
					.getChannels()) {
				if (messageChannel.getType() == MessageChannelType.VOICE) {
					vocalChannels.add(messageChannel);
				}
			}
		}

		return vocalChannels;
	}

	public void enqueueRadioMessageToOtherTeams(Message message) {
		messagePrioritiser.updateMessagePriority(message);
		messagesToOtherTeams.add(message);
	}

	public void enqueueRadioMessageToOwnTeam(Message message) {
		messagePrioritiser.updateMessagePriority(message);
		messagesToOwnTeam.add(message);
	}

	@Override
	public void enqueueMessage(Message message, MessageChannel channel) {
		communicationModule.enqueueMessage(message, channel);
	}

	@Override
	public void flushOutbox() {
		List<MessageChannel> channelsToSubscribeTo = detector.getScenario()
				.getChannelsToSubscribeTo();
		// Check if any are silent
		
		/*if (ignoreUntil + FAULTY_CHANNEL_DETECTION_TIME < timer.getTime()) {
			List<MessageChannel> finalList = new ArrayList<MessageChannel>(
					channelsToSubscribeTo.size());
			List<MessageChannel> substituteList = null;
			for (MessageChannel messageChannel : channelsToSubscribeTo) {
				if (silentChannels.contains(messageChannel.getChannelNumber())) {
					LOGGER.warn("Noticed failure of channel " + messageChannel);
					// Need to replace this with another channel.
					if (substituteList == null) {
						substituteList = new ArrayList<MessageChannel>(
								configuration.getRadioChannels());
						Collections.shuffle(substituteList);
					}
					Iterator<MessageChannel> iterator = substituteList
							.iterator();
					while (iterator.hasNext()) {
						MessageChannel option = iterator.next();
						iterator.remove();
						if (!silentChannels.contains(option)
								&& (!channelsToSubscribeTo.contains(option))) {
							LOGGER.warn("Substituting with " + option);
							finalList.add(option);
							break;
						}
					}
				} else {
					finalList.add(messageChannel);
				}
			}
			channelsToSubscribeTo = finalList;
		} */
		

		subscribeToChannels(channelsToSubscribeTo);
		Map<MessageChannel, List<Message>> messagesAllocation = detector
				.getScenario().distributeMessages(messagesToOwnTeam,
						messagesToOtherTeams, timer);
		for (Entry<MessageChannel, List<Message>> channelEntry : messagesAllocation
				.entrySet()) {
			MessageChannel channel = channelEntry.getKey();
			List<Message> messages = channelEntry.getValue();
			for (Message message : messages) {
				communicationModule.enqueueMessage(message, channel);
			}
		}
		communicationModule.flushOutbox();
		messagesToOtherTeams.clear();
		messagesToOwnTeam.clear();

	}

	@Override
	public Collection<MessageChannel> getChannels() {
		return communicationModule.getChannels();
	}

	@Override
	public Collection<Message> getUnreadMessages() {
		return communicationModule.getUnreadMessages();
	}

	@Override
	public void hear(Collection<Command> heard) {
		communicationModule.hear(heard);
		if (LOGGER.isInfoEnabled()) {
			Collection<Message> unreadMessages = getUnreadMessages();
			StringBuffer sb = new StringBuffer();
			sb.append("Contents of inbox:");
			for (Message message : unreadMessages) {
				sb.append(' ');
				sb.append(message.toShortString() + ",t:"
						+ message.getTimestepReceived());
			}
			LOGGER.info(sb.toString());
		}
		// Did I hear from all my centres?
		if (timer.getTime() > ignoreUntil) {
			Set<EntityID> unheardCentres = new FastSet<EntityID>();
			unheardCentres.addAll(detector.getScenario().getMyCentres());
			unheardCentres.remove(configuration.getEntityID());
			for (Command c : heard) {
				unheardCentres.remove(c.getAgentID());
			}

			// Now go through unheard centres
			if (unheardCentres.size() > 0) {
				Map<EntityID, Integer> newUnheardMap = new FastMap<EntityID, Integer>();

				for (EntityID id : unheardCentres) {
					Integer already = unheard.get(id);
					if (already == null) {
						already = 0;
					}
					int newAlready = already + 1;
					newUnheardMap.put(id, newAlready);
					if (newAlready >= UNHEARD_CENTRE_REMOVAL_THRESHOLD) {
						// Need to remove this
						centresToIgnore.add(id);
					}

					if (newAlready > 1) {
						LOGGER.warn("Have not heard from centre " + id + " "
								+ newAlready + " time steps in a row.");
					} else {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("Have not heard from centre " + id
									+ " this time step.");
						}
					}
				}
				unheard = newUnheardMap;

				if (centresToIgnore.size() > 0) {
					LOGGER.warn("Have not heard some centres ("
							+ centresToIgnore + ") in "
							+ UNHEARD_CENTRE_REMOVAL_THRESHOLD + " time steps."
							+ " Reinitialising team communications.");
					// Reinitialise!
					detector.getScenario().reinitialiseTeam(centresToIgnore);
					// LOGGER.info("New team communication infrastrucutre: " +
					// detector.getScenario().get )
				}
			} else {
				// Heard all
				if (unheard.size() > 0) {
					unheard.clear();
				}
			}

			/*for (Command command : heard) {
				if (command instanceof AKSpeak) {
					AKSpeak speak = (AKSpeak) command;
					int channel = speak.getChannel();
					silentChannels.remove(channel);
				}
			}*/
		}

	}

	@Override
	public boolean isRadioCommunicationPossible() {
		return communicationModule.isRadioCommunicationPossible();
	}

	@Override
	public void enqueueMessage(Message message, List<MessageChannel> channels) {
		communicationModule.enqueueMessage(message, channels);
	}

	@Override
	public void subscribeToChannels(List<MessageChannel> channels) {
		// if (timer.getTime() < ignoreUntil) {
		communicationModule.subscribeToChannels(channels);
		// }
	}

	public boolean amICentre() {
		return detector.getScenario().amICentre();
	}

	public List<MessageChannel> getChannelsToOwnTeam() {
		return detector.getScenario().getChannelsToOwnTeam();
	}
}
