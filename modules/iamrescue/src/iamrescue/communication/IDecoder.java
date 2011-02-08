package iamrescue.communication;

import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.messages.codec.ICommunicationBeliefBaseAdapter;
import iamrescue.communication.messages.codec.UnknownMessageFormatException;

import java.util.List;

import rescuecore2.worldmodel.EntityID;

public interface IDecoder {

	public boolean canDecode(EntityID senderAgentID, MessageChannel channel,
			int timestep, byte[] messageContents,
			ICommunicationBeliefBaseAdapter beliefBase);

	public List<Message> decode(EntityID senderAgentID, MessageChannel channel,
			int timestep, byte[] messageContents,
			ICommunicationBeliefBaseAdapter beliefBase)
			throws UnknownMessageFormatException;
}
