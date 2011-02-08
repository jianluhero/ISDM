package iamrescue.communication.messages.codec.property;

import iamrescue.communication.messages.codec.BitStreamDecoder;
import iamrescue.communication.messages.codec.BitStreamEncoder;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.IntArrayProperty;

public class IntArrayPropertyCodec extends APropertyCodec {

	private String urn;

	public IntArrayPropertyCodec(String urn) {
		this.urn = urn;
	}

	@Override
	public Property decode(Entity object, BitStreamDecoder decoder) {
		return new IntArrayProperty(urn, decoder.readIntArray());
	}

	@Override
	public void encode(Entity object, Property property,
			BitStreamEncoder encoder) {
		IntArrayProperty intArray = (IntArrayProperty) property;
		encoder.appendIntArray(intArray.getValue());
	}

	@Override
	public String getPropertyKey() {
		return urn;
	}

}
