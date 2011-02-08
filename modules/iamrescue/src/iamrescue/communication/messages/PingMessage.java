package iamrescue.communication.messages;

import iamrescue.communication.messages.codec.AbstractMessageCodec;
import iamrescue.communication.messages.codec.BitStreamDecoder;
import iamrescue.communication.messages.codec.BitStreamEncoder;
import iamrescue.communication.messages.codec.IMessageCodec;

public class PingMessage extends Message {

	public PingMessage() {
	}

	@Override
	public Message copy() {
		return new PingMessage();
	}

	@Override
	public IMessageCodec getCodec() {
		return new AbstractMessageCodec<PingMessage>() {

			@Override
			protected PingMessage decodeMessage(BitStreamDecoder decoder) {
				return new PingMessage();
			}

			@Override
			protected void encodeMessage(PingMessage message,
					BitStreamEncoder encoder) {
				return;

			}

			@Override
			public byte getMessagePrefix() {
				return MessagePrefixes.PING_PREFIX;
			}
		};
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PingMessage) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 13;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.Message#getMessageContentsAsString()
	 */
	@Override
	public String getMessageContentsAsString() {
		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.communication.messages.Message#getMessageName()
	 */
	@Override
	public String getMessageName() {
		return "PingMessage";
	}
}