package iamrescue.communication;

import iamrescue.communication.messages.Message;

public class InstanceOfMessageFilter implements IMessageFilter {

	private Class<?> clazz;

	public InstanceOfMessageFilter(Class<?> clazz) {
		this.clazz = clazz;
	}

	public boolean evaluate(Message message) {
		return clazz.isInstance(message);
	}
}
