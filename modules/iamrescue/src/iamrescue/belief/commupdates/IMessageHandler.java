/**
 * 
 */
package iamrescue.belief.commupdates;

import iamrescue.communication.messages.Message;

/**
 * @author Sebastian
 * 
 */
public interface IMessageHandler {
	/**
	 * 
	 * @param message
	 * @return True iff this caused a change in the agent's world model
	 */
	public boolean handleMessage(Message message);

	public boolean canHandle(Message message);
}
