package iamrescue.communication.messages.codec;

import iamrescue.communication.BitStream;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;
import rescuecore2.worldmodel.EntityID;

public abstract class AbstractMessageCodec<T extends Message> implements
		IMessageCodec<T> {

	// private static Log log = LogFactory.getLog(AbstractMessageCodec.class);

	@Override
	public BitStream encode(T message,
			ICommunicationBeliefBaseAdapter beliefBase) {
		BitStreamEncoder encoder = new BitStreamEncoder(beliefBase);
		encoder.appendByte(getMessagePrefix());

		// let the concrete subclass encode the rest of the message
		encodeMessage(message, encoder);

		return encoder.getBitStream();
	}

	@Override
	public T decode(EntityID senderAgentID, MessageChannel channel,
			int timestepReceived, BitStream bitStream,
			ICommunicationBeliefBaseAdapter beliefBase) throws Exception {

		// by this time, the Decoder has already removed the message prefix.
		// all we have to do is to let the subclass decode the contents of the
		// message

		bitStream.markStart();
		bitStream.moveStart(-8);

		BitStreamDecoder decoder = new BitStreamDecoder(bitStream, beliefBase,
				timestepReceived);

		T decodedMessage = decodeMessage(decoder);
		decodedMessage.setSenderAgentID(senderAgentID);
		decodedMessage.setChannel(channel);
		decodedMessage.setTimestepReceived(timestepReceived);

		bitStream.markEnd();

		decodedMessage.setEncoded(bitStream.extractStartToEnd());

		return decodedMessage;
	}

	protected abstract void encodeMessage(T message, BitStreamEncoder encoder);

	protected abstract T decodeMessage(BitStreamDecoder decoder);
}
