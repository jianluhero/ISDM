/**
 * 
 */
package iamrescue.communication.scenario.scenarios;

import iamrescue.agent.ISimulationTimer;
import iamrescue.communication.IMessagingSchedule;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.scenario.ICommunicationScenario;
import iamrescue.communication.scenario.scenarios.DefaultIAMCommunicationsScenario.MultiChannelScheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class SingleChannelScenario implements ICommunicationScenario {

	private MessageChannel channel;
	private int agents;
	private StandardEntityURN myType;
	private Map<MessageChannel, List<Message>> map = new FastMap<MessageChannel, List<Message>>();
	private final List<MessageChannel> voiceChannels;
	private MultiChannelScheduler schedule;

	public SingleChannelScenario(MessageChannel channel, int agents,
			StandardEntityURN myType, List<MessageChannel> voiceChannels) {
		this.channel = channel;
		this.agents = agents;
		this.myType = myType;
		this.voiceChannels = voiceChannels;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.scenario.ICommunicationScenario#getChannelsToCenters
	 * ()
	 */
	@Override
	public List<MessageChannel> getChannelsToOtherTeams() {
		return Collections.singletonList(channel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.scenario.ICommunicationScenario#getChannelsToPlatoons
	 * ()
	 */
	@Override
	public List<MessageChannel> getChannelsToOwnTeam() {
		return Collections.singletonList(channel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeiamrescue.communication.scenario.ICommunicationScenario#
	 * getChannelsToSubscribeTo()
	 */
	@Override
	public List<MessageChannel> getChannelsToSubscribeTo() {
		return Collections.singletonList(channel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.communication.scenario.ICommunicationScenario#getMyRole()
	 */
	@Override
	public StandardEntityURN getMyRole() {
		return myType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.scenario.ICommunicationScenario#getScheduler()
	 */
	@Override
	public IMessagingSchedule getScheduler() {
		if (schedule == null) {
			schedule = new MultiChannelScheduler(voiceChannels);
			schedule.addSchedule(channel, new SharedBandwidthSchedule(channel,
					agents));
		}
		return schedule;
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
		map.put(channel, list);
		return map;
	}

	@Override
	public void reinitialiseTeam(Collection<EntityID> toIgnore) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean amICentre() {
		return false;
	}

	@Override
	public List<EntityID> getMyCentres() {
		return new ArrayList<EntityID>();
	}

}
