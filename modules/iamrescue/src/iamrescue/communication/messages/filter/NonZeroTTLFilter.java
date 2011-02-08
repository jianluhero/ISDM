package iamrescue.communication.messages.filter;

import iamrescue.communication.messages.Message;

import org.apache.commons.collections15.Predicate;


public class NonZeroTTLFilter implements Predicate<Message> {

	public boolean evaluate(Message message) {
		return message.getTTL() > 0;
	}

}
