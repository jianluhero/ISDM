package iamrescue.communication.scenario.scenarios;

import iamrescue.agent.ISimulationTimer;
import iamrescue.communication.IMessagingSchedule;
import iamrescue.communication.ISimulationCommunicationConfiguration;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.scenario.ICommunicationScenario;
import iamrescue.util.EntityComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public class CompleteCoverageScenario implements ICommunicationScenario {

	private ISimulationCommunicationConfiguration configuration;

	private MessageChannel myChannel;

	private int totalAgentsOnChannel;

	private SharedBandwidthSchedule myScheduler;
	Map<MessageChannel, List<Message>> map = new FastMap<MessageChannel, List<Message>>();

	private static final Logger LOGGER = Logger
			.getLogger(CompleteCoverageScenario.class);

	public CompleteCoverageScenario(
			ISimulationCommunicationConfiguration configuration) {
		this.configuration = configuration;

		// Divide channels equally between all agents
		List<MessageChannel> channels = new ArrayList<MessageChannel>(
				configuration.getRadioChannels());

		Collections.sort(channels, new Comparator<MessageChannel>() {
			@Override
			public int compare(MessageChannel o1, MessageChannel o2) {
				return Double.compare(o1.getChannelNumber(), o2
						.getChannelNumber());
			}
		});

		// Bandwidth per channel
		int bandwidths[] = new int[channels.size()];

		// number of agents assigned to channel
		int assigned[] = new int[channels.size()];

		int totalBandwidth = 0;

		for (int i = 0; i < channels.size(); i++) {
			MessageChannel messageChannel = channels.get(i);
			bandwidths[i] = messageChannel.getBandwidth();
			assigned[i] = 0;
			totalBandwidth += messageChannel.getBandwidth();
		}

		List<StandardEntity> agents = new ArrayList<StandardEntity>();
		Map<StandardEntityURN, Collection<StandardEntity>> agentsByType = configuration
				.getAgentsByType();
		for (Entry<StandardEntityURN, Collection<StandardEntity>> entry : agentsByType
				.entrySet()) {
			Collection<StandardEntity> theseAgents = entry.getValue();
			agents.addAll(theseAgents);
		}

		int numAgents = agents.size();

		// Which channel are they assigned to?
		int[] assignedTo = new int[numAgents];

		Collections.sort(agents, new EntityComparator());

		// Greedily add agents to channels
		int myIndex = -1;
		for (int i = 0; i < assignedTo.length; i++) {
			int bestChannel = -1;
			int bestBandwidth = -1;
			for (int j = 0; j < bandwidths.length; j++) {
				int newBandwidth;
				if (assigned[j] == 0) {
					newBandwidth = bandwidths[j];
				} else {
					newBandwidth = assigned[j] * bandwidths[j]
							/ (assigned[j] + 1);
				}
				if (newBandwidth > bestBandwidth) {
					bestBandwidth = newBandwidth;
					bestChannel = j;
				}
			}
			assignedTo[i] = bestChannel;
			bandwidths[bestChannel] = bestBandwidth;
			assigned[bestChannel]++;
			if (agents.get(i).getID().equals(configuration.getEntityID())) {
				// Done myself
				myChannel = channels.get(bestChannel);
				myIndex = i;
			}
		}
		totalAgentsOnChannel = assigned[assignedTo[myIndex]];
		myScheduler = new SharedBandwidthSchedule(myChannel,
				totalAgentsOnChannel);
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Assigned channel " + myChannel + " to "
					+ configuration.getEntityID() + ". This is shared with "
					+ totalAgentsOnChannel
					+ " in total, resulting in a bandwidth of "
					+ myScheduler.getAllocatedMessagesSize(myChannel, 1)
					+ " bytes each, maximum "
					+ myScheduler.getAllocatedMessagesCount(myChannel, 0)
					+ " messages and size "
					+ myScheduler.getAllocatedMessagesSize(myChannel, 0));
		}
	}

	@Override
	public List<MessageChannel> getChannelsToOtherTeams() {
		return Collections.singletonList(myChannel);
	}

	@Override
	public List<MessageChannel> getChannelsToOwnTeam() {
		return Collections.singletonList(myChannel);
	}

	@Override
	public List<MessageChannel> getChannelsToSubscribeTo() {
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		channels.addAll(configuration.getRadioChannels());
		return channels;
	}

	@Override
	public StandardEntityURN getMyRole() {
		return configuration.getAgentType();
	}

	@Override
	public IMessagingSchedule getScheduler() {
		return myScheduler;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.scenario.ICommunicationScenario#distributeMessages
	 * (java.util.List, java.util.List, iamrescue.agent.ISimulationTimer)
	 */
	@Override
	public Map<MessageChannel, List<Message>> distributeMessages(
			List<Message> messagesToOwnTeam,
			List<Message> messagesToOtherTeams, ISimulationTimer timer) {
		map.clear();
		List<Message> list = new FastList<Message>();
		list.addAll(messagesToOtherTeams);
		list.addAll(messagesToOwnTeam);
		map.put(myChannel, list);
		return map;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.scenario.ICommunicationScenario#reinitialiseTeam
	 * (iamrescue.communication.ISimulationCommunicationConfiguration)
	 */
	@Override
	public void reinitialiseTeam(Collection<EntityID> toIgnore) {
		// Don't need to do anything
	}

	@Override
	public List<EntityID> getMyCentres() {
		return new ArrayList<EntityID>();
	}

	@Override
	public boolean amICentre() {
		return false;
	}

}
