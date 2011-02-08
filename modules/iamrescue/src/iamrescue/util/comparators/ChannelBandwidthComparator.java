package iamrescue.util.comparators;

import iamrescue.communication.messages.MessageChannel;

import java.util.Comparator;

public class ChannelBandwidthComparator implements Comparator<MessageChannel> {

	public static final ChannelBandwidthComparator INSTANCE = new ChannelBandwidthComparator();

	@Override
	public int compare(MessageChannel arg0, MessageChannel arg1) {
		int compared = Double.compare(arg0.getEffectiveBandwidth(), arg1
				.getEffectiveBandwidth());
		if (compared == 0) {
			compared = Double.compare(arg0.getChannelNumber(), arg1
					.getChannelNumber());
		}
		return compared;
	}

}
