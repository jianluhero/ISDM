/**
 * 
 */
package iamrescue.belief.commupdates;

import iamrescue.communication.messages.Message;

import java.util.List;

/**
 * @author Sebastian
 * 
 */
public interface IWorldModelCommsUpdater {

	public void addUpdateHandler(IMessageHandler handler);

	public List<Message> update();
}
