package iamrescue.communication.messages.codec.property;

import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;

public class ScaleConverter extends ByteScaleConverter {

	private String propertyKey;

	@Override
	public synchronized int getMaxValue(Entity object) {
		return PropertyValues.getMaxValue(propertyKey);
	}

	@Override
	public synchronized int getMinValue(Entity object) {
		return PropertyValues.getMinValue(propertyKey);
	}

	public ScaleConverter(String propertyKey,
			boolean improveResolutionForSmallerValues) {
		super(improveResolutionForSmallerValues);

		this.propertyKey = propertyKey;
	}

	@Override
	public String getPropertyKey() {
		return propertyKey;
	}
}
