package iamrescue.communication;

import iamrescue.agent.ISimulationTimer;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannelConfiguration;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javolution.util.FastMap;

import org.apache.log4j.Logger;

import rescuecore2.connection.Connection;
import rescuecore2.connection.ConnectionException;
import rescuecore2.messages.Command;
import rescuecore2.standard.messages.AKSay;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.standard.messages.AKSubscribe;
import rescuecore2.standard.messages.AKTell;
import rescuecore2.worldmodel.EntityID;

public class RescueCore2IncomingMessageService extends AIncomingMessageService {

	private CommunicationModule module;
	private ISimulationTimer timer;
	private EntityID id;
	private Connection connection;
	private Map<Integer, Integer> sizes = new FastMap<Integer, Integer>();
	private MessageChannelConfiguration channelConfiguration;

	public RescueCore2IncomingMessageService(EntityID id,
			ISimulationTimer timer, CommunicationModule module,
			Connection connection,
			ISimulationCommunicationConfiguration configuration) {
		super(configuration);

		this.module = module;
		this.timer = timer;
		this.id = id;
		this.connection = connection;
		channelConfiguration = configuration.getChannelConfiguration();
	}

	private static final Logger LOGGER = Logger
			.getLogger(RescueCore2IncomingMessageService.class);

	public void flushChannelCommands() {
		if (getSubscribedChannels().size() + getNumberOfVoiceChannels() < getMaximumNumberofSubscribedChannels()) {
			LOGGER.warn("Listening to " + getSubscribedChannels().size()
					+ " channels, but maximum allowable is "
					+ getMaximumNumberofSubscribedChannels());
		}

		if (getSubscribedChannels().size() + getNumberOfVoiceChannels() > getMaximumNumberofSubscribedChannels()) {
			LOGGER.warn("Attempting to listening to "
					+ getSubscribedChannels().size()
					+ " channels, but maximum allowable is "
					+ getMaximumNumberofSubscribedChannels());
		}

		LOGGER.debug("Sending AKSubscribe Command "
				+ Arrays.toString(getChannels()));

		try {
			connection.sendMessage(new AKSubscribe(id, timer.getTime(),
					getChannels()));
		} catch (ConnectionException e) {
			e.printStackTrace();
		}
	}

	private void updateSizes(int channel, int size) {
		Integer prevSize = sizes.get(channel);
		if (prevSize == null) {
			prevSize = 0;
		}
		sizes.put(channel, prevSize + size);
	}

	public void hear(Collection<Command> heard) {

		if (LOGGER.isInfoEnabled()) {
			sizes = FastMap.newInstance();
		}
		for (Command c : heard) {
			if (c instanceof AKSpeak) {
				AKSpeak say = (AKSpeak) c;
				module.processIncomingMessage(say.getAgentID(),
						channelConfiguration.get(say.getChannel()), say
								.getTime(), say.getContent());
				if (LOGGER.isInfoEnabled()) {
					updateSizes(say.getChannel(), say.getContent().length);
				}
			} else if (c instanceof AKSay) {
				AKSay say = (AKSay) c;
				// it is assumed that the verbal message channel is at channel 0
				module.processIncomingMessage(say.getAgentID(),
						channelConfiguration.get(0), say.getTime(), say
								.getContent());
				if (LOGGER.isInfoEnabled()) {
					updateSizes(0, say.getContent().length);
				}
			} else if (c instanceof AKTell) {
				AKTell say = (AKTell) c;
				// TODO find out what AKTell is
				module.processIncomingMessage(say.getAgentID(),
						channelConfiguration.get(0), say.getTime(), say
								.getContent());
				if (LOGGER.isInfoEnabled()) {
					updateSizes(0, say.getContent().length);
				}
			} else {
				assert false : "Unexpected message type";
			}
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Heard " + heard.size() + " messages.");
			LOGGER.trace("There are " + module.getUnreadMessages().size()
					+ " unread messages");

			for (Message msg : module.getUnreadMessages()) {
				LOGGER.trace(msg.toString());
			}

			LOGGER.trace("Done");
		}
		if (LOGGER.isInfoEnabled()) {
			StringBuffer sb = new StringBuffer();
			sb.append("Total bytes received:");
			for (Entry<Integer, Integer> entry : sizes.entrySet()) {
				sb.append(" ( ");
				sb.append(entry.getKey());
				sb.append(" -> ");
				sb.append(entry.getValue());
				sb.append(" )");
			}
			LOGGER.info(sb.toString());
		}
	}
}
