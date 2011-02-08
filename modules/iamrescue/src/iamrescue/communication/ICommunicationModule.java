package iamrescue.communication;

import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;

import java.util.Collection;
import java.util.List;

import rescuecore2.messages.Command;


public interface ICommunicationModule {

	void enqueueMessage(Message message, MessageChannel channel);

	void enqueueMessage(Message message, List<MessageChannel> channelsToCenters);

	void flushOutbox();

	Collection<Message> getUnreadMessages();

	public boolean isRadioCommunicationPossible();

	void hear(Collection<Command> heard);

	Collection<MessageChannel> getChannels();

	void subscribeToChannels(List<MessageChannel> channels);

	


}
