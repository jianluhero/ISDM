package iamrescue.communication.scenario.scenarios;

import iamrescue.agent.ISimulationTimer;
import iamrescue.communication.IMessagingSchedule;
import iamrescue.communication.ISimulationCommunicationConfiguration;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.scenario.ChannelDividerUtil;
import iamrescue.communication.scenario.ICommunicationScenario;
import iamrescue.util.comparators.ChannelBandwidthComparator;
import iamrescue.util.comparators.EntityIDComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;
import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public class DefaultIAMCommunicationsScenario implements ICommunicationScenario {

	// TODO: Should do this per agent?
	// Channels with this bandwidth are considered high-bandwidth
	public static final int HIGH_BANDWIDTH_THRESHOLD = 800;

	// If we can achieve this per agent, we always prefer single teams:
	public static final int PER_AGENT_SINGLE_TEAM_THRESHOLD = 150;

	private ISimulationCommunicationConfiguration configuration;
	private ArrayList<MessageChannel> radioChannels;

	private Map<EntityID, IAMTeamCommunicationConfiguration> teamConfigurations = new FastMap<EntityID, IAMTeamCommunicationConfiguration>();

	private Map<MessageChannel, List<Message>> mapOut = new FastMap<MessageChannel, List<Message>>();

	private int maxCentreChannels;

	private int maxPlatoonChannels;

	private List<IAMTeamCommunicationConfiguration> configs = new ArrayList<IAMTeamCommunicationConfiguration>();

	private MessageChannel myTeamOut;
	private IMessagingSchedule teamSchedule;
	private IMessagingSchedule overflowSchedule;
	private MessageChannel myOverflowOut;
	private List<MessageChannel> mySubscribed;
	private List<MessageChannel> channelsToOwnTeam;
	private List<MessageChannel> channelsToOtherTeams;

	private IMessagingSchedule scheduler;

	private IAMTeamCommunicationConfiguration myTeamConfig;

	private static final Logger LOGGER = Logger
			.getLogger(DefaultIAMCommunicationsScenario.class);

	public DefaultIAMCommunicationsScenario(
			ISimulationCommunicationConfiguration configuration) {

		// Set up config fields.
		this.configuration = configuration;
		maxCentreChannels = configuration.getMaxListenChannelCountCentre()
				- configuration.getVoiceChannels().size();
		maxPlatoonChannels = configuration.getMaxListenChannelCountPlatoon()
				- configuration.getVoiceChannels().size();
		radioChannels = new ArrayList<MessageChannel>(configuration
				.getRadioChannels());

		// sort channels by increasing bandwidth
		Collections.sort(radioChannels, ChannelBandwidthComparator.INSTANCE);

		// First try single team with all agents
		Collection<StandardEntity> allPlatoons = ChannelDividerUtil
				.flattenMap(configuration.getPlatoonsByType());
		int numAgents = allPlatoons.size();

		// If desired num. of bytes can be met, we prefer single team scenario.
		int desired = numAgents * PER_AGENT_SINGLE_TEAM_THRESHOLD;

		// Now try desired scenarios in order

		// First try single team, desired bandwidth
		boolean done = attemptToGenerateScenario(1, maxPlatoonChannels, desired);

		// Now try 3,2,1 teams in order, using high bandwidth target.
		int teams = 3;
		while (!done && teams > 0) {
			done = attemptToGenerateScenario(teams, maxPlatoonChannels,
					HIGH_BANDWIDTH_THRESHOLD);
			teams--;
		}

		// Now repeat 3,2,1 teams, but accept any bandwidth above 0
		teams = 3;
		while (!done && teams > 0) {
			done = attemptToGenerateScenario(teams, maxPlatoonChannels, 1);
			teams--;
		}

		if (!done) {
			LOGGER.warn("No communication scenario was possible. "
					+ "Agent is not sending or receiving");
			// generateNoCommunicationScenario();
			throw new IllegalStateException("Could not generate scenario.");
		} else {
			finaliseAllocation();
		}
	}

	private void generateNoCommunicationScenario() {
		List<MessageChannel> emptyList = new ArrayList<MessageChannel>();
		channelsToOtherTeams = emptyList;
		channelsToOwnTeam = emptyList;
		mySubscribed = emptyList;

		// Empty multi channel scheduler returns 0 to all by default.
		scheduler = new MultiChannelScheduler(configuration.getVoiceChannels());
	}

	private boolean attemptToGenerateScenario(int teams,
			int maxChannelsPerTeam, int targetMinimumBandwidth) {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Attempting to achieve total bandwidth of "
					+ targetMinimumBandwidth + " bytes with " + teams + " team"
					+ ((teams == 1) ? "" : "s") + ".");
		}

		// Try to allocate
		List<List<MessageChannel>> mainTeamChannels = distributeChannels(teams,
				maxPlatoonChannels);

		// What's the bandwidth for this?
		int minBandwidthAvailable = ChannelDividerUtil
				.findMinimumBandwidth(mainTeamChannels);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Total achievable: " + minBandwidthAvailable
					+ " bytes");
		}

		if (minBandwidthAvailable >= targetMinimumBandwidth) {
			// Good
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("This is enough. Starting generation "
						+ "for main channels: " + mainTeamChannels);
			}
			generateMultipleChannelsScenario(mainTeamChannels);
			return true;
		} else {
			// Not good
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("This is not enough.");
			}
			return false;
		}
	}

	/**
	 * 
	 */
	private void finaliseAllocation() {
		// All allocations are done now. Extract relevant info from the configs
		for (IAMTeamCommunicationConfiguration config : configs) {
			Set<EntityID> centres = config.getCentres();
			for (EntityID entityID : centres) {
				teamConfigurations.put(entityID, config);
			}
			Set<EntityID> platoons = config.getPlatoons();
			for (EntityID entityID : platoons) {
				teamConfigurations.put(entityID, config);
			}
		}

		EntityID id = configuration.getEntityID();
		myTeamConfig = teamConfigurations.get(id);

		if (myTeamConfig == null) {
			// I am a centre that is not taking part

		} else {

			myTeamOut = myTeamConfig.getOutputMainChannels().get(id);
			myOverflowOut = myTeamConfig.getOutputOverflowChannels().get(id);
			mySubscribed = myTeamConfig.getSubscribedChannels().get(id);
			channelsToOwnTeam = myTeamConfig.getMainCommunicationChannels();
			channelsToOtherTeams = new ArrayList<MessageChannel>();

			// Now create schedules

			int myShare;
			if (myTeamConfig.getCentres().contains(id)) {
				myShare = IAMTeamCommunicationConfiguration.RELATIVE_CENTRE_BANDIWDTH;
			} else {
				myShare = 1;
			}

			List<EntityID> list = myTeamConfig.getChannelSenders().get(
					myTeamOut);
			int totalBandwidthTeamOut = 0;
			for (EntityID otherID : list) {
				if (myTeamConfig.getCentres().contains(otherID)) {
					totalBandwidthTeamOut += IAMTeamCommunicationConfiguration.RELATIVE_CENTRE_BANDIWDTH;
				} else {
					totalBandwidthTeamOut++;
				}
			}

			teamSchedule = new SharedBandwidthSchedule(myTeamOut,
					totalBandwidthTeamOut, myShare);

			if (myOverflowOut != null) {
				list = myTeamConfig.getChannelSenders().get(myOverflowOut);
				int totalBandwidthOverflow = 0;
				for (EntityID otherID : list) {
					if (myTeamConfig.getCentres().contains(otherID)) {
						totalBandwidthOverflow += IAMTeamCommunicationConfiguration.RELATIVE_CENTRE_BANDIWDTH;
					} else {
						totalBandwidthOverflow++;
					}
				}
				overflowSchedule = new SharedBandwidthSchedule(myOverflowOut,
						totalBandwidthOverflow, myShare);
			}

			MultiChannelScheduler scheduler = new MultiChannelScheduler(
					configuration.getVoiceChannels());
			scheduler.addSchedule(myTeamOut, teamSchedule);
			if (overflowSchedule != null) {
				scheduler.addSchedule(myOverflowOut, overflowSchedule);
			}
			this.scheduler = scheduler;

			if (LOGGER.isInfoEnabled()) {
				LOGGER.info(toVerboseString());
				LOGGER.info(myTeamConfig.toVerboseString());
			}
		}
	}

	public String toVerboseString() {
		String str = "Scenario for agent " + configuration.getEntityID();
		str += myTeamConfig.getCentres().contains(configuration.getEntityID()) ? " (centre role) "
				: " (platoon) ";

		if (myTeamOut == null) {
			str += ", inactive (not sending)";
		} else {

			str += ", sending to " + myTeamOut.getChannelNumber() + " ("
					+ scheduler.getAllocatedTotalBandwidth(myTeamOut, 1)
					+ " bytes ), overflow: ";
			str += (myOverflowOut != null) ? myOverflowOut.getChannelNumber()
					+ " ("
					+ scheduler.getAllocatedTotalBandwidth(myOverflowOut, 0)
					+ " bytes)" : "none";

		}
		str += ", listening to";

		for (MessageChannel channel : mySubscribed) {
			str += " " + channel.getChannelNumber();
		}

		return str;
	}

	private void generateMultipleChannelsScenario(
			List<List<MessageChannel>> mainTeamChannels) {

		// Work out how many teams there are
		int teams = mainTeamChannels.size();

		// Filter out remaining channels as overflow channels
		Set<MessageChannel> taken = new FastSet<MessageChannel>();

		for (List<MessageChannel> channelList : mainTeamChannels) {
			for (MessageChannel messageChannel : channelList) {
				taken.add(messageChannel);
			}
		}

		List<MessageChannel> overflowChannels = new ArrayList<MessageChannel>();
		for (MessageChannel messageChannel : radioChannels) {
			if (!taken.contains(messageChannel)) {
				overflowChannels.add(messageChannel);
			}
		}

		// Now assign overflow channels equally to the teams.

		int maxOverflow = maxCentreChannels;
		List<List<MessageChannel>> overflowChannelsAssigned = distributeUnallocatedChannels(
				mainTeamChannels, Integer.MAX_VALUE);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Assigned initial overflow channels: "
					+ overflowChannelsAssigned + ", based on max number: "
					+ maxOverflow);
		}

		// Now assign all remaining as overflow (in case there are multiple
		// centres)
		/*
		 * List<List<MessageChannel>> moreOverflowChannelsAssigned =
		 * distributeUnallocatedChannels( mainTeamChannels, maxOverflow);
		 */

		sortByDecreasingMainChannelBW(mainTeamChannels,
				overflowChannelsAssigned);

		// Now split up teams
		List<List<StandardEntity>> generatedTeams = generateTeams(teams);

		for (int i = 0; i < generatedTeams.size(); i++) {
			ArrayList<MessageChannel> otherOverflow = new ArrayList<MessageChannel>();
			if (teams > 1) {
				otherOverflow.addAll(overflowChannelsAssigned.get((i + 1)
						% teams));
				if (teams > 2) {
					otherOverflow.addAll(overflowChannelsAssigned.get((i + 2)
							% teams));
				}
			}

			List<MessageChannel> lowPriority;
			if (teams == 1) {
				lowPriority = new ArrayList<MessageChannel>();
			} else if (teams == 2) {
				lowPriority = otherOverflow;
			} else {
				lowPriority = mainTeamChannels.get((i + 2) % 3);
			}

			List<MessageChannel> veryLowPriority;
			if (teams <= 2) {
				veryLowPriority = new ArrayList<MessageChannel>();
			} else {
				veryLowPriority = otherOverflow;
			}

			List<MessageChannel> highPriority;
			if (teams == 1) {
				highPriority = new ArrayList<MessageChannel>();
			} else {
				highPriority = mainTeamChannels.get((i + 1) % teams);
			}

			IAMTeamCommunicationConfiguration team = IAMTeamCommunicationConfiguration
					.createTeam(mainTeamChannels.get(i),
							overflowChannelsAssigned.get(i), highPriority,
							lowPriority, veryLowPriority,
							generatedTeams.get(i), maxPlatoonChannels,
							maxCentreChannels);
			configs.add(team);
		}
	}

	private void sortByDecreasingMainChannelBW(
			List<List<MessageChannel>> mainTeamChannels,
			List<List<MessageChannel>> overflowChannelsAssigned) {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Reordering main team and associated overflow "
					+ "channels, so that they are "
					+ "ordered by decreasing bandwidth for main "
					+ "channels. Main before: " + mainTeamChannels
					+ ", overflow before: " + overflowChannelsAssigned);
		}

		List<Pair<List<MessageChannel>, Integer>> pairs = new ArrayList<Pair<List<MessageChannel>, Integer>>();
		for (int i = 0; i < mainTeamChannels.size(); i++) {
			pairs.add(new Pair<List<MessageChannel>, Integer>(mainTeamChannels
					.get(i), i));
		}
		// Now sort based on total bandwidth for main channels
		Collections.sort(pairs,
				new Comparator<Pair<List<MessageChannel>, Integer>>() {

					@Override
					public int compare(
							Pair<List<MessageChannel>, Integer> arg0,
							Pair<List<MessageChannel>, Integer> arg1) {
						int compared = -Double.compare(ChannelDividerUtil
								.computeTotalBandwidth(arg0.first()),
								ChannelDividerUtil.computeTotalBandwidth(arg1
										.first()));
						if (compared != 0) {
							return compared;
						} else {
							return Double.compare(arg1.second(), arg0.second());
						}
					}
				});

		List<List<MessageChannel>> overflowCopy = new ArrayList<List<MessageChannel>>(
				overflowChannelsAssigned);

		// Use result to re-order original lists
		for (int i = 0; i < pairs.size(); i++) {
			mainTeamChannels.set(i, pairs.get(i).first());
			overflowChannelsAssigned.set(i, overflowCopy.get(i));
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Main after: " + mainTeamChannels
					+ ", overflow after: " + overflowChannelsAssigned);
		}

	}

	private List<List<StandardEntity>> generateTeams(int number) {
		if (number == 1) {
			List<List<StandardEntity>> list = new ArrayList<List<StandardEntity>>();
			list.add(ChannelDividerUtil.flattenMap(configuration
					.getAgentsByType()));
			return list;
		} else {
			// Split into two or more teams
			List<StandardEntity> policeAgents = new ArrayList<StandardEntity>(
					configuration.getAgentsByType().get(
							StandardEntityURN.POLICE_FORCE));
			List<StandardEntity> policeCentres = new ArrayList<StandardEntity>(
					configuration.getAgentsByType().get(
							StandardEntityURN.POLICE_OFFICE));
			List<StandardEntity> ambulanceAgents = new ArrayList<StandardEntity>(
					configuration.getAgentsByType().get(
							StandardEntityURN.AMBULANCE_TEAM));
			List<StandardEntity> ambulanceCentres = new ArrayList<StandardEntity>(
					configuration.getAgentsByType().get(
							StandardEntityURN.AMBULANCE_CENTRE));
			List<StandardEntity> fireAgents = new ArrayList<StandardEntity>(
					configuration.getAgentsByType().get(
							StandardEntityURN.FIRE_BRIGADE));
			List<StandardEntity> fireCentres = new ArrayList<StandardEntity>(
					configuration.getAgentsByType().get(
							StandardEntityURN.FIRE_STATION));

			// sort by ids, so that consistent information is generated by all
			// agents.
			Collections.sort(policeAgents, new EntityIDComparator());
			Collections.sort(policeCentres, new EntityIDComparator());
			Collections.sort(ambulanceAgents, new EntityIDComparator());
			Collections.sort(ambulanceCentres, new EntityIDComparator());
			Collections.sort(fireAgents, new EntityIDComparator());
			Collections.sort(fireCentres, new EntityIDComparator());

			List<List<StandardEntity>> platoons = new ArrayList<List<StandardEntity>>();
			platoons.add(policeAgents);
			platoons.add(fireAgents);
			platoons.add(ambulanceAgents);

			List<List<StandardEntity>> centres = new ArrayList<List<StandardEntity>>();
			centres.add(policeCentres);
			centres.add(fireCentres);
			centres.add(ambulanceCentres);

			if (number == 2) {
				List<StandardEntity> team1 = new ArrayList<StandardEntity>();
				List<StandardEntity> team2 = new ArrayList<StandardEntity>();
				List<StandardEntity> centres1 = new ArrayList<StandardEntity>();
				List<StandardEntity> centres2 = new ArrayList<StandardEntity>();

				// Add largest platoon team to first channel
				int largestSize = -1;
				int largestIndex = -1;
				for (int i = 0; i < platoons.size(); i++) {
					if (platoons.get(i).size() > largestSize) {
						largestSize = platoons.get(i).size();
						largestIndex = i;
					}
				}
				team1.addAll(platoons.remove(largestIndex));
				centres1.addAll(centres.remove(largestIndex));

				// Are the remaining small enough to share one team?
				if (platoons.get(0).size() + platoons.get(1).size() <= team1
						.size()) {
					// Yes
					team2.addAll(platoons.remove(0));
					centres2.addAll(centres.remove(0));
					team2.addAll(platoons.remove(1));
					centres2.addAll(centres.remove(1));
				} else {
					// No
					team2.addAll(platoons.remove(0));
					centres2.addAll(centres.remove(0));

					// Divide up remaining team
					List<StandardEntity> remainingPlatoons = platoons.get(0);
					List<StandardEntity> remainingCentres = centres.get(0);

					int toSecond = (team1.size() - team2.size() + remainingPlatoons
							.size()) / 2;

					for (int i = 0; i < toSecond; i++) {
						team2.add(remainingPlatoons.get(i));
					}
					for (int i = toSecond; i < remainingPlatoons.size(); i++) {
						team1.add(remainingPlatoons.get(i));
					}

					// Divide up centres equally
					for (int i = 0; i < remainingCentres.size(); i++) {
						if (i % 2 == 0) {
							centres2.add(remainingCentres.get(i));
						} else {
							centres1.add(remainingCentres.get(i));
						}
					}

					// Check if we can re-distribute centres?
					while (centres2.size() - centres1.size() > 1) {
						centres1.add(centres2.remove(centres2.size() - 1));
					}
					while (centres1.size() - centres2.size() > 1) {
						centres2.add(centres1.remove(centres1.size() - 1));
					}
				}
				List<List<StandardEntity>> teams = new ArrayList<List<StandardEntity>>();
				team1.addAll(centres1);
				team2.addAll(centres2);
				teams.add(team1);
				teams.add(team2);
				return teams;

			} else {
				// 3 teams
				List<List<StandardEntity>> platoonList = new ArrayList<List<StandardEntity>>();
				platoonList.add(policeAgents);
				platoonList.add(ambulanceAgents);
				platoonList.add(fireAgents);

				List<List<StandardEntity>> centreList = new ArrayList<List<StandardEntity>>();
				centreList.add(policeCentres);
				centreList.add(ambulanceCentres);
				centreList.add(fireCentres);

				boolean done = false;
				while (!done) {
					int largest = getLargest(centreList);
					int smallest = getSmallest(centreList);
					List<StandardEntity> largestList = centreList.get(largest);
					List<StandardEntity> smallestList = centreList
							.get(smallest);
					if (largestList.size() - smallestList.size() > 1) {
						smallestList.add(largestList
								.remove(largestList.size() - 1));
					} else {
						done = true;
					}
				}

				for (int i = 0; i < 3; i++) {
					platoonList.get(i).addAll(centreList.get(i));
				}
				return platoonList;
			}
		}
	}

	public int getLargest(List<? extends List<?>> collection) {
		int largest = -1;
		int largestIndex = -1;
		int counter = 0;
		for (List<?> collection2 : collection) {
			if (collection2.size() > largest) {
				largest = collection2.size();
				largestIndex = counter;
			}
			counter++;
		}
		return largestIndex;
	}

	public int getSmallest(List<? extends List<?>> collection) {
		int smallest = Integer.MAX_VALUE;
		int smallestIndex = -1;
		int counter = 0;
		for (List<?> collection2 : collection) {
			if (collection2.size() < smallest) {
				smallest = collection2.size();
				smallestIndex = counter;
			}
			counter++;
		}
		return smallestIndex;
	}

	private void generateSingleChannelScenario(List<MessageChannel> list) {
		// We have only one team. Every writes to and reads from the same
		// channels

		// First work out if we have centres. Each one can listen to overflow
		// channels and aggregate information from those into the main channel

		List<MessageChannel> overflowChannels = new ArrayList<MessageChannel>();

		for (int i = 0; i < radioChannels.size(); i++) {
			MessageChannel messageChannel = radioChannels.get(i);
			if (!list.contains(messageChannel)) {
				overflowChannels.add(messageChannel);
			}
		}
		List<StandardEntity> allAgents = ChannelDividerUtil
				.flattenMap(configuration.getAgentsByType());

		IAMTeamCommunicationConfiguration team = IAMTeamCommunicationConfiguration
				.createTeam(list, overflowChannels, Collections.EMPTY_LIST,
						Collections.EMPTY_LIST, Collections.EMPTY_LIST,
						allAgents, maxPlatoonChannels, maxCentreChannels);

		configs.add(team);
	}

	/**
	 * 
	 * @param radioChannels
	 *            All radio channels
	 * @param requiredTeams
	 *            The number of teams to allocate channels to
	 * @param maxChannelsPerTeam
	 *            The maximum number of channels per team
	 * @return An allocation with the highest bandwidth channels distributed
	 *         between teams
	 */
	private List<List<MessageChannel>> distributeChannels(int requiredTeams,
			int maxChannelsPerTeam) {
		List<List<MessageChannel>> alreadyAllocated = new ArrayList<List<MessageChannel>>(
				requiredTeams);
		for (int i = 0; i < requiredTeams; i++) {
			alreadyAllocated.add(new ArrayList<MessageChannel>());
		}
		return distributeUnallocatedChannels(alreadyAllocated,
				maxChannelsPerTeam);
	}

	/**
	 * This method distributes all radioChannels that have not been allocated to
	 * the teams, maximising the minimum total bandwidth for any team.
	 * 
	 * @param alreadyAllocated
	 *            The channels for each team that have already been allocated.
	 * @param maxChannelsPerTeam
	 *            The maximum number of channels that each team should get
	 *            allocated.
	 * @return The allocation.
	 */
	private List<List<MessageChannel>> distributeUnallocatedChannels(
			List<List<MessageChannel>> alreadyAllocated, int maxChannelsPerTeam) {

		// Try to satisfy high bandwidth requirements for the number of required
		// teams.
		Set<MessageChannel> available = new FastSet<MessageChannel>();
		available.addAll(radioChannels);

		int teams = alreadyAllocated.size();

		List<List<MessageChannel>> allocated = new ArrayList<List<MessageChannel>>();

		// total allocated bandwidth to each team
		int[] totalBandwidths = new int[teams];

		// Total allocated num. channels to each team
		int[] totalChannels = new int[teams];

		// Initialise
		for (int i = 0; i < teams; i++) {
			allocated.add(new ArrayList<MessageChannel>());
			totalBandwidths[i] = 0;
			List<MessageChannel> list = alreadyAllocated.get(i);
			totalChannels[i] = list.size();
			for (MessageChannel messageChannel : list) {
				totalBandwidths[i] += messageChannel.getEffectiveBandwidth();
				available.remove(messageChannel);
			}
		}

		// Just consider radio channels that have not been allocated yet.
		List<MessageChannel> radioChannelsLeft = new ArrayList<MessageChannel>();
		radioChannelsLeft.addAll(available);

		// Sort by ascending bandwidth
		Collections
				.sort(radioChannelsLeft, ChannelBandwidthComparator.INSTANCE);

		// Assign one by one starting with highest bandwidth
		for (int i = radioChannelsLeft.size() - 1; i >= 0; i--) {
			MessageChannel channel = radioChannelsLeft.get(i);
			int lowest = -1;
			int lowestBandwidth = Integer.MAX_VALUE;
			// Find lowest total bandwidth
			for (int j = 0; j < teams; j++) {
				if (totalChannels[j] < maxChannelsPerTeam
						&& totalBandwidths[j] < lowestBandwidth) {
					lowest = j;
					lowestBandwidth = totalBandwidths[j];
				}
			}
			// Add channel to lowest
			if (lowest != -1) {
				allocated.get(lowest).add(channel);
				totalBandwidths[lowest] += channel.getEffectiveBandwidth();
				totalChannels[lowest]++;
			} else {
				// Capacity reached
				break;
			}
		}
		return allocated;
	}

	@Override
	public Map<MessageChannel, List<Message>> distributeMessages(
			List<Message> messagesToOwnTeam,
			List<Message> messagesToOtherTeams, ISimulationTimer timer) {
		mapOut.clear();
		// if (myTeamOut != null) {
		mapOut.put(myTeamOut, messagesToOwnTeam);
		// } else {
		// if (messagesToOwnTeam.size() > 0) {
		// LOGGER.error("Cannot send any messages as channel is null: "
		// + messagesToOwnTeam);
		// }
		// }
		if (messagesToOtherTeams.size() > 0) {
			LOGGER.error("Cannot send messages to other team. Tried: "
					+ messagesToOtherTeams);
		}
		return mapOut;
	}

	@Override
	public List<MessageChannel> getChannelsToOtherTeams() {
		return channelsToOtherTeams;
	}

	@Override
	public List<MessageChannel> getChannelsToOwnTeam() {
		return channelsToOwnTeam;
	}

	@Override
	public List<MessageChannel> getChannelsToSubscribeTo() {
		return mySubscribed;
	}

	@Override
	public StandardEntityURN getMyRole() {
		return configuration.getAgentType();
	}

	@Override
	public IMessagingSchedule getScheduler() {
		return scheduler;
	}

	@Override
	public void reinitialiseTeam(Collection<EntityID> toIgnore) {
		myTeamConfig.reinitialise(toIgnore);
		finaliseAllocation();
	}

	public static class MultiChannelScheduler implements IMessagingSchedule {
		private Map<MessageChannel, IMessagingSchedule> map = new FastMap<MessageChannel, IMessagingSchedule>();

		public MultiChannelScheduler(List<MessageChannel> voiceChannels) {
			// System.out.println("Voice channels: " + voiceChannels);
			for (MessageChannel channel : voiceChannels) {
				map.put(channel, new IMessagingSchedule() {

					@Override
					public int getMaximumRepetitions(MessageChannel channel,
							int time) {
						return 0;
					}

					@Override
					public int getAllocatedTotalBandwidth(
							MessageChannel channel, int time) {
						if (channel.getBandwidth() == null) {
							// Some high number (this happens for voice channels
							return getAllocatedMessagesSize(channel, time)
									* getAllocatedMessagesCount(channel, time);
						} else {
							return channel.getBandwidth();
						}
					}

					@Override
					public int getAllocatedMessagesSize(MessageChannel channel,
							int time) {
						return channel.getMaxMessageSize();
					}

					@Override
					public int getAllocatedMessagesCount(
							MessageChannel channel, int timestep) {
						return channel.getMaxMessageCount();
					}
				});
			}
		}

		public void addSchedule(MessageChannel channel,
				IMessagingSchedule schedule) {
			map.put(channel, schedule);
		}

		@Override
		public int getAllocatedMessagesCount(MessageChannel channel,
				int timestep) {

			IMessagingSchedule schedule = map.get(channel);
			if (schedule != null) {
				return schedule.getAllocatedMessagesCount(channel, timestep);
			} else {
				return 0;
			}
		}

		@Override
		public int getAllocatedMessagesSize(MessageChannel channel, int time) {
			IMessagingSchedule schedule = map.get(channel);
			if (schedule != null) {
				return schedule.getAllocatedMessagesSize(channel, time);
			} else {
				return 0;
			}
		}

		@Override
		public int getAllocatedTotalBandwidth(MessageChannel channel, int time) {
			IMessagingSchedule schedule = map.get(channel);
			if (schedule != null) {
				return schedule.getAllocatedTotalBandwidth(channel, time);
			} else {
				return 0;
			}
		}

		@Override
		public int getMaximumRepetitions(MessageChannel channel, int time) {
			IMessagingSchedule schedule = map.get(channel);
			if (schedule != null) {
				return schedule.getMaximumRepetitions(channel, time);
			} else {
				return 0;
			}
		}

	}

	@Override
	public boolean amICentre() {
		EntityID myID = configuration.getEntityID();
		return myTeamConfig.getCentres().contains(myID);
	}

	@Override
	public Collection<EntityID> getMyCentres() {
		return myTeamConfig.getCentres();
	}
}
