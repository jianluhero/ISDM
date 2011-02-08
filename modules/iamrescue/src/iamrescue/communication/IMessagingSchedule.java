package iamrescue.communication;

import iamrescue.communication.messages.MessageChannel;

/**
 * Determines how many messages and of what size to send to which channel for
 * every timestep
 * 
 * @author rs06r
 * 
 */
public interface IMessagingSchedule {

	int getAllocatedMessagesCount(MessageChannel channel, int timestep);

	int getAllocatedMessagesSize(MessageChannel channel, int time);
	
	int getMaximumRepetitions(MessageChannel channel, int time);

	int getAllocatedTotalBandwidth(MessageChannel channel, int time);
}
