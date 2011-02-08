package iamrescue.communication.scenario.scenarios;

import iamrescue.agent.ISimulationTimer;
import iamrescue.communication.IMessagingSchedule;
import iamrescue.communication.ISimulationCommunicationConfiguration;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.scenario.ICommunicationScenario;
import iamrescue.util.EntityComparator;
import iamrescue.util.comparators.ChannelIDComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public class ManyLowBandwidthScenario implements ICommunicationScenario {

	// Centre counts as five agents for relative bandwidth share.
	private static final int RELATIVE_CENTRE_SHARE = 5;

	private static final int POLICE_INDEX = 0;
	private static final int AMBULANCE_INDEX = 1;
	private static final int FIRE_INDEX = 2;

	private List<MessageChannel> myTeamChannels = new ArrayList<MessageChannel>();
	private List<MessageChannel> otherTeamCentreChannels = new ArrayList<MessageChannel>();
	private List<MessageChannel> myCentreOutChannels = new ArrayList<MessageChannel>();

	private List<MessageChannel> mySubscribedChannels = new ArrayList<MessageChannel>();

	private MessageChannel myOutputChannel;

	Map<MessageChannel, List<Message>> outMap = new FastMap<MessageChannel, List<Message>>();

	private boolean iamCentre;

	private ISimulationCommunicationConfiguration configuration;

	private SharedBandwidthSchedule schedule;

	private int myTeamIndex;

	public ManyLowBandwidthScenario(
			ISimulationCommunicationConfiguration configuration) {

		List<MessageChannel> channels = new ArrayList<MessageChannel>(
				configuration.getRadioChannels());
		Collections.sort(channels, ChannelIDComparator.INSTANCE);

		Map<StandardEntityURN, Collection<StandardEntity>> agentsByType = configuration
				.getAgentsByType();

		int ambulanceCentres = agentsByType.get(
				StandardEntityURN.AMBULANCE_CENTRE).size();
		int ambulances = agentsByType.get(StandardEntityURN.AMBULANCE_TEAM)
				.size();

		int policeCentres = agentsByType.get(StandardEntityURN.POLICE_OFFICE)
				.size();
		int policeForces = agentsByType.get(StandardEntityURN.POLICE_FORCE)
				.size();

		int fireStations = agentsByType.get(StandardEntityURN.FIRE_STATION)
				.size();
		int fireBrigades = agentsByType.get(StandardEntityURN.FIRE_BRIGADE)
				.size();

		this.configuration = configuration;

		// Keep track of channels
		List<List<MessageChannel>> centreChannels = new ArrayList<List<MessageChannel>>();
		List<MessageChannel> policeCentreChannels = new ArrayList<MessageChannel>();
		List<MessageChannel> ambulanceCentreChannels = new ArrayList<MessageChannel>();
		List<MessageChannel> fireBrigadeCentreChannels = new ArrayList<MessageChannel>();
		centreChannels.add(policeCentreChannels);
		centreChannels.add(ambulanceCentreChannels);
		centreChannels.add(fireBrigadeCentreChannels);

		List<List<MessageChannel>> platoonChannels = new ArrayList<List<MessageChannel>>();
		List<MessageChannel> policeChannels = new ArrayList<MessageChannel>();
		List<MessageChannel> ambulanceChannels = new ArrayList<MessageChannel>();
		List<MessageChannel> fireBrigadeChannels = new ArrayList<MessageChannel>();
		platoonChannels.add(policeChannels);
		platoonChannels.add(ambulanceChannels);
		platoonChannels.add(fireBrigadeChannels);

		// We assume for now channels are homogeneous

		// Start by reserving one channel for each team.
		policeChannels.add(channels.remove(0));
		ambulanceChannels.add(channels.remove(0));
		fireBrigadeChannels.add(channels.remove(0));

		// Reserve three channels for the centres (outgoing)
		for (int i = 0; i < 3 && channels.size() > 0; i++) {
			centreChannels.get(i).add(channels.remove(0));
		}

		// How many channels can a platoon listen to?
		int maxListenPlatoon = configuration.getMaxListenChannelCountPlatoon()
				- configuration.getVoiceChannels().size();
		int maxListenCentre = configuration.getMaxListenChannelCountCentre()
				- configuration.getVoiceChannels().size();

		// Divide up the remaining channels. For every 2 platoon channels,
		// create 1 centre channel. (while there is any more point)
		boolean added = false;
		do {
			for (int i = 0; i < platoonChannels.size() && channels.size() > 0; i++) {
				List<MessageChannel> platoonList = platoonChannels.get(i);
				if (maxListenPlatoon > platoonList.size()) {
					platoonList.add(channels.remove(0));
					added = true;
				}
			}
			for (int i = 0; i < 3 && channels.size() > 0; i++) {
				int otherCenterChannelCount = Math
						.max(centreChannels.get(i + 1 % 3).size(),
								centreChannels.get(i + 2 % 3).size());
				if (maxListenCentre > otherCenterChannelCount
						+ platoonChannels.get(i).size()) {
					centreChannels.get(i).add(channels.remove(0));
					added = true;
				}
			}
		} while (added && channels.size() > 0);

		// We have channels now
		// Get my team
		List<StandardEntity> myPlatoonTeam;
		Map<EntityID, MessageChannel> outputChannels = new FastMap<EntityID, MessageChannel>();

		if (configuration.getAgentType().equals(StandardEntityURN.POLICE_FORCE)
				|| configuration.getAgentType().equals(
						StandardEntityURN.POLICE_OFFICE)) {
			myTeamIndex = POLICE_INDEX;
			myPlatoonTeam = new ArrayList<StandardEntity>(configuration
					.getAgentsByType().get(StandardEntityURN.POLICE_FORCE));
		} else if (configuration.getAgentType().equals(
				StandardEntityURN.AMBULANCE_TEAM)
				|| configuration.getAgentType().equals(
						StandardEntityURN.AMBULANCE_CENTRE)) {
			myTeamIndex = AMBULANCE_INDEX;
			myPlatoonTeam = new ArrayList<StandardEntity>(configuration
					.getAgentsByType().get(StandardEntityURN.AMBULANCE_TEAM));
		} else { // if
			// (configuration.getAgentType().equals(StandardEntityURN.FIRE_BRIGADE)
			// ||
			// configuration.getAgentType().equals(StandardEntityURN.FIRE_STATION))
			// {
			myTeamIndex = FIRE_INDEX;
			myPlatoonTeam = new ArrayList<StandardEntity>(configuration
					.getAgentsByType().get(StandardEntityURN.FIRE_BRIGADE));
		}

		Collections.sort(myPlatoonTeam, new EntityComparator());

		myTeamChannels = platoonChannels.get(myTeamIndex);

		int[] perChannel = new int[myTeamChannels.size()];
		for (int i = 0; i < perChannel.length; i++) {
			perChannel[i] = 0;
		}

		iamCentre = (configuration.getAgentType().equals(
				StandardEntityURN.AMBULANCE_CENTRE)
				|| configuration.getAgentType().equals(
						StandardEntityURN.POLICE_OFFICE) || configuration
				.getAgentType().equals(StandardEntityURN.FIRE_STATION));

		List<StandardEntity> centres;
		if (myTeamIndex == 0) {
			centres = new ArrayList<StandardEntity>(configuration
					.getCentresByType().get(StandardEntityURN.POLICE_OFFICE));
		} else if (myTeamIndex == 1) {
			centres = new ArrayList<StandardEntity>(configuration
					.getCentresByType().get(StandardEntityURN.AMBULANCE_CENTRE));
		} else {
			centres = new ArrayList<StandardEntity>(configuration
					.getCentresByType().get(StandardEntityURN.FIRE_STATION));
		}

		Collections.sort(centres, new EntityComparator());

		int counter = 0;
		int highest = 0;
		int myChannelIndex = -1;
		for (StandardEntity centre : centres) {
			int index = counter % myTeamChannels.size();
			MessageChannel channel = myTeamChannels.get(index);
			outputChannels.put(centre.getID(), channel);
			perChannel[index] += RELATIVE_CENTRE_SHARE;
			if (perChannel[index] > highest) {
				highest = perChannel[index];
			}
			if (centre.getID().equals(configuration.getEntityID())) {
				myOutputChannel = channel;
				myChannelIndex = index;
			}
			counter++;
		}

		int startCounter = counter;
		boolean metHighest = false;

		for (StandardEntity standardEntity : myPlatoonTeam) {
			int index = counter % myTeamChannels.size();

			// This is to ensure centre channels are avoided until equivalent
			// capacity on other channels has been filled up
			if (index == 0 && !metHighest) {
				index = startCounter % myTeamChannels.size();
			}
			MessageChannel channel = myTeamChannels.get(index);
			outputChannels.put(standardEntity.getID(), channel);
			perChannel[index]++;
			if (perChannel[index] == highest) {
				metHighest = true;
			}
			if (standardEntity.getID().equals(configuration.getEntityID())) {
				myOutputChannel = channel;
				myChannelIndex = index;
			}
			counter++;
		}

		myCentreOutChannels = centreChannels.get(myTeamIndex);
		mySubscribedChannels.addAll(myTeamChannels);
		if (iamCentre) {
			mySubscribedChannels.addAll(centreChannels.get(myTeamIndex + 1
					% centreChannels.size()));
			mySubscribedChannels.addAll(centreChannels.get(myTeamIndex + 2
					% centreChannels.size()));

			// If more channels available, listen to other platoons directly
			for (int i = 0; i < platoonChannels.get(0).size()
					&& mySubscribedChannels.size() < maxListenCentre; i++) {
				for (int j = 1; j <= 2
						&& mySubscribedChannels.size() < maxListenCentre; j++) {
					int index = (myTeamIndex + 1 + j) % 3;
					List<MessageChannel> list = platoonChannels.get(index);
					if (list.size() <= i) {
						continue;
					} else {
						mySubscribedChannels.add(list.get(i));
					}
				}
			}
			schedule = new SharedBandwidthSchedule(myOutputChannel,
					perChannel[myChannelIndex], RELATIVE_CENTRE_SHARE);
		} else {
			// If more channels available, listen to other centres directly
			for (int i = 0; i < centreChannels.get(0).size()
					&& mySubscribedChannels.size() < maxListenPlatoon; i++) {
				for (int j = 0; j < 2
						&& mySubscribedChannels.size() < maxListenPlatoon; j++) {
					int index = (myTeamIndex + 1 + j) % 3;
					List<MessageChannel> list = centreChannels.get(index);
					if (list.size() <= i) {
						continue;
					} else {
						mySubscribedChannels.add(list.get(i));
					}
				}
			}
			schedule = new SharedBandwidthSchedule(myOutputChannel,
					perChannel[myChannelIndex]);
		}

	}

	@Override
	public Map<MessageChannel, List<Message>> distributeMessages(
			List<Message> messagesToOwnTeam,
			List<Message> messagesToOtherTeams, ISimulationTimer timer) {
		outMap.clear();
		outMap.put(myOutputChannel, messagesToOwnTeam);
		if (messagesToOtherTeams.size() > 0) {
			for (MessageChannel channel : myCentreOutChannels) {
				outMap.put(channel, new ArrayList<Message>());
			}
			int counter = 0;
			for (Message message : messagesToOtherTeams) {
				outMap.get(
						myCentreOutChannels.get(counter
								% myCentreOutChannels.size())).add(message);
				counter++;
			}
		}
		return outMap;
	}

	@Override
	public List<MessageChannel> getChannelsToOtherTeams() {
		return myCentreOutChannels;
	}

	@Override
	public List<MessageChannel> getChannelsToOwnTeam() {
		return myTeamChannels;
	}

	@Override
	public List<MessageChannel> getChannelsToSubscribeTo() {
		return mySubscribedChannels;
	}

	@Override
	public StandardEntityURN getMyRole() {
		return configuration.getAgentType();
	}

	@Override
	public IMessagingSchedule getScheduler() {
		return schedule;
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
		// 
		// TODO : Code this.
		// newConfiguration.getCentresByType().get(key)
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
