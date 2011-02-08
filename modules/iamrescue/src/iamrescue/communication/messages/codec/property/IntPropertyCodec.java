package iamrescue.communication.messages.codec.property;

import iamrescue.communication.messages.codec.BitStreamDecoder;
import iamrescue.communication.messages.codec.BitStreamEncoder;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.IntProperty;

public class IntPropertyCodec extends APropertyCodec {

	private String propertyKey;

	public IntPropertyCodec(String propertyKey) {
		this.propertyKey = propertyKey;
	}

	@Override
	public Property decode(Entity object, BitStreamDecoder decoder) {
		return new IntProperty(getPropertyKey(), decoder.readNumber());
	}

	@Override
	public void encode(Entity object, Property property,
			BitStreamEncoder encoder) {
		IntProperty intProperty = (IntProperty) property;
		encoder.appendNumber(intProperty.getValue());
	}

	@Override
	public String getPropertyKey() {
		return propertyKey;
	}
}
