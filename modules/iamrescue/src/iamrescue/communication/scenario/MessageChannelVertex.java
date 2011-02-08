package iamrescue.communication.scenario;

import iamrescue.communication.messages.MessageChannel;

public class MessageChannelVertex implements CommunicationGraphVertex {

	private MessageChannel channel;

	public MessageChannelVertex(MessageChannel channel) {
		this.channel = channel;
	}

	public MessageChannel getChannel() {
		return channel;
	}

}
