package iamrescue.communication;

import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;

import java.util.List;
import java.util.Map;


/**
 * An IOutgoingMessageSelector selects messages that should be sent, and send
 * them on an {@link IOutgoingMessageService}. Basically, it acts as a filter
 * 
 * @author rs06r
 * 
 */
public interface IOutgoingMessageSelector {

	// void sendShoutMessages(Collection<Message> shoutMessageQ);

	void sendMessages(Map<MessageChannel, List<Message>> messageQs);

	void setOutgoingMessageService(
			IOutgoingMessageService outgoingMessageService);

}
