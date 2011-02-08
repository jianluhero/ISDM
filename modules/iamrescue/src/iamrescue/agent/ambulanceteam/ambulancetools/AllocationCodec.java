package iamrescue.agent.ambulanceteam.ambulancetools;

import iamrescue.communication.messages.MessagePrefixes;
import iamrescue.communication.messages.codec.AbstractMessageCodec;
import iamrescue.communication.messages.codec.BitStreamDecoder;
import iamrescue.communication.messages.codec.BitStreamEncoder;

public class AllocationCodec extends AbstractMessageCodec<AllocationMessage> {

	@Override
	public byte getMessagePrefix() {
		return MessagePrefixes.AMBULANCE_ALLOCATION_PREFIX;
	}

	@Override
	protected AllocationMessage decodeMessage(BitStreamDecoder decoder) {
		int[] task = decoder.readIntArray();
		int time = decoder.readNumber() - Byte.MIN_VALUE;
		AllocationMessage allocation = new AllocationMessage(task, time);

		return allocation;
	}

	@Override
	protected void encodeMessage(AllocationMessage message,
			BitStreamEncoder encoder) {
		encoder.appendIntArray(message.getTask());
		encoder.appendNumber(message.getTime() + Byte.MIN_VALUE);

	}
}
