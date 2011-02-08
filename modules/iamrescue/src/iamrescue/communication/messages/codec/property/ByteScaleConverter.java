package iamrescue.communication.messages.codec.property;

import iamrescue.communication.messages.codec.BitStreamDecoder;
import iamrescue.communication.messages.codec.BitStreamEncoder;
import iamrescue.communication.messages.codec.CodecUtils;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.DoubleProperty;
import rescuecore2.worldmodel.properties.IntProperty;


public abstract class ByteScaleConverter extends APropertyCodec {

	private Log log = LogFactory.getLog(ByteScaleConverter.class);
	private boolean improveResolutionForSmallerValues;

	public Class<? extends Number> getEncodedValueType() {
		return Byte.class;
	}

	public ByteScaleConverter(boolean improveResolutionForSmallerValues) {
		this.improveResolutionForSmallerValues = improveResolutionForSmallerValues;
	}

	@Override
	public Property decode(Entity object, BitStreamDecoder decoder) {
		int encodedValue = decoder.readInt();

		int minValue = getMinValue(object);
		int range = getMaxValue(object) - getMinValue(object);

		double percentage = CodecUtils
				.convertByteToPercentage((byte) encodedValue);

		if (improveResolutionForSmallerValues)
			percentage *= percentage;

		int value = (int) ((percentage) * range + minValue);

		Validate.isTrue(value >= getMinValue(object), "Decoded Value " + value
				+ " is < min value " + getMinValue(object) + " of property "
				+ getPropertyKey());
		Validate.isTrue(value <= getMaxValue(object), "Decoded Value " + value
				+ " is > max value " + getMaxValue(object) + " of property "
				+ getPropertyKey());

		return PropertyFactory.create(getPropertyKey(), value);
	}

	@Override
	public void encode(Entity object, Property propertyValue,
			BitStreamEncoder encoder) {
		int minValue = getMinValue(object);
		int range = getMaxValue(object) - getMinValue(object);

		if (!(propertyValue instanceof DoubleProperty || propertyValue instanceof IntProperty)) {
			throw new IllegalArgumentException("Can only encode numbers");
		}

		double value = ((Number) propertyValue.getValue()).doubleValue();

		if (value < getMinValue(object)) {
			log.warn("Losing precision: Value " + value + " is < min value "
					+ getMinValue(object) + " of property " + getPropertyKey());
			value = getMinValue(object);
		}

		if (value > getMaxValue(object)) {
			log.warn("Losing precision: Value " + value + " is > max value "
					+ getMaxValue(object) + " of property " + getPropertyKey());
			value = getMaxValue(object);
		}

		double percentage = (value - minValue) / range;

		if (improveResolutionForSmallerValues)
			percentage = Math.sqrt(percentage);

		byte convertPercentageToByte = CodecUtils
				.convertPercentageToByte(percentage);

		encoder.appendByte(convertPercentageToByte);
	}

	public abstract int getMaxValue(Entity object);

	public abstract int getMinValue(Entity object);
}
