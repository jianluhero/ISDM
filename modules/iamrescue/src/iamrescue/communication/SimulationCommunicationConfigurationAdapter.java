package iamrescue.communication;

import iamrescue.belief.IAMWorldModel;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.messages.MessageChannelConfiguration;
import iamrescue.communication.messages.MessageChannelType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rescuecore2.config.Config;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public class SimulationCommunicationConfigurationAdapter extends
		ASimulationCommunicationConfiguration {

	private static final String COMMS_CHANNELS_MAX_PLATOON = "comms.channels.max.platoon";
	private static final String COMMS_CHANNELS_MAX_CENTRE = "comms.channels.max.centre";
	private static final String COMMS_CHANNELS_COUNT = "comms.channels.count";
	private Config config;
	private ArrayList<MessageChannel> channels;
	private IAMWorldModel worldModel;
	private Map<StandardEntityURN, Collection<StandardEntity>> centresByType;
	private Map<StandardEntityURN, Collection<StandardEntity>> platoonsByType;
	private MessageChannelConfiguration channelConfig;

	public SimulationCommunicationConfigurationAdapter(Config config,
			EntityID id, StandardEntityURN myType, IAMWorldModel iamWorldModel) {
		super(id, myType);
		this.config = config;
		this.worldModel = iamWorldModel;
	}

	@Override
	public Config getConfig() {
		return config;
	}

	@Override
	public int getChannelCount() {
		return config.getIntValue(COMMS_CHANNELS_COUNT);
	}

	@Override
	public int getMaxListenChannelCountCentre() {
		return getConfigIntValue(COMMS_CHANNELS_MAX_CENTRE)
				+ getVoiceChannels().size();
	}

	@Override
	public int getMaxListenChannelCountPlatoon() {
		return getConfigIntValue(COMMS_CHANNELS_MAX_PLATOON)
				+ getVoiceChannels().size();
	};

	@Override
	public List<MessageChannel> getChannels() {
		if (channels == null) {
			channels = new ArrayList<MessageChannel>();
			channelConfig = new MessageChannelConfiguration();

			for (int i = 0; i < getChannelCount(); i++) {
				channels.add(getMessageChannel(i));
			}
		}
		return channels;
	}

	private MessageChannel getMessageChannel(int i) {
		String rootKey = "comms.channels." + i;

		String maxMessageCountKey = rootKey + ".messages.max";
		String maxMessageSizeKey = rootKey + ".messages.size";
		String rangeKey = rootKey + ".range";
		String typeKey = rootKey + ".type";
		String bandwidthKey = rootKey + ".bandwidth";
		String useInputDropOutKey = rootKey + ".noise.input.dropout.use";
		String useOutputFailureKey = rootKey + ".noise.output.failure.use";
		String inputDropOutKey = rootKey + ".noise.input.dropout.p";
		String outputFailureKey = rootKey + ".noise.output.failure.p";
		String useOutputDropOutKey = rootKey + ".noise.output.dropout.use";
		String useInputFailureKey = rootKey + ".noise.input.failure.use";
		String outputDropOutKey = rootKey + ".noise.output.dropout.p";
		String inputFailureKey = rootKey + ".noise.input.failure.p";

		// System.out.println(getConfigIntValue(maxMessageCountKey));
		// System.out.println(getConfigIntValue(maxMessageSizeKey));
		// System.out.println(getConfigIntValue(rangeKey));
		// System.out.println(getConfigStringValue(noiseKey));
		// System.out.println(getConfigStringValue(typeKey));
		// System.out.println(getConfigIntValue(bandwidthKey));

		MessageChannel messageChannel = new MessageChannel(i);

		channelConfig.put(messageChannel);

		messageChannel
				.setMessageMaxCount(getConfigIntValue(maxMessageCountKey));
		messageChannel.setMessageMaxSize(getConfigIntValue(maxMessageSizeKey));
		messageChannel.setType(MessageChannelType.valueOf(getConfigStringValue(
				typeKey).toUpperCase()));
		messageChannel.setRange(getConfigIntValue(rangeKey));

		messageChannel.setBandwidth(getConfigIntValue(bandwidthKey));

		// Check noise
		String useInputDropOut = config.getValue(useInputDropOutKey, "no");
		if (useInputDropOut.equals("yes") || useInputDropOut.equals("true")) {
			messageChannel.setInputDropoutProbability(config.getFloatValue(
					inputDropOutKey, 0));
		} else {
			messageChannel.setInputDropoutProbability(0);
		}

		String useOutputFailure = config.getValue(useOutputFailureKey, "no");
		if (useOutputFailure.equals("yes") || useOutputFailure.equals("true")) {
			messageChannel.setOutputFailureProbability(config.getFloatValue(
					outputFailureKey, 0));
		} else {
			messageChannel.setOutputFailureProbability(0);
		}

		String useInputFailure = config.getValue(useInputFailureKey, "no");
		if (useInputFailure.equals("yes") || useInputFailure.equals("true")) {
			messageChannel.setInputFailureProbability(config.getFloatValue(
					inputFailureKey, 0));
		} else {
			messageChannel.setInputFailureProbability(0);
		}

		String useOutputDropOut = config.getValue(useOutputDropOutKey, "no");
		if (useOutputDropOut.equals("yes") || useOutputDropOut.equals("true")) {
			messageChannel.setOutputDropoutProbability(config.getFloatValue(
					outputDropOutKey, 0));
		} else {
			messageChannel.setOutputDropoutProbability(0);
		}

		return messageChannel;
	}

	private Integer getConfigIntValue(String key) {
		if (config.isDefined(key)) {
			return config.getIntValue(key);
		}
		return null;
	}

	private String getConfigStringValue(String key) {
		if (config.isDefined(key)) {
			return config.getValue(key);
		}
		return null;
	}

	@Override
	public Map<StandardEntityURN, Collection<StandardEntity>> getAgentsByType() {
		return worldModel.getRescueAgents();
	}

	@Override
	public Map<StandardEntityURN, Collection<StandardEntity>> getCentresByType() {
		if (centresByType == null) {
			centresByType = new HashMap<StandardEntityURN, Collection<StandardEntity>>();
			centresByType.put(StandardEntityURN.AMBULANCE_CENTRE, worldModel
					.getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE));
			centresByType.put(StandardEntityURN.FIRE_STATION, worldModel
					.getEntitiesOfType(StandardEntityURN.FIRE_STATION));
			centresByType.put(StandardEntityURN.POLICE_OFFICE, worldModel
					.getEntitiesOfType(StandardEntityURN.POLICE_OFFICE));
		}
		return centresByType;
	}

	@Override
	public Map<StandardEntityURN, Collection<StandardEntity>> getPlatoonsByType() {
		if (platoonsByType == null) {
			platoonsByType = new HashMap<StandardEntityURN, Collection<StandardEntity>>();
			platoonsByType.put(StandardEntityURN.AMBULANCE_TEAM, worldModel
					.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM));
			platoonsByType.put(StandardEntityURN.FIRE_BRIGADE, worldModel
					.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE));
			platoonsByType.put(StandardEntityURN.POLICE_FORCE, worldModel
					.getEntitiesOfType(StandardEntityURN.POLICE_FORCE));
		}
		return platoonsByType;
	}

	@Override
	public MessageChannelConfiguration getChannelConfiguration() {
		return channelConfig;
	}
}
