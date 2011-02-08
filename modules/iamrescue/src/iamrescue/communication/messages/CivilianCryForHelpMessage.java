package iamrescue.communication.messages;

import iamrescue.communication.messages.codec.IMessageCodec;

/**
 * Represents a cry for help from a civilian.
 * 
 * @author rs06r
 * 
 */
public class CivilianCryForHelpMessage extends Message {

	@Override
	public IMessageCodec<CivilianCryForHelpMessage> getCodec() {
		throw new UnsupportedOperationException("Message cannot be encoded");
	}

	@Override
	public Message copy() {
		throw new UnsupportedOperationException("Message cannot be copied");
	}

	/* (non-Javadoc)
	 * @see iamrescue.communication.messages.Message#getMessageContentsAsString()
	 */
	@Override
	public String getMessageContentsAsString() {
		return "";
	}

	/* (non-Javadoc)
	 * @see iamrescue.communication.messages.Message#getMessageName()
	 */
	@Override
	public String getMessageName() {
		return "CivilianCryForHelpMessage";
	}
}
