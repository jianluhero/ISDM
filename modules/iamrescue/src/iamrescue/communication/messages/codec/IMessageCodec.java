package iamrescue.communication.messages.codec;

import iamrescue.communication.BitStream;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.messages.MessagePrefixes;
import rescuecore2.worldmodel.EntityID;

public interface IMessageCodec<T extends Message> {

	T decode(EntityID senderAgentID, MessageChannel channel,
			int timestepReceived, BitStream bitStream,
			ICommunicationBeliefBaseAdapter beliefBase) throws Exception;

	BitStream encode(T message, ICommunicationBeliefBaseAdapter beliefBase);

	/**
	 * The message prefix uniquely defines the type of the message. It is
	 * prepended to the encoded byte stream to enable the receiving agent to
	 * decode it with the appropriate decoder.
	 * 
	 * See {@link MessagePrefixes}.
	 * 
	 * @return the message prefix of this message type
	 */
	byte getMessagePrefix();

	// byte[] encode(T message);
}
