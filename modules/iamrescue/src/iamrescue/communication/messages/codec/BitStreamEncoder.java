package iamrescue.communication.messages.codec;

import iamrescue.communication.BitStream;
import iamrescue.communication.messages.codec.property.PropertyCodec;
import iamrescue.communication.messages.codec.property.PropertyEncoderStore;
import iamrescue.util.ArrayUtils;

import java.math.BigInteger;

import org.apache.commons.lang.Validate;

import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

public class BitStreamEncoder {

	protected static final boolean[] BYTE_PREFIX = { true };
	protected static final boolean[] SHORT_PREFIX = { false, true };
	protected static final boolean[] INT_PREFIX = { false, false };
	private BitStream outputStream;
	private ICommunicationBeliefBaseAdapter beliefBase;
	private PropertyEncoderStore encoders;

	public BitStreamEncoder(ICommunicationBeliefBaseAdapter beliefBase) {
		outputStream = new BitStream();
		this.beliefBase = beliefBase;
		this.encoders = beliefBase.getEncoders();
	}

	public ICommunicationBeliefBaseAdapter getBeliefBase() {
		return beliefBase;
	}

	public void appendByte(byte b) {
		outputStream.append(CodecUtils.encodeByte(b));
	}

	public void appendInt(int integer) {
		outputStream.append(CodecUtils.encodeInt(integer));
	}

	public void appendNumber(int number) {
		if (number == (byte) number) {
			outputStream.append(BYTE_PREFIX);
			appendByte((byte) number);
		} else if (number == (short) number) {
			outputStream.append(SHORT_PREFIX);
			appendShort((short) number);
		} else {
			outputStream.append(INT_PREFIX);
			appendInt(number);
		}
	}

	// public void appendNumberSpecial(int number) {
	// boolean sign = number < 0;
	// number = number > 0 ? number : -number;
	//
	// int bitsNeeded = bitsNeeded(number);
	// System.out.println(bitsNeeded);
	//
	// // Math.ceil(bitsneeded / 4)
	// int blocksNeeded = ((bitsNeeded + 3) / 4);
	// System.out.println(blocksNeeded);
	//
	// int actualBits = blocksNeeded * 4;
	//
	// int padding = actualBits - bitsNeeded;
	// System.out.println("padding " + padding);
	//
	// appendBoolean(((blocksNeeded) & 1) != 0);
	// appendBoolean(((blocksNeeded) & 2) != 0);
	// appendBoolean(((blocksNeeded) & 4) != 0);
	// // encode the sign of the number
	// appendBoolean(sign);
	//
	// number = number > 0 ? number : -number;
	//
	// while (number != 0) {
	// appendBoolean((number & 1) == 1);
	// number >>= 1;
	// }
	// }

	public void appendShort(int integer) {
		outputStream.append(CodecUtils.encodeShort(integer));
	}

	public byte[] toByteArray() {
		return outputStream.toByteArray();
	}

	/**
	 * Append an integer array to the byte stream. Note that the size of the
	 * array should be smaller than 127
	 * 
	 * @param array
	 */
	public void appendIntArray(int[] array) {
		Validate.isTrue(array.length < Byte.MAX_VALUE,
				"Does not support arrays larger than " + Byte.MAX_VALUE);
		appendByte((byte) array.length);

		for (int i = 0; i < array.length; i++) {
			appendNumber(array[i]);
		}
	}

	// public void appendNumber(Class<? extends Number> valueType,
	// Number propertyValue) {
	// int encodedValue;
	//
	// if (valueType.equals(Integer.class)) {
	// encodedValue = propertyValue.intValue();
	// appendInt(encodedValue);
	// } else if (valueType.equals(Short.class)) {
	// encodedValue = propertyValue.shortValue();
	// appendShort(propertyValue.shortValue());
	// } else if (valueType.equals(Byte.class)) {
	// encodedValue = propertyValue.byteValue();
	// appendByte(propertyValue.byteValue());
	// } else {
	// throw new IllegalArgumentException(valueType + " not supported");
	// }
	//
	// if (propertyValue.intValue() != encodedValue)
	// throw new IllegalArgumentException("Losing precision. Original: "
	// + propertyValue.intValue() + ". Encoded as " + valueType
	// + ": " + encodedValue);
	// 

	public void appendByteArray(byte[] array) {
		Validate.isTrue(array.length < Byte.MAX_VALUE,
				"Does not support arrays larger than " + Byte.MAX_VALUE);
		appendByte((byte) array.length);

		for (int i = 0; i < array.length; i++) {
			appendByte(array[i]);
		}
	}

	/**
	 * This is more efficient than appendByteArray
	 * 
	 * @param b
	 */
	public void appendByteArraySpecial(byte[] b) {
		Validate.isTrue(b.length < Byte.MAX_VALUE,
				"Does not support arrays larger than " + Byte.MAX_VALUE);

		if (b.length == 0) {
			appendByte(Byte.MIN_VALUE);
			return;
		}

		byte[] array = new byte[b.length];

		// get rid of zeros
		for (int i = 0; i < array.length; i++)
			array[i] = (byte) (b[i] + 1);

		byte max = (byte) (ArrayUtils.max(array) + 1);
		appendByte(max);

		BigInteger encoded = BigInteger.valueOf(array[array.length - 1]);

		BigInteger maxBI = BigInteger.valueOf(max);

		for (int i = array.length - 2; i >= 0; i--) {
			encoded = encoded.multiply(maxBI);
			encoded = encoded.add(BigInteger.valueOf(array[i]));
		}

		appendByteArray(encoded.toByteArray());
	}

	public void appendBoolean(boolean b) {
		outputStream.append(b);
	}

	public void appendProperty(Entity object, Property property) {
		PropertyCodec propertyEncoder = encoders.get(property.getURN());
		propertyEncoder.encode(object, property, this);
	}

	public void appendEntityID(Entity object) {
		if (beliefBase.isShortIDAvailable(object.getClass())) {
			appendBoolean(true);
			appendShort(beliefBase.getShortID(object));
		} else {
			appendBoolean(false);
			appendNumber(object.getID().getValue() + Short.MIN_VALUE);
		}
	}

	public void appendEntityID(EntityID id) {
		if (beliefBase.getShortIndex().knowsAboutLongID(id)) {
			appendBoolean(true);
			appendShort(beliefBase.getShortIndex().getShortID(id));
		} else {
			appendBoolean(false);
			appendNumber(id.getValue() + Short.MIN_VALUE);
		}
	}

	public BitStream getBitStream() {
		return outputStream;
	}

	public static int bitsNeeded(int number) {
		if (number == Integer.MIN_VALUE) {
			throw new IllegalArgumentException("Can't encode Integer.MIN_VALUE");
		}

		int count = 0;

		while (number != 0) {
			number >>= 1;
			count++;
		}

		return count;
	}
	//
	// public static void main(String[] args) {
	//
	// BitStreamEncoder bitStreamEncoder = new BitStreamEncoder(null);
	//
	// bitStreamEncoder.appendNumberSpecial(121);
	//
	// System.out.println(bitStreamEncoder.getBitStream().size());
	// System.out.println(bitStreamEncoder.getBitStream());
	//
	// bitStreamEncoder = new BitStreamEncoder(null);
	// bitStreamEncoder.appendNumberSpecial(-121);
	//
	// System.out.println(bitStreamEncoder.getBitStream().size());
	// System.out.println(bitStreamEncoder.getBitStream());
	//
	// // int val = Integer.MIN_VALUE + 1;
	// //
	// // System.out.println(bitsNeeded(val));
	// //
	// // int number = -1;
	// //
	// // System.out.println(Integer.toBinaryString(number));
	// // System.out.println(Integer.toBinaryString(Integer.MAX_VALUE));
	// // System.out.println(Integer.toBinaryString(Integer.MIN_VALUE));
	// //
	// // System.out.println(Integer.bitCount(Integer.MAX_VALUE));
	// // System.out.println(Integer.bitCount(Integer.MIN_VALUE));
	// }
}
