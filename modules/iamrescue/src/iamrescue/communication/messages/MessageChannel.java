package iamrescue.communication.messages;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

public class MessageChannel {

	//private static Map<Integer, MessageChannel> channels = new HashMap<Integer, MessageChannel>();
	private MessageChannelType type;
	private Integer range;
	private Integer maxMessageSize;
	private Integer maxMessageCount;
	private Integer bandwidth;
	private int channelNumber;
	private double outputFailureProbability = 0;
	private double inputDropoutProbability = 0;
	private double inputFailureProbability = 0;
	private double outputDropoutProbability = 0;

	public MessageChannel(int channelNumber) {
		this.channelNumber = channelNumber;
	}

	public MessageChannel(int i, MessageChannelType type) {
		this(i);
		setType(type);
	}

	public int getEffectiveBandwidth() {
		double failureProbability = getOverallFailureProbability();
		return (int) (bandwidth * (1 - failureProbability));
	}

	public double getOverallFailureProbability() {
		return 1 - (1 - outputFailureProbability)
				* (1 - outputDropoutProbability)
				* (1 - inputFailureProbability) * (1 - inputDropoutProbability);
	}

	public double getOutputFailureProbability() {
		return outputFailureProbability;
	}

	public void setOutputFailureProbability(double outputFailureProbability) {
		this.outputFailureProbability = outputFailureProbability;
	}

	public double getInputDropoutProbability() {
		return inputDropoutProbability;
	}

	public void setInputDropoutProbability(double inputDropoutProbability) {
		this.inputDropoutProbability = inputDropoutProbability;
	}

	public double getInputFailureProbability() {
		return inputFailureProbability;
	}

	public void setInputFailureProbability(double inputFailureProbability) {
		this.inputFailureProbability = inputFailureProbability;
	}

	public double getOutputDropoutProbability() {
		return outputDropoutProbability;
	}

	public void setOutputDropoutProbability(double outputDropoutProbability) {
		this.outputDropoutProbability = outputDropoutProbability;
	}

	public MessageChannelType getType() {
		return type;
	}

	public Integer getRange() {
		return range;
	}

	public void setType(MessageChannelType type) {
		this.type = type;
	}

	public void setRange(Integer range) {
		this.range = range;
	}

	public void setMessageMaxSize(Integer maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	public void setMessageMaxCount(Integer maxMessageCount) {
		this.maxMessageCount = maxMessageCount;
	}

	public Integer getMaxMessageCount() {
		return maxMessageCount;
	}

	public Integer getMaxMessageSize() {
		return maxMessageSize;
	}

	public void setBandwidth(Integer bandwidth) {
		this.bandwidth = bandwidth;
	}

	public Integer getBandwidth() {
		return bandwidth;
	}

	public int getChannelNumber() {
		return channelNumber;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.reflectionToString(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MessageChannel) {
			MessageChannel channel = (MessageChannel) obj;
			return channel.getChannelNumber() == getChannelNumber();
		}

		return false;
	}

	@Override
	public int hashCode() {
		return getChannelNumber() * 31;
	}

/*	public static MessageChannel get(int channel, MessageChannelType type) {
		if (!channels.containsKey(channel)) {
			channels.put(channel, new MessageChannel(channel, type));
		}

		return channels.get(channel);
	}*/

}
