package iamrescue.communication.messages.codec.property;

import iamrescue.communication.messages.codec.BitStreamDecoder;
import iamrescue.communication.messages.codec.BitStreamEncoder;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.EntityRefProperty;

public class EntityRefPropertyCodec implements PropertyCodec {

	private String propertyKey;

	public EntityRefPropertyCodec(String propertyKey) {
		this.propertyKey = propertyKey;
	}

	@Override
	public Property decode(Entity object, BitStreamDecoder decoder) {
		return new EntityRefProperty(getPropertyKey(), decoder.readEntityID());
	}

	@Override
	public void encode(Entity object, Property property,
			BitStreamEncoder encoder) {
		EntityRefProperty entityRefProperty = (EntityRefProperty) property;
		encoder.appendEntityID(entityRefProperty.getValue());
	}

	@Override
	public String getPropertyKey() {
		return propertyKey;
	}

}
