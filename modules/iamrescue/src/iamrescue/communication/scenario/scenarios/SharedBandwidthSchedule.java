package iamrescue.communication.scenario.scenarios;

import iamrescue.communication.IMessagingSchedule;
import iamrescue.communication.messages.MessageChannel;

public class SharedBandwidthSchedule implements IMessagingSchedule {

	private static double DEFAULT_RELIABILITY = 1;
	private static int DEFAULT_MAX_REPETITIONS = 2;
	
	private static double BW_INFLATION = 1.25;  

	private MessageChannel channel;
	// private int agents;
	private double reliability = DEFAULT_RELIABILITY;
	private int maxMessages = Integer.MAX_VALUE;
	private int maxRepetitions = DEFAULT_MAX_REPETITIONS;
	private int numRepetitions;
	private int allocation;

	// private int myProportion;

	public SharedBandwidthSchedule(MessageChannel channel, int agents) {
		this(channel, agents, 1);
		computeNumMessages();
	}

	public SharedBandwidthSchedule(MessageChannel channel, int agents,
			int myProportion) {
		this.channel = channel;
		// this.agents = agents;
		this.allocation = (myProportion * channel.getBandwidth()) / agents;
		// this.myProportion = myProportion;
		
		this.allocation = (int)(BW_INFLATION * allocation);
		computeNumMessages();
	}

	private void computeNumMessages() {
		double failureProbability = channel.getOverallFailureProbability();
		if (reliability == 0) {
			numRepetitions = 0;
		} else if (reliability == 1) {
			numRepetitions = maxRepetitions;
		} else if (failureProbability == 0) {
			numRepetitions = 0;
		} else {
			int required = (int) Math.ceil(Math.log(1 - reliability)
					/ Math.log(failureProbability));
			if (required - 1 > maxRepetitions) {
				numRepetitions = maxRepetitions;
			} else {
				numRepetitions = required - 1;
			}
		}
		// numMessages = 5;
	}

	public void setReliability(double reliability) {
		if (reliability < 0 || reliability > 1) {
			throw new IllegalArgumentException(
					"Reliability must be between 0 and 1");
		}
		this.reliability = reliability;
		computeNumMessages();
	}

	public void setMaxMessages(int maxMessages) {
		this.maxMessages = maxMessages;
		computeNumMessages();
	}

	@Override
	public int getAllocatedMessagesSize(MessageChannel channel, int time) {
		if (channel.getChannelNumber() == this.channel.getChannelNumber()) {
			return allocation;
		} else {
			return 0;
		}
	}

	@Override
	public int getAllocatedMessagesCount(MessageChannel channel, int timestep) {
		if (channel.getChannelNumber() == this.channel.getChannelNumber()) {
			return maxMessages;
		} else {
			return 0;
		}
	}

	@Override
	public int getAllocatedTotalBandwidth(MessageChannel channel, int time) {
		return getAllocatedMessagesSize(channel, time);
	}

	@Override
	public int getMaximumRepetitions(MessageChannel channel, int time) {
		if (channel.getChannelNumber() == this.channel.getChannelNumber()) {
			return numRepetitions;
		} else {
			return 0;
		}
	}
}
