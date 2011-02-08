package iamrescue.communication.scenario;

import iamrescue.agent.ISimulationTimer;
import iamrescue.communication.CommunicationModule;
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
import java.util.List;

import org.apache.log4j.Logger;

import rescuecore2.connection.Connection;
import rescuecore2.messages.Command;
import rescuecore2.worldmodel.EntityID;

public class ScenarioAwareCommunicationModule implements ICommunicationModule {

	private static final Logger LOGGER = Logger
			.getLogger(ScenarioAwareCommunicationModule.class);
	private ICommunicationScenarioDetector detector;

	private ICommunicationModule communicationModule;

	private Collection<MessageChannel> vocalChannels;

	private List<MessageChannel> lastChannelsSubscribed = null;

	private ISimulationTimer timer;

	public ScenarioAwareCommunicationModule(EntityID id,
			ISimulationTimer timer, ICommunicationBeliefBaseAdapter beliefBase,
			final ISimulationCommunicationConfiguration configuration,
			Connection connection) {
		this.detector = new DefaultCommunicationScenarioDetector(configuration);
		this.timer = timer;
		IMessagingSchedule scheduler = detector.getScenario().getScheduler();
		communicationModule = new CommunicationModule(id, timer, beliefBase,
				configuration, connection, scheduler);
	}

	public void enqueueVocalMessage(Message message) {
		if (getVocalChannels().size() > 1) {
			LOGGER.warn("More than one vocal channel exist. "
					+ "Selecting a random one");
		}

		MessageChannel first = getVocalChannels().iterator().next();
		communicationModule.enqueueMessage(message, first);
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

	public void enqueueRadioMessageToCenter(Message message) {
		List<MessageChannel> channelsToCenters = detector.getScenario()
				.getChannelsToOtherTeams();
		enqueueMessage(message, channelsToCenters);
	}

	public void enqueueRadioMessageToPlatoons(Message message) {
		List<MessageChannel> channelsToCenters = detector.getScenario()
				.getChannelsToOwnTeam();
		enqueueMessage(message, channelsToCenters);
	}

	@Override
	public void enqueueMessage(Message message, MessageChannel channel) {
		communicationModule.enqueueMessage(message, channel);
	}

	@Override
	public void flushOutbox() {
		subscribeToChannels(detector.getScenario().getChannelsToSubscribeTo());
		communicationModule.flushOutbox();
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
				sb.append(message.toShortString());
			}
			LOGGER.info(sb.toString());
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
		if (timer.getTime() <= 3) {
			communicationModule.subscribeToChannels(channels);
		}
	}
}
