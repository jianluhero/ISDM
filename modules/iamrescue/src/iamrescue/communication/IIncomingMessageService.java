package iamrescue.communication;

import iamrescue.communication.messages.MessageChannel;

import java.util.Collection;
import java.util.Set;

import rescuecore2.messages.Command;


public interface IIncomingMessageService {
	/**
	 * Returns the maximum number of channels this agent can listen to
	 * 
	 * @return
	 */
	public int getMaximumNumberofSubscribedChannels();

	public void startListeningToChannel(MessageChannel channel);

	public void stopListeningToChannel(MessageChannel channel);

	public Set<MessageChannel> getSubscribedChannels();

	public int getNumberOfChannels();

	public void hear(Collection<Command> heard);

	public void flushChannelCommands();

}
