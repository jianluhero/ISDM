package iamrescue.util.comparators;

import iamrescue.communication.messages.MessageChannel;

import java.util.Comparator;

public class ChannelIDComparator implements Comparator<MessageChannel> {

	public static final ChannelIDComparator INSTANCE = new ChannelIDComparator();
	
	@Override
	public int compare(MessageChannel arg0, MessageChannel arg1) {
		return Double.compare(arg0.getChannelNumber(), arg1.getChannelNumber());
	}

}
