/**
 * 
 */
package iamrescue.communication.scenario.scenarios;

import iamrescue.agent.ISimulationTimer;
import iamrescue.communication.IMessagingSchedule;
import iamrescue.communication.ISimulationCommunicationConfiguration;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.scenario.ICommunicationScenario;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class NoCommunicationScenario implements ICommunicationScenario {

	private StandardEntityURN myType;
	private final Map<MessageChannel, List<Message>> EMPTY_MAP = new FastMap<MessageChannel, List<Message>>();
	private final List<MessageChannel> voiceChannels;

	public NoCommunicationScenario(StandardEntityURN myType,
			List<MessageChannel> voiceChannels) {
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
		return new ArrayList<MessageChannel>();
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
		return new ArrayList<MessageChannel>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeiamrescue.communication.scenario.ICommunicationScenario#
	 * getChannelsToSubscribeTo()
	 */
	@Override
	public List<MessageChannel> getChannelsToSubscribeTo() {
		return new ArrayList<MessageChannel>();
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
		return new DefaultIAMCommunicationsScenario.MultiChannelScheduler(
				voiceChannels);
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
		return EMPTY_MAP;
	}

	@Override
	public void reinitialiseTeam(Collection<EntityID> toIgnore) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean amICentre() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<EntityID> getMyCentres() {
		// TODO Auto-generated method stub
		return new ArrayList<EntityID>();
	}
}
