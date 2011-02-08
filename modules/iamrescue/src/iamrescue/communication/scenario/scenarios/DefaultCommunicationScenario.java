package iamrescue.communication.scenario.scenarios;

import iamrescue.agent.AgentTypeUtils;
import iamrescue.agent.ISimulationTimer;
import iamrescue.communication.IMessagingSchedule;
import iamrescue.communication.ISimulationCommunicationConfiguration;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.messages.MessageChannelType;
import iamrescue.communication.scenario.ChannelAllocation;
import iamrescue.communication.scenario.IChannelAllocation;
import iamrescue.communication.scenario.ICommunicationScenario;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public class DefaultCommunicationScenario implements ICommunicationScenario {

	private static final Logger LOGGER = Logger
			.getLogger(DefaultCommunicationScenario.class);

	private ISimulationCommunicationConfiguration configuration;

	private Collection<IChannelAllocation> channelAllocations = new ArrayList<IChannelAllocation>();

	private Collection<IChannelAllocation> channelAllocationsToPlatoon = new ArrayList<IChannelAllocation>();
	private Collection<IChannelAllocation> channelAllocationsToCentre = new ArrayList<IChannelAllocation>();

	private Map<MessageChannel, IChannelAllocation> channelToAllocationMap = new HashMap<MessageChannel, IChannelAllocation>();

	public DefaultCommunicationScenario(
			ISimulationCommunicationConfiguration configuration) {
		this.configuration = configuration;

		channelAllocations.add(new ChannelAllocation(
				StandardEntityURN.AMBULANCE_CENTRE,
				StandardEntityURN.AMBULANCE_TEAM, configuration, 0));

		channelAllocations.add(new ChannelAllocation(
				StandardEntityURN.AMBULANCE_TEAM,
				StandardEntityURN.AMBULANCE_CENTRE, configuration, 1));

		channelAllocations.add(new ChannelAllocation(
				StandardEntityURN.FIRE_STATION, StandardEntityURN.FIRE_BRIGADE,
				configuration, 2));

		channelAllocations.add(new ChannelAllocation(
				StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.FIRE_STATION,
				configuration, 3));

		channelAllocations.add(new ChannelAllocation(
				StandardEntityURN.POLICE_OFFICE,
				StandardEntityURN.POLICE_FORCE, configuration, 4));

		channelAllocations.add(new ChannelAllocation(
				StandardEntityURN.POLICE_FORCE,
				StandardEntityURN.POLICE_OFFICE, configuration, 5));

		channelAllocations.add(new ChannelAllocation(EnumSet.of(
				StandardEntityURN.AMBULANCE_CENTRE,
				StandardEntityURN.FIRE_STATION),
				StandardEntityURN.POLICE_OFFICE, configuration, 6));

		channelAllocations.add(new ChannelAllocation(EnumSet.of(
				StandardEntityURN.AMBULANCE_CENTRE,
				StandardEntityURN.POLICE_OFFICE),
				StandardEntityURN.FIRE_STATION, configuration, 7));

		channelAllocations.add(new ChannelAllocation(EnumSet
				.of(StandardEntityURN.FIRE_STATION,
						StandardEntityURN.POLICE_OFFICE),
				StandardEntityURN.AMBULANCE_CENTRE, configuration, 8));

		for (IChannelAllocation allocation : channelAllocations) {
			channelToAllocationMap.put(allocation.getChannel(), allocation);
		}

		channelAllocationsToPlatoon = getChannelAllocationsToPlatoon();
		channelAllocationsToCentre = getChannelAllocationsToCentres();

	}

	private Collection<IChannelAllocation> getChannelAllocationsToPlatoon() {
		Collection<IChannelAllocation> result = new ArrayList<IChannelAllocation>();

		if (AgentTypeUtils.isCentre(getMyRole())) {
			for (IChannelAllocation allocation : channelAllocations) {
				if (allocation.getSenderTypes().contains(getMyRole())) {
					if (AgentTypeUtils.getAssociatedPlatoon(getMyRole()) == allocation
							.getReceiverType()) {
						result.add(allocation);
					}
				}
			}
		}

		return result;
	}

	private Collection<IChannelAllocation> getChannelAllocationsToCentres() {
		Collection<IChannelAllocation> result = new ArrayList<IChannelAllocation>();

		for (IChannelAllocation allocation : channelAllocations) {
			if (allocation.getSenderTypes().contains(getMyRole())) {
				if (AgentTypeUtils.isCentre(allocation.getReceiverType())) {
					result.add(allocation);
				}
			}
		}

		return result;
	}

	@Override
	public List<MessageChannel> getChannelsToOtherTeams() {
		List<MessageChannel> result = new ArrayList<MessageChannel>();
		for (IChannelAllocation allocation : channelAllocationsToCentre) {
			result.add(allocation.getChannel());
		}

		return result;
	}

	@Override
	public List<MessageChannel> getChannelsToOwnTeam() {
		List<MessageChannel> result = new ArrayList<MessageChannel>();
		for (IChannelAllocation allocation : channelAllocationsToPlatoon) {
			result.add(allocation.getChannel());
		}

		return result;
	}

	@Override
	public List<MessageChannel> getChannelsToSubscribeTo() {
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		// configuration.getVoiceChannels());
		channels.addAll(getRadioChannelsToSubscribeTo());

		return channels;
	}

	private List<MessageChannel> getRadioChannelsToSubscribeTo() {
		List<MessageChannel> result = new ArrayList<MessageChannel>();

		for (IChannelAllocation allocation : channelAllocations) {
			// am I allocated as a receiver on this channel, if so, listen to
			// this channel
			if (allocation.isReceiver(configuration.getEntityID())) {
				result.add(allocation.getChannel());
			}
		}

		return result;
	}

	@Override
	public StandardEntityURN getMyRole() {
		return configuration.getAgentType();
	}

	@Override
	public IMessagingSchedule getScheduler() {
		return new IMessagingSchedule() {

			@Override
			public int getAllocatedMessagesSize(MessageChannel channel, int time) {
				if (channel.getType() == MessageChannelType.VOICE) {
					return channel.getMaxMessageSize();
				}

				IChannelAllocation allocation = channelToAllocationMap
						.get(channel);
				Validate.notNull(allocation);
				Validate.notNull(channel);

				int allocatedBandwidth = allocation
						.getAllocatedBandwidth(configuration.getEntityID());

				return allocatedBandwidth;
			}

			@Override
			public int getAllocatedMessagesCount(MessageChannel channel,
					int timestep) {
				if (channel.getType() == MessageChannelType.VOICE) {
					return channel.getMaxMessageCount();
				}

				IChannelAllocation allocation = channelToAllocationMap
						.get(channel);

				// if I'm a sender for this channel, I can send as many messages
				// as I want, otherwise 0
				if (allocation.isSender(configuration.getEntityID()))
					return Integer.MAX_VALUE;
				else
					return 0;
			}

			@Override
			public int getAllocatedTotalBandwidth(MessageChannel channel,
					int time) {
				return getAllocatedMessagesSize(channel, time);
			}

			@Override
			public int getMaximumRepetitions(MessageChannel channel, int time) {
				return 0;
			}
		};
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
		Map<MessageChannel, List<Message>> map = new FastMap<MessageChannel, List<Message>>();
		List<MessageChannel> channelsToOtherTeams = getChannelsToOtherTeams();
		for (MessageChannel messageChannel : channelsToOtherTeams) {
			map.put(messageChannel, messagesToOtherTeams);
		}

		List<MessageChannel> channelsToOwnTeam = getChannelsToOwnTeam();
		for (MessageChannel messageChannel : channelsToOwnTeam) {
			map.put(messageChannel, messagesToOwnTeam);
		}
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
		LOGGER.error("Have not implemented reconfiguration.");
	}

	@Override
	public boolean amICentre() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<EntityID> getMyCentres() {
		// TODO Auto-generated method stub
		return null;
	}
}
