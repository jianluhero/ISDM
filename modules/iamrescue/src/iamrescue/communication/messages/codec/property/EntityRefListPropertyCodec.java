/**
 * 
 */
package iamrescue.communication.messages.codec.property;

import iamrescue.communication.messages.codec.BitStreamDecoder;
import iamrescue.communication.messages.codec.BitStreamEncoder;

import java.util.ArrayList;
import java.util.List;

import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.EntityRefListProperty;

/**
 * @author Sebastian
 * 
 */
public class EntityRefListPropertyCodec extends APropertyCodec {

	private String propertyKey;

	/**
	 * @param propertyKey
	 * 
	 */
	public EntityRefListPropertyCodec(String propertyKey) {
		this.propertyKey = propertyKey;
	}

	@Override
	public Property decode(Entity object, BitStreamDecoder decoder) {
		int[] intArray = decoder.readIntArray();
		List<EntityID> ids = new ArrayList<EntityID>(intArray.length);

		for (int id : intArray) {
			ids.add(new EntityID(id));
		}

		EntityRefListProperty entityRefArray = new EntityRefListProperty(
				propertyKey, ids);

		return entityRefArray;
	}

	@Override
	public void encode(Entity object, Property property,
			BitStreamEncoder encoder) {
		EntityRefListProperty entityRefArray = (EntityRefListProperty) property;

		List<EntityID> value = entityRefArray.getValue();

		int[] values = new int[value.size()];

		int i = 0;
		for (EntityID entityID : value) {
			values[i++] = entityID.getValue();
		}

		encoder.appendIntArray(values);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.codec.property.PropertyCodec#getPropertyKey
	 * ()
	 */
	@Override
	public String getPropertyKey() {
		return propertyKey;
	}

}
