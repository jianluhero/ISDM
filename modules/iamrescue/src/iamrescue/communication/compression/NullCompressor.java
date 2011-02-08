package iamrescue.communication.compression;

import iamrescue.communication.BitStream;

public class NullCompressor implements IByteArrayCompressor {

	/*
	 * public static final byte[] prefix = new byte[] { 110, 106 }; private
	 * boolean expectPrefix;
	 */

	public NullCompressor() {
		// this(true);
	}

	/*
	 * public NullCompressor(boolean expectPrefix) { this.expectPrefix =
	 * expectPrefix; }
	 */
	public byte[] compress(BitStream input) {
		// if (!expectPrefix) {
		// return input.toByteArray();
		// } else {
		/*
		 * byte[] array = input.toByteArray(); return
		 * ArrayUtils.concatenate(prefix, array);
		 */
		// }
		return input.toByteArray();
	}

	public BitStream decompress(byte[] array) throws CompressorException {
		return new BitStream(array);
		// /if (!expectPrefix) {
		// return new BitStream(array);
		// } else {
		/*
		 * byte[] actualPrefix = org.apache.commons.lang.ArrayUtils.subarray(
		 * array, 0, prefix.length); if (!Arrays.equals(actualPrefix, prefix)) {
		 * throw new CompressorException(
		 * "Cannot decompress messages that have prefix " +
		 * Arrays.toString(actualPrefix) + ". Was expecting " +
		 * Arrays.toString(prefix)); } else { byte[] decodedArray =
		 * org.apache.commons.lang.ArrayUtils .subarray(array, prefix.length,
		 * array.length);
		 * 
		 * return new BitStream(decodedArray); }
		 */// }
	}
}
