package iamrescue.communication.messages.codec.property;

import iamrescue.communication.messages.codec.BitStreamDecoder;
import iamrescue.communication.messages.codec.BitStreamEncoder;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.Property;


public interface PropertyCodec {

	void encode(Entity object, Property property, BitStreamEncoder encoder);

	Property decode(Entity object, BitStreamDecoder decoder);

	String getPropertyKey();

}
