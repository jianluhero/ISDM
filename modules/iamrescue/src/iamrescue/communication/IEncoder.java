package iamrescue.communication;

import iamrescue.communication.messages.Message;

import java.util.Collection;


public interface IEncoder {

	/**
	 * Encodes messages into a byte array. The messages that have been encoded
	 * will marked as sent. Messages should be processed in the order that they
	 * occur in the list
	 * 
	 * @param messages
	 *            the messages that are to be encoded. Messages that have been
	 *            encoded will be marked as sent. Messages are encoded in the
	 *            order that they appear in the collection
	 * @param maxLength
	 *            the maximum length of the encoded message
	 * @return a byte array with the messages that have been encoded, which does
	 *         not necessarily contain all the encoded messages in the list
	 */
	byte[] encodeMessages(Collection<Message> messages, int maxLength);

}
