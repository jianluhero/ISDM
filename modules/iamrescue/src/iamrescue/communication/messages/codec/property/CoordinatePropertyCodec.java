package iamrescue.communication.messages.codec.property;

import iamrescue.communication.messages.codec.BitStreamDecoder;
import iamrescue.communication.messages.codec.BitStreamEncoder;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.IntProperty;

public class CoordinatePropertyCodec extends APropertyCodec {

	private String propertyKey;

	private int accuracy = 1;

	private boolean x;

	public CoordinatePropertyCodec(String propertyKey, int accuracy) {
		this.propertyKey = propertyKey;
		this.accuracy = accuracy;
		this.x = propertyKey.toLowerCase().endsWith("x");
	}

	@Override
	public Property decode(Entity object, BitStreamDecoder decoder) {
		int min;
		if (x) {
			min = decoder.getBeliefBase().getMinX();
		} else {
			min = decoder.getBeliefBase().getMinY();
		}
		int number = decoder.readNumber();
		int realNumber = (number - Short.MIN_VALUE) * accuracy + min;
		return new IntProperty(getPropertyKey(), realNumber);
	}

	@Override
	public void encode(Entity object, Property property,
			BitStreamEncoder encoder) {
		int min;
		if (x) {
			min = encoder.getBeliefBase().getMinX();
		} else {
			min = encoder.getBeliefBase().getMinY();
		}
		IntProperty intProperty = (IntProperty) property;
		int value = intProperty.getValue();
		int subtracted = (int) ((value + 0.5 * accuracy - min) / accuracy)
				+ Short.MIN_VALUE;
		encoder.appendNumber(subtracted);
	}

	@Override
	public String getPropertyKey() {
		return propertyKey;
	}
}