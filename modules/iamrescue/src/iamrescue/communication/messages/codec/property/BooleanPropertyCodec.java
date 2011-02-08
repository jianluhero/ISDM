package iamrescue.communication.messages.codec.property;

import iamrescue.communication.messages.codec.BitStreamDecoder;
import iamrescue.communication.messages.codec.BitStreamEncoder;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.BooleanProperty;


public class BooleanPropertyCodec extends APropertyCodec {

	private String propertyKey;

	public BooleanPropertyCodec(String propertyKey) {
		this.propertyKey = propertyKey;
	}

	@Override
	public Property decode(Entity object, BitStreamDecoder decoder) {
		return new BooleanProperty(getPropertyKey(), decoder.readBoolean());
	}

	@Override
	public void encode(Entity object, Property property,
			BitStreamEncoder encoder) {
		BooleanProperty intProperty = (BooleanProperty) property;

		encoder.appendBoolean(intProperty.getValue());
	}

	@Override
	public String getPropertyKey() {
		return propertyKey;
	}
}
