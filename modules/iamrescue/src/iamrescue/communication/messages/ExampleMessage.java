package iamrescue.communication.messages;

import iamrescue.communication.messages.codec.ExampleMessageCodec;
import iamrescue.communication.messages.codec.IMessageCodec;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.IntProperty;

public class ExampleMessage extends Message {

	private int dummyInt;
	private short dummyShort;
	private Property buriedness = new IntProperty(
			StandardPropertyURN.BURIEDNESS);
	private boolean dummyBoolean;

	public ExampleMessage(int dummyInt, short dummyShort, Property buriedness,
			boolean dummyBoolean) {
		this.dummyInt = dummyInt;
		this.dummyShort = dummyShort;
		this.buriedness = buriedness;
		this.dummyBoolean = dummyBoolean;
	}

	public Property getBuriedness() {
		return buriedness;
	}

	public boolean isDummyBoolean() {
		return dummyBoolean;
	}

	public int getDummyInt() {
		return dummyInt;
	}

	public short getDummyShort() {
		return dummyShort;
	}

	public void setBuriedness(Property buriedness) {
		this.buriedness = buriedness;
	}

	public void setDummyInt(int dummyInt) {
		this.dummyInt = dummyInt;
	}

	public void setDummyShort(short dummyShort) {
		this.dummyShort = dummyShort;
	}

	@Override
	public Message copy() {
		return new ExampleMessage(dummyInt, dummyShort, buriedness,
				dummyBoolean);
	}

	@Override
	public IMessageCodec getCodec() {
		return new ExampleMessageCodec();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ExampleMessage) {
			ExampleMessage message = (ExampleMessage) obj;

			if (message.getDummyInt() != dummyInt)
				return false;

			if (message.getDummyShort() != dummyShort)
				return false;

			if (!message.getBuriedness().getValue().equals(
					buriedness.getValue()))
				return false;

			if (message.isDummyBoolean() != dummyBoolean)
				return false;

			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		// assert false : "hashcode not implemented";
		return 42;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.Message#getMessageContentsAsString()
	 */
	@Override
	public String getMessageContentsAsString() {
		return "not implemented.";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.communication.messages.Message#getMessageName()
	 */
	@Override
	public String getMessageName() {
		return "ExampleMessage";
	}
}
