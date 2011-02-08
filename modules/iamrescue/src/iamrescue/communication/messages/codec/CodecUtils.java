package iamrescue.communication.messages.codec;

import java.io.UnsupportedEncodingException;

import org.apache.commons.lang.Validate;

public class CodecUtils {

	private final static String CHARACTER_ENCODING = "US-ASCII";

	public static final int INT_SIZE = Integer.SIZE / 8;
	public static final int BYTE_SIZE = Byte.SIZE / 8;
	public static final int SHORT_SIZE = Short.SIZE / 8;

	private CodecUtils() {
	}

	/**
	 * Decode a byte from a buffer
	 * 
	 * @param buffer
	 *            The buffer we are looking at
	 * @param off
	 *            The offset into the buffer to start decoding from
	 * @return The next byte in the buffer
	 */

	public static byte decodeByte(byte[] buffer, int off) {
		return buffer[off];
	}

	/**
	 * Decode a byte array from a buffer
	 * 
	 * @param buffer
	 *            The buffer we are looking at
	 * @param off
	 *            The offset into the buffer to start decoding from
	 * @param length
	 *            The number of bytes to read
	 * @return The next byte array in the buffer
	 */

	public static byte[] decodeBytes(byte[] buffer, int off, int length) {
		byte[] result = new byte[length];
		System.arraycopy(buffer, off, result, 0, length);
		return result;
	}

	/**
	 * Decode a short from a buffer
	 * 
	 * @param buffer
	 *            The buffer we are looking at
	 * @param off
	 *            The offset into the buffer to start decoding from
	 * @return The next short in the buffer
	 */

	public static short decodeShort(byte[] buffer, int off) {
		int result = ((buffer[off] << 8) & 0x0000FF00)
				| (buffer[off + 1] & 0x000000FF);
		return (short) result;
	}

	/**
	 * Decode an int from a buffer
	 * 
	 * @param buffer
	 *            The buffer we are looking at
	 * @param off
	 *            The offset into the buffer to start decoding from
	 * @return The next int in the buffer
	 */
	public static int decodeInt(byte[] buffer, int off) {
		int result = ((buffer[off] << 24) & 0xFF000000)
				| ((buffer[off + 1] << 16) & 0x00FF0000)
				| ((buffer[off + 2] << 8) & 0x0000FF00)
				| (buffer[off + 3] & 0x000000FF);
		return result;
	}

	/**
	 * Decode a String from a buffer
	 * 
	 * @param buffer
	 *            The buffer we are looking at
	 * @param off
	 *            The offset into the buffer to start decoding from
	 * @param length
	 *            The number of characters in the String
	 * @return The next String in the buffer
	 */

	public static String decodeString(byte[] buffer, int off, int length) {
		int realLength = Math.min(length, buffer.length - off);
		byte[] data = new byte[realLength];
		System.arraycopy(buffer, off, data, 0, realLength);
		try {
			return new String(data, CHARACTER_ENCODING);
		} catch (UnsupportedEncodingException e) {
			return new String(data);
		}
	}

	/**
	 * Encode a byte into a byte array
	 * 
	 * @param value
	 *            The byte to encode
	 * @return A byte array representation of the input value
	 */

	public static byte[] encodeByte(byte value) {
		return new byte[] { value };
	}

	/**
	 * Encode a byte into a buffer
	 * 
	 * @param value
	 *            The byte to encode
	 * @param buf
	 *            The buffer to write the result into
	 * @param off
	 *            The offset to start writing at
	 */

	public static void encodeByte(int value, byte[] buf, int off) {
		buf[off] = (byte) (value & 0xFF);
	}

	/**
	 * Encode a byte arrray into a buffer
	 * 
	 * @param bytes
	 *            The byte array to encode
	 * @param buf
	 *            The buffer to write the result into
	 * @param off
	 *            The offset to start writing at
	 */

	public static void encodeBytes(byte[] bytes, byte[] buf, int off) {
		System.arraycopy(bytes, 0, buf, off, bytes.length);
	}

	/**
	 * Encode part of a byte array into a buffer
	 * 
	 * @param bytes
	 *            The byte arrray to encode
	 * @param bytesOffset
	 *            The offset into bytes to start writing from
	 * @param bytesLength
	 *            The number of bytes to write
	 * @param buf
	 *            The buffer to write the result into
	 * @param off
	 *            The offset to start writing at
	 */

	public static void encodeBytes(byte[] bytes, int bytesOffset,
			int bytesLength, byte[] buf, int off) {
		System.arraycopy(bytes, bytesOffset, buf, off, bytesLength);
	}

	/**
	 * Encode a short into a byte array
	 * 
	 * @param value
	 *            The short to encode
	 * @return A byte array representation of the input value
	 */

	public static byte[] encodeShort(int value) {
		Validate.isTrue(value <= Short.MAX_VALUE);

		byte[] result = new byte[2];
		result[0] = (byte) (value >> 8 & 0xFF);
		result[1] = (byte) (value & 0xFF);
		return result;
	}

	/**
	 * Encode a short into a buffer
	 * 
	 * @param value
	 *            The short to encode
	 * @param buf
	 *            The buffer to write the result into
	 * @param off
	 *            The offset to start writing at
	 */

	public static void encodeShort(int value, byte[] buf, int off) {
		buf[off] = (byte) (value >> 8 & 0xFF);
		buf[off + 1] = (byte) (value & 0xFF);
	}

	/**
	 * Encode an int into a byte array
	 * 
	 * @param value
	 *            The int to encode
	 * @return A byte array representation of the input value
	 */

	public static byte[] encodeInt(int value) {
		byte[] result = new byte[4];
		result[0] = (byte) (value >> 24 & 0xFF);
		result[1] = (byte) (value >> 16 & 0xFF);
		result[2] = (byte) (value >> 8 & 0xFF);
		result[3] = (byte) (value & 0xFF);
		return result;
	}

	/**
	 * Encode an int into a buffer
	 * 
	 * @param value
	 *            The int to encode
	 * @param buf
	 *            The buffer to write the result into
	 * @param off
	 *            The offset to start writing at
	 */

	public static void encodeInt(int value, byte[] buf, int off) {
		buf[off] = (byte) (value >> 24 & 0xFF);
		buf[off + 1] = (byte) (value >> 16 & 0xFF);
		buf[off + 2] = (byte) (value >> 8 & 0xFF);
		buf[off + 3] = (byte) (value & 0xFF);
	}

	public static int decodeTimestamp(int b, int timeStep) {
		int timeStamp = b & 0xFF;
		if (timeStamp < 50 && timeStep > 255)
			return 256 + timeStamp;
		else
			return timeStamp;
	}

	public static byte[] encodeTimestamp(short timestep) {
		return encodeByte((byte) timestep);
	}

	public static byte[] encodeIntAsByte(int number) {
		return encodeIntAsByte(number, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	public static byte[] encodeIntAsByte(int number, int minValue, int maxValue) {
		// TODO Auto-generated method stub
		return null;
	}

	private static int byteRange = Byte.MAX_VALUE - Byte.MIN_VALUE;
	private static int shortRange = Short.MAX_VALUE - Short.MIN_VALUE;

	/**
	 * Converts a byte to a percentage (between 0.0 and 1.0), where
	 * Byte.MIN_VALUE is mapped to 0.0 and Byte.MAX_VALUE is mapped to 1.0
	 * 
	 * @param percentage
	 * @return
	 */
	public static double convertByteToPercentage(byte b) {
		return (b - Byte.MIN_VALUE) / (double) byteRange;
	}

	/**
	 * Converts a percentage (between 0.0 and 1.0) to a byte, where 0.0 is
	 * mapped to Byte.MIN_VALUE and 1.0 is mapped to Byte.MAX_VALUE
	 * 
	 * @param percentage
	 * @return
	 */
	public static byte convertPercentageToByte(double percentage) {
		if (percentage > 1 || percentage < 0)
			throw new IllegalArgumentException("Value out of range "
					+ percentage);
		return (byte) (Math.round(percentage * byteRange) + Byte.MIN_VALUE);
	}

	public static boolean equals(Number object1, Number object2, double delta) {
		double d1 = object1.doubleValue();
		double d2 = object2.doubleValue();

		if (d1 == d2)
			return true;

		return (Math.max(d1, d2) / Math.min(d1, d2) - 1) < delta;
	}

	public static boolean almostEquals(Number number, Number number2, double d,
			int i) {
		if (!equals(number, number2, d))
			return (Math.abs(number.doubleValue() - number2.doubleValue()) < i);
		else
			return true;
	}

	/**
	 * Converts a byte to a percentage (between 0.0 and 1.0), where
	 * Byte.MIN_VALUE is mapped to 0.0 and Byte.MAX_VALUE is mapped to 1.0
	 * 
	 * @param percentage
	 * @return
	 */
	public static double convertShortToPercentage(short b) {
		return (b - Short.MIN_VALUE) / (double) shortRange;
	}

	/**
	 * Converts a percentage (between 0.0 and 1.0) to a byte, where 0.0 is
	 * mapped to Byte.MIN_VALUE and 1.0 is mapped to Byte.MAX_VALUE
	 * 
	 * @param percentage
	 * @return
	 */
	public static short convertPercentageToShort(double percentage) {
		if (percentage > 1 || percentage < 0)
			throw new IllegalArgumentException("Value out of range "
					+ percentage);
		return (short) (Math.round(percentage * shortRange) + Short.MIN_VALUE);
	}

	public static double computePercentage(double value, double min, double max) {
		Validate.isTrue(value >= min);
		Validate.isTrue(value <= max);

		return (value - min) / (max - min);
	}

	public static double computeValue(double percentage, double min, double max) {
		return percentage * (max - min) + min;
	}

}
