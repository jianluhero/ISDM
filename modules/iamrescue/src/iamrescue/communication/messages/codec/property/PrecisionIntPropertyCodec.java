package iamrescue.communication.messages.codec.property;

import iamrescue.communication.messages.codec.BitStreamDecoder;
import iamrescue.communication.messages.codec.BitStreamEncoder;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.IntProperty;

public class PrecisionIntPropertyCodec extends APropertyCodec {

	private int precision;
	private String propertyKey;

	public PrecisionIntPropertyCodec(String propertyKey, int precision) {
		this.propertyKey = propertyKey;
		this.precision = precision;
	}

	@Override
	public Property decode(Entity object, BitStreamDecoder decoder) {
		int number = decoder.readNumber();
		//System.out.print("Received " + number);
		number -= Byte.MIN_VALUE;
		number *= precision;
		// System.out.println(", converted back to " + number + " for "
		// + propertyKey);
		return new IntProperty(getPropertyKey(), number);
	}

	@Override
	public void encode(Entity object, Property property,
			BitStreamEncoder encoder) {
		IntProperty intProperty = (IntProperty) property;
		int value = intProperty.getValue();
		int converted;
		if (precision == 1) {
			converted = value;
		} else {
			converted = (int) ((value / (double) precision) + 0.999999);
		}
		converted += Byte.MIN_VALUE;
		// System.out.println("Converted " + value + " to " + converted
		// + ". Byte: " + (byte) converted + " for " + propertyKey);
		encoder.appendNumber(converted);
	}

	@Override
	public String getPropertyKey() {
		return propertyKey;
	}
}