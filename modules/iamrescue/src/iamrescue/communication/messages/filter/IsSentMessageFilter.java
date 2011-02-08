package iamrescue.communication.messages.filter;

import iamrescue.communication.messages.Message;

import org.apache.commons.collections15.Predicate;


public class IsSentMessageFilter implements Predicate<Message> {

	public boolean evaluate(Message message) {
		return message.isSent();
	}

}
