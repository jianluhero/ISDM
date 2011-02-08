package iamrescue.communication.messages.codec;

import iamrescue.communication.messages.ExampleMessage;
import iamrescue.communication.messages.MessagePrefixes;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Property;

public class ExampleMessageCodec extends AbstractMessageCodec<ExampleMessage> {

	@Override
	protected ExampleMessage decodeMessage(BitStreamDecoder decoder) {
		int dummyInt = decoder.readNumber();
		short dummyShort = (short) decoder.readNumber();
		Property buriedness = decoder.readProperty(null,
				StandardPropertyURN.BURIEDNESS.toString());
		boolean dummyBoolean = decoder.readBoolean();

		ExampleMessage exampleMessage = new ExampleMessage(dummyInt,
				dummyShort, buriedness, dummyBoolean);

		return exampleMessage;
	}

	@Override
	protected void encodeMessage(ExampleMessage message,
			BitStreamEncoder encoder) {
		encoder.appendNumber(message.getDummyInt());
		encoder.appendNumber(message.getDummyShort());
		encoder.appendProperty(null, message.getBuriedness());
		encoder.appendBoolean(message.isDummyBoolean());
	}

	@Override
	public byte getMessagePrefix() {
		return MessagePrefixes.EXAMPLE;
	}
}
