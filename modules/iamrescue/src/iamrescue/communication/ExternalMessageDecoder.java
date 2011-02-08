package iamrescue.communication;

import iamrescue.communication.messages.CivilianCryForHelpMessage;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.messages.codec.ICommunicationBeliefBaseAdapter;
import iamrescue.communication.messages.codec.UnknownMessageFormatException;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import rescuecore2.worldmodel.EntityID;

/**
 * This decoder is used to decode messages from external agents. Currently, only
 * civilians send messages to our agents, in which they cry for help.
 * 
 * @author rs06r
 * 
 */
public class ExternalMessageDecoder implements IDecoder {

	private static final byte[] civilianHelpMessage = "Help".getBytes();

	private static final byte[] civilianOuchMessage = "Ouch".getBytes();

	private static final byte[] civilianDropoutMessage = new byte[0];

	private final static List<byte[]> possibleMessages = new ArrayList<byte[]>();

	static {
		possibleMessages.add(civilianHelpMessage);
		possibleMessages.add(civilianOuchMessage);
		possibleMessages.add(civilianDropoutMessage);
	}

	@Override
	public List<Message> decode(EntityID senderAgentID, MessageChannel channel,
			int timestep, byte[] rawData,
			ICommunicationBeliefBaseAdapter beliefBase)
			throws UnknownMessageFormatException {
		if (!beliefBase.isRescueEntity(senderAgentID)) {
			for (byte[] message : possibleMessages) {
				if (ArrayUtils.isEquals(rawData, message)) {
					List<Message> result = new ArrayList<Message>();
					CivilianCryForHelpMessage civilianMessage = new CivilianCryForHelpMessage();
					civilianMessage.setSenderAgentID(senderAgentID);
					civilianMessage.setTimestepReceived(timestep);
					result.add(civilianMessage);
					return result;
				}
			}
		}

		throw new UnknownMessageFormatException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.IDecoder#canDecode(rescuecore2.worldmodel.EntityID
	 * , iamrescue.communication.messages.MessageChannel, int, byte[],
	 * iamrescue.communication.messages.codec.ICommunicationBeliefBaseAdapter)
	 */
	@Override
	public boolean canDecode(EntityID senderAgentID, MessageChannel channel,
			int timestep, byte[] messageContents,
			ICommunicationBeliefBaseAdapter beliefBase) {
		return !beliefBase.isRescueEntity(senderAgentID);
	}
}