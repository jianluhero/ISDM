package iamrescue.communication;

import iamrescue.communication.messages.MessageChannel;

/**
 * This interface is responsible for actually sending a message on an outgoing
 * channel. An implementation of this interface should interact with the
 * specific api of the simulator to ensure a message gets delivered
 * 
 * @author rs06r
 * 
 */
public interface IOutgoingMessageService {

	// public void sendShoutMessage(byte[] message);

	public void sendMessage(byte[] message, MessageChannel channel);

	/**
	 * Gets the maximum number of radio messages that can be sent, given the
	 * number of platoon agents of the same type
	 * 
	 * @param platoonCount
	 *            the number of platoon agents of the same type as the agent, or
	 *            in case of a center, the type of the agents associated to it
	 * @return
	 */
	// public int getMaximumRadioMessageCount();

	/**
	 * Gets the maximum allowable size of a 'say' message in bytes
	 * 
	 * @return
	 */
	// public int getMaximumRadioMessageLength();

	/**
	 * Gets the maximum number of shout messages that can be sent
	 * 
	 * @return
	 */
	// public int getMaximumShoutMessageCount();

	/**
	 * Gets the maximum allowable size of a 'tell' message in bytes
	 * 
	 * @return
	 */
	// public int getMaximumShoutMessageLength();

}
