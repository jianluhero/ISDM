package iamrescue.communication.scenario.scenarios;

import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.messages.MessageChannelType;
import iamrescue.communication.scenario.ChannelDividerUtil;
import iamrescue.util.comparators.EntityIDComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import javolution.util.FastMap;
import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.FireStation;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

public class IAMTeamCommunicationConfiguration {

	public static final int RELATIVE_CENTRE_BANDIWDTH = 5;

	private static Logger LOGGER = Logger
			.getLogger(IAMTeamCommunicationConfiguration.class);

	private List<MessageChannel> mainCommunicationChannels;
	private List<MessageChannel> overflowChannels;

	private Map<EntityID, MessageChannel> outputMainChannels;
	private Map<EntityID, MessageChannel> outputOverflowChannels;

	private Map<MessageChannel, List<EntityID>> channelSenders;

	private Map<EntityID, List<MessageChannel>> subscribedChannels;

	private Set<EntityID> centres;
	private Set<EntityID> platoons;
	private ConfigInfo configInfo;

	private Set<EntityID> ignoredEntities = new FastSet<EntityID>();

	private IAMTeamCommunicationConfiguration(ConfigInfo configInfo) {
		this.configInfo = configInfo;
	}

	public void setIgnoredEntities(Collection<EntityID> ignored) {
		ignoredEntities.clear();
		ignoredEntities.addAll(ignored);
	}

	public Map<EntityID, MessageChannel> getOutputMainChannels() {
		return outputMainChannels;
	}

	public Map<EntityID, MessageChannel> getOutputOverflowChannels() {
		return outputOverflowChannels;
	}

	public Map<MessageChannel, List<EntityID>> getChannelSenders() {
		return channelSenders;
	}

	public Map<EntityID, List<MessageChannel>> getSubscribedChannels() {
		return subscribedChannels;
	}

	public Set<EntityID> getCentres() {
		return centres;
	}

	public Set<EntityID> getPlatoons() {
		return platoons;
	}

	public List<MessageChannel> getMainCommunicationChannels() {
		return mainCommunicationChannels;
	}

	public List<MessageChannel> getOverflowChannels() {
		return overflowChannels;
	}

	public static IAMTeamCommunicationConfiguration createTeam(
			List<MessageChannel> mainChannels,
			List<MessageChannel> overflowChannels,
			List<MessageChannel> highPriorityTeamsToListen,
			List<MessageChannel> lowPriorityTeamsToListen,
			List<MessageChannel> veryLowPriorityTeamsToListen,
			Collection<StandardEntity> entities, int maxPlatoonChannels,
			int maxCentreChannels) {

		ConfigInfo configInfo = new ConfigInfo(mainChannels, overflowChannels,
				highPriorityTeamsToListen, lowPriorityTeamsToListen,
				veryLowPriorityTeamsToListen, entities, maxPlatoonChannels,
				maxCentreChannels);

		IAMTeamCommunicationConfiguration config = new IAMTeamCommunicationConfiguration(
				configInfo);

		config.initialise();

		return config;
	}

	public void reinitialise(Collection<EntityID> ignoredEntities) {
		setIgnoredEntities(ignoredEntities);
		initialise();
	}

	private void initialise() {

		List<MessageChannel> mainChannels = configInfo.getMainChannels();
		List<MessageChannel> overflowChannels = configInfo
				.getOverflowChannels();
		List<MessageChannel> highPriorityTeamsToListen = configInfo
				.getHighPriorityTeamsToListen();
		List<MessageChannel> lowPriorityTeamsToListen = configInfo
				.getLowPriorityTeamsToListen();
		List<MessageChannel> veryLowPriorityTeamsToListen = configInfo
				.getVeryLowPriorityTeamsToListen();
		int maxPlatoonChannels = configInfo.getMaxPlatoonChannels();
		int maxCentreChannels = configInfo.getMaxCentreChannels();
		Collection<StandardEntity> entities = new ArrayList<StandardEntity>();

		for (StandardEntity se : configInfo.getEntities()) {
			if (!ignoredEntities.contains(se.getID())) {
				entities.add(se);
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Starting team configuration with main channels "
					+ mainChannels + ", overflow channels: " + overflowChannels
					+ ", high priority " + highPriorityTeamsToListen + ", low "
					+ lowPriorityTeamsToListen + ", very low: "
					+ veryLowPriorityTeamsToListen + ", max platoon: "
					+ maxPlatoonChannels + ", maxCentre: " + maxCentreChannels
					+ ",  entities: " + entities);
		}

		this.mainCommunicationChannels = mainChannels;
		this.centres = new FastSet<EntityID>();
		this.subscribedChannels = new FastMap<EntityID, List<MessageChannel>>();
		this.outputMainChannels = new FastMap<EntityID, MessageChannel>();
		this.outputOverflowChannels = new FastMap<EntityID, MessageChannel>();
		this.overflowChannels = overflowChannels;
		List<StandardEntity> platoons = new ArrayList<StandardEntity>();
		List<StandardEntity> centres = new ArrayList<StandardEntity>();

		for (StandardEntity entity : entities) {
			if (entity instanceof FireBrigade || entity instanceof PoliceForce
					|| entity instanceof AmbulanceTeam) {
				platoons.add(entity);
			} else {
				centres.add(entity);
			}
		}

		Collections.sort(platoons, new EntityIDComparator());
		Collections.sort(centres, new EntityIDComparator());

		// How many normal platoon agents need to additionally listen to other
		// teams.
		int needToListen = 0;
		int channelsEach = 1;

		List<MessageChannel> channelsToListenTo = new ArrayList<MessageChannel>();
		channelsToListenTo.addAll(highPriorityTeamsToListen);
		channelsToListenTo.addAll(mainChannels);
		channelsToListenTo.addAll(overflowChannels);
		channelsToListenTo.addAll(lowPriorityTeamsToListen);
		channelsToListenTo.addAll(veryLowPriorityTeamsToListen);

		if (channelsToListenTo.size() > maxPlatoonChannels) {
			// Only do centres if they are required.
			if (centres.size() > 0) {
				// There are centres
				// First do high priority channels

				int index = 0;
				for (int i = 0; i < centres.size(); i++) {
					List<MessageChannel> toSubscribe = new ArrayList<MessageChannel>();
					while (toSubscribe.size() < maxCentreChannels
							&& index < channelsToListenTo.size()) {
						toSubscribe.add(channelsToListenTo.get(index++));
					}
					if (toSubscribe.size() > 0) {
						if (toSubscribe.size() < maxCentreChannels) {
							// Fill up with own channels if still space
							Iterator<MessageChannel> iterator = mainChannels
									.iterator();
							while (iterator.hasNext()
									&& toSubscribe.size() < maxCentreChannels) {
								MessageChannel next = iterator.next();
								if (!toSubscribe.contains(next)) {
									toSubscribe.add(next);
								}
							}
						}
						EntityID id = centres.get(i).getID();
						this.centres.add(id);
						this.subscribedChannels.put(id, toSubscribe);
					}
				}

				List<StandardEntity> newCentres = new ArrayList<StandardEntity>();
				for (StandardEntity se : centres) {
					if (this.centres.contains(se.getID())) {
						newCentres.add(se);
					}
				}

				/*for (StandardEntity se : centres) {
					if (!newCentres.contains(se)) {
						newCentres.add(se);
						this.centres.add(se.getID());
						this.subscribedChannels.put(se.getID(), mainChannels);
					}
				}*/

				centres = newCentres;

				int nextGoal = highPriorityTeamsToListen.size();
				if (index < nextGoal) {
					LOGGER.error("Could not get centres to listen "
							+ "to all high priority channels.");
					this.overflowChannels = new ArrayList<MessageChannel>();
				} else {
					nextGoal += mainChannels.size();
					if (index < nextGoal) {
						LOGGER.warn("Centre is not listening to all its own "
								+ "team's channels. This can lead to "
								+ "duplicate messages.");
						this.overflowChannels = new ArrayList<MessageChannel>();
					} else {
						nextGoal += overflowChannels.size();
						if (index < nextGoal) {
							// Could not add all overflow channels
							index -= (nextGoal - overflowChannels.size());
							// index should now point to last overflow channel
							// that
							// was added
							this.overflowChannels = new ArrayList<MessageChannel>();
							for (int i = 0; i < index; i++) {
								this.overflowChannels.add(overflowChannels
										.get(i));
							}
							LOGGER.info("Added only " + index
									+ " overflow channels.");
						}
						// No need to be more verbose now.
					}
				}
			} else {
				// No centres
				// Assign several agents to just listen on the other channels
				needToListen = highPriorityTeamsToListen.size();
				channelsEach = 1;
				if (needToListen > platoons.size()) {
					// If too few agents, make one of them listen to all
					channelsEach = needToListen;
					needToListen = 1;
				}
				centres = new ArrayList<StandardEntity>();
				// int counter = 0;
				for (int i = 0; i < needToListen; i++) {
					StandardEntity newCentre = platoons.remove(0);
					centres.add(newCentre);
				}
				// No overflow channels in this case
				overflowChannels = new ArrayList<MessageChannel>();
			}
		}

		// Add any remaining centres to platoons
		boolean added = false;
		for (StandardEntity se : entities) {
			if (!centres.contains(se)) {
				if (!platoons.contains(se)) {
					// Add to platoons
					platoons.add(se);
					added = true;
				}
			}
		}
		if (added) {
			Collections.sort(platoons, new EntityIDComparator());
		}

		// Now work out assignments for main channel.
		Map<MessageChannel, List<EntityID>> divideAgents = ChannelDividerUtil
				.divideAgents(mainChannels, ChannelDividerUtil
						.convertToIDs(platoons), ChannelDividerUtil
						.convertToIDs(centres), RELATIVE_CENTRE_BANDIWDTH);

		// Store this
		for (Entry<MessageChannel, List<EntityID>> entry : divideAgents
				.entrySet()) {
			MessageChannel channel = entry.getKey();
			List<EntityID> listeners = entry.getValue();
			for (EntityID entityID : listeners) {
				this.outputMainChannels.put(entityID, channel);
			}
		}

		// Ensure centres are listening to their own allocated channels where
		// possible
		if (needToListen == 0) {
			// Only do for real centres, otherwise next if statement takes care
			// of this.
			Map<EntityID, MessageChannel> needed = new FastMap<EntityID, MessageChannel>();
			Map<EntityID, Set<MessageChannel>> surplus = new FastMap<EntityID, Set<MessageChannel>>();
			for (EntityID centreID : this.centres) {
				surplus.put(centreID, new FastSet<MessageChannel>());

				MessageChannel messageChannel = this.outputMainChannels
						.get(centreID);

				needed.put(centreID, messageChannel);

				Set<MessageChannel> mainCommsChannels = new FastSet<MessageChannel>();
				mainCommsChannels.addAll(this.getMainCommunicationChannels());

				boolean ok = false;
				for (MessageChannel already : this.getSubscribedChannels().get(
						centreID)) {
					if (already.equals(messageChannel)) {
						ok = true;
					} else if (mainCommsChannels.contains(already)) {
						surplus.get(centreID).add(already);
					}
				}
				if (ok) {
					needed.remove(centreID);
				}
			}

			// Now swap

			for (EntityID centreID : this.centres) {
				MessageChannel neededChannel = needed.get(centreID);
				if (neededChannel != null) {
					for (EntityID otherCentreID : this.centres) {
						Set<MessageChannel> otherSurplus = surplus
								.get(otherCentreID);
						MessageChannel toSwap = null;
						if (otherSurplus.contains(neededChannel)) {
							for (MessageChannel unneeded : surplus
									.get(centreID)) {
								if (!otherSurplus.contains(unneeded)
										&& ((needed.get(otherCentreID) != null && needed
												.get(otherCentreID).equals(
														unneeded)) || !this
												.getOutputMainChannels().get(
														otherCentreID).equals(
														unneeded))) {
									toSwap = unneeded;
									break;
								}
							}
							if (toSwap != null) {
								this.getSubscribedChannels().get(centreID).add(
										neededChannel);
								this.getSubscribedChannels().get(centreID)
										.remove(toSwap);
								this.getSubscribedChannels().get(otherCentreID)
										.add(toSwap);
								this.getSubscribedChannels().get(otherCentreID)
										.remove(neededChannel);
								needed.remove(centreID);
							}
						}
					}
				}
			}
		}

		// Next, assign platoon centres (if any) to listen to other channels and
		// their own
		if (needToListen > 0) {
			int counter = 0;

			// Yes, there are platoon centres
			for (int i = 0; i < centres.size(); i++) {
				List<MessageChannel> listenTo = new ArrayList<MessageChannel>();
				while (listenTo.size() < maxPlatoonChannels
						&& counter < highPriorityTeamsToListen.size()
						&& listenTo.size() < channelsEach) {
					listenTo.add(highPriorityTeamsToListen.get(counter++));
				}
				if (listenTo.size() > 0 && listenTo.size() < maxPlatoonChannels) {
					MessageChannel myChannel = this.outputMainChannels
							.get(centres.get(i).getID());

					// Add own channel if possible (for error detection)
					listenTo.add(myChannel);

					if (listenTo.size() < maxPlatoonChannels) {
						List<MessageChannel> remaining = new ArrayList<MessageChannel>(
								mainChannels);
						Collections.shuffle(remaining, new Random(1234));
						counter = 0;
						while (listenTo.size() < maxPlatoonChannels) {
							if (counter < remaining.size()) {
								MessageChannel channel = remaining
										.get(counter++);
								if (channel.equals(myChannel)) {
									// Already subscribed
									continue;
								} else {
									// Add channel
									listenTo.add(channel);
								}
							} else {
								break;
							}
						}
					}

				}

				this.centres.add(centres.get(i).getID());
				this.subscribedChannels.put(centres.get(i).getID(), listenTo);
			}
		} // Done allocating channels to platoon centres

		// Work out subscribed channels to listen to for everyone else
		for (int i = 0; i < platoons.size(); i++) {
			List<MessageChannel> list;
			StandardEntity se = platoons.get(i);
			if (maxPlatoonChannels >= mainChannels.size()) {
				list = new ArrayList<MessageChannel>(mainChannels);
			} else {
				list = new ArrayList<MessageChannel>();
				list.add(this.outputMainChannels.get(se.getID()));
				int counter = 0;
				List<MessageChannel> channels = new ArrayList<MessageChannel>();
				channels.addAll(mainChannels);
				Collections
						.shuffle(channels, new Random(se.getID().getValue()));
				for (int j = 1; j < maxPlatoonChannels; j++) {
					MessageChannel messageChannel = channels.get(counter++);
					if (messageChannel.equals(list.get(0))) {
						j--;
						continue;
					}
					list.add(messageChannel);
				}
			}
			int counter = 0;
			while (list.size() < maxPlatoonChannels) {
				if (counter < highPriorityTeamsToListen.size()) {
					list.add(highPriorityTeamsToListen.get(counter++));
				} else {
					int index = counter - highPriorityTeamsToListen.size();
					if (index < lowPriorityTeamsToListen.size()) {
						list.add(lowPriorityTeamsToListen.get(index));
						counter++;
					} else {
						index = counter - highPriorityTeamsToListen.size()
								- lowPriorityTeamsToListen.size();
						if (index < veryLowPriorityTeamsToListen.size()) {
							list.add(veryLowPriorityTeamsToListen.get(index));
							counter++;
						} else {
							break;
						}
					}
				}
			}
			this.subscribedChannels.put(se.getID(), list);
		}

		// Then, work out overflow channel allocations
		Map<MessageChannel, List<EntityID>> overflowAssignment = ChannelDividerUtil
				.divideAgents(this.overflowChannels, ChannelDividerUtil
						.convertToIDs(platoons), new ArrayList<EntityID>(), 1);

		// Save all in scenario
		for (Entry<MessageChannel, List<EntityID>> entry : overflowAssignment
				.entrySet()) {
			MessageChannel channel = entry.getKey();
			List<EntityID> listeners = entry.getValue();
			for (EntityID entityID : listeners) {
				this.outputOverflowChannels.put(entityID, channel);
			}
		}

		this.platoons = new FastSet<EntityID>();
		this.platoons.addAll(ChannelDividerUtil.convertToIDs(platoons));

		this.updateChannelSenders();
	}

	/**
	 * 
	 */
	private void updateChannelSenders() {
		channelSenders = new FastMap<MessageChannel, List<EntityID>>();
		for (int i = 0; i < 2; i++) {
			Set<Entry<EntityID, MessageChannel>> entrySet = (i == 0) ? outputMainChannels
					.entrySet()
					: outputOverflowChannels.entrySet();
			for (Entry<EntityID, MessageChannel> entry : entrySet) {
				MessageChannel channel = entry.getValue();
				EntityID sender = entry.getKey();
				List<EntityID> list = channelSenders.get(channel);
				if (list == null) {
					list = new ArrayList<EntityID>();
					channelSenders.put(channel, list);
				}
				list.add(sender);
			}
		}
	}

	public String toVerboseString() {
		StringBuffer sb = new StringBuffer("Allocation: ");
		sb.append('\n');
		sb.append("\nPlatoons: ");
		sb.append(platoons.toString());
		sb.append('\n');
		sb.append("\nCentres: ");
		sb.append(centres.toString());
		sb.append('\n');
		sb.append("\nAllocation:");
		sb.append('\n');
		for (EntityID id : centres) {
			MessageChannel overflow = outputOverflowChannels.get(id);
			String overflowStr = overflow == null ? "none" : overflow
					.getChannelNumber()
					+ "";
			sb.append("Centre " + id + " -> main: "
					+ outputMainChannels.get(id).getChannelNumber()
					+ ", overflow: " + overflowStr + "\n");
			sb.append("Centre " + id + " <-");
			List<MessageChannel> list = subscribedChannels.get(id);
			for (MessageChannel messageChannel : list) {
				sb.append(" " + messageChannel.getChannelNumber());
			}
			sb.append("\n");
		}
		for (EntityID id : platoons) {
			MessageChannel overflow = outputOverflowChannels.get(id);
			String overflowStr = overflow == null ? "none" : overflow
					.getChannelNumber()
					+ "";
			sb.append("Platoon " + id + " -> main: "
					+ outputMainChannels.get(id).getChannelNumber()
					+ ", overflow: " + overflowStr + "\n");
			sb.append("Platoon " + id + " <-");
			List<MessageChannel> list = subscribedChannels.get(id);
			for (MessageChannel messageChannel : list) {
				sb.append(" " + messageChannel.getChannelNumber());
			}
			sb.append("\n");
		}
		sb.append("\nChannels:\n");
		sb.append("\nMain Channels:\n");
		for (MessageChannel channel : mainCommunicationChannels) {
			sb.append(channel.getChannelNumber() + " <- "
					+ channelSenders.get(channel) + "\n");
		}
		sb.append("\nOverflow Channels:\n");
		for (MessageChannel channel : overflowChannels) {
			sb.append("Channel " + channel.getChannelNumber() + " <- "
					+ channelSenders.get(channel) + "\n");
		}
		return sb.toString();
	}

	public static void main(String[] args) {
		List<StandardEntity> agents = new ArrayList<StandardEntity>();
		agents.add(new FireBrigade(new EntityID(1)));
		agents.add(new FireBrigade(new EntityID(2)));
		agents.add(new FireBrigade(new EntityID(3)));
		agents.add(new FireBrigade(new EntityID(4)));

		agents.add(new FireStation(new EntityID(6)));
		agents.add(new FireStation(new EntityID(5)));
		agents.add(new FireStation(new EntityID(7)));

		// MessageChannelConfig

		MessageChannel mc1 = new MessageChannel(1, MessageChannelType.RADIO);
		mc1.setBandwidth(1000);
		mc1.setInputDropoutProbability(0.5);
		MessageChannel mc2 = new MessageChannel(2, MessageChannelType.RADIO);
		mc2.setBandwidth(900);
		MessageChannel mc3 = new MessageChannel(3, MessageChannelType.RADIO);
		mc3.setBandwidth(800);

		MessageChannel other1 = new MessageChannel(4, MessageChannelType.RADIO);
		other1.setBandwidth(800);
		MessageChannel other2 = new MessageChannel(5, MessageChannelType.RADIO);
		other2.setBandwidth(750);

		MessageChannel other3 = new MessageChannel(6, MessageChannelType.RADIO);
		other3.setBandwidth(800);
		MessageChannel other4 = new MessageChannel(7, MessageChannelType.RADIO);
		other4.setBandwidth(750);

		List<MessageChannel> main = new ArrayList<MessageChannel>();
		main.add(mc1);
		main.add(mc2);

		List<MessageChannel> overflow = new ArrayList<MessageChannel>();
		overflow.add(mc3);

		List<MessageChannel> other = new ArrayList<MessageChannel>();
		other.add(other1);
		other.add(other2);

		List<MessageChannel> yetother = new ArrayList<MessageChannel>();
		yetother.add(other3);
		yetother.add(other4);
		IAMTeamCommunicationConfiguration team = IAMTeamCommunicationConfiguration
				.createTeam(main, overflow, other, yetother,
						new ArrayList<MessageChannel>(), agents, 1, 3);
		System.out.println(team.toVerboseString());
		team.reinitialise(Collections.singleton(new EntityID(7)));
		System.out.println(team.toVerboseString());
	}

	private static class ConfigInfo {
		private List<MessageChannel> mainChannels;
		private List<MessageChannel> overflowChannels;
		private List<MessageChannel> highPriorityTeamsToListen;
		private List<MessageChannel> lowPriorityTeamsToListen;
		private List<MessageChannel> veryLowPriorityTeamsToListen;
		private Collection<StandardEntity> entities;
		private int maxPlatoonChannels;
		private int maxCentreChannels;

		public ConfigInfo(List<MessageChannel> mainChannels,
				List<MessageChannel> overflowChannels,
				List<MessageChannel> highPriorityTeamsToListen,
				List<MessageChannel> lowPriorityTeamsToListen,
				List<MessageChannel> veryLowPriorityTeamsToListen,
				Collection<StandardEntity> entities, int maxPlatoonChannels,
				int maxCentreChannels) {
			this.mainChannels = mainChannels;
			this.overflowChannels = overflowChannels;
			this.highPriorityTeamsToListen = highPriorityTeamsToListen;
			this.lowPriorityTeamsToListen = lowPriorityTeamsToListen;
			this.veryLowPriorityTeamsToListen = veryLowPriorityTeamsToListen;
			this.entities = entities;
			this.maxPlatoonChannels = maxPlatoonChannels;
			this.maxCentreChannels = maxCentreChannels;
		}

		/**
		 * @return the mainChannels
		 */
		public List<MessageChannel> getMainChannels() {
			return Collections.unmodifiableList(mainChannels);
		}

		/**
		 * @return the overflowChannels
		 */
		public List<MessageChannel> getOverflowChannels() {
			return Collections.unmodifiableList(overflowChannels);
		}

		/**
		 * @return the highPriorityTeamsToListen
		 */
		public List<MessageChannel> getHighPriorityTeamsToListen() {
			return Collections.unmodifiableList(highPriorityTeamsToListen);
		}

		/**
		 * @return the lowPriorityTeamsToListen
		 */
		public List<MessageChannel> getLowPriorityTeamsToListen() {
			return Collections.unmodifiableList(lowPriorityTeamsToListen);
		}

		/**
		 * @return the veryLowPriorityTeamsToListen
		 */
		public List<MessageChannel> getVeryLowPriorityTeamsToListen() {
			return Collections.unmodifiableList(veryLowPriorityTeamsToListen);
		}

		/**
		 * @return the entities
		 */
		public Collection<StandardEntity> getEntities() {
			return Collections.unmodifiableCollection(entities);
		}

		/**
		 * @return the maxPlatoonChannels
		 */
		public int getMaxPlatoonChannels() {
			return maxPlatoonChannels;
		}

		/**
		 * @return the maxCentreChannels
		 */
		public int getMaxCentreChannels() {
			return maxCentreChannels;
		}

	}
}