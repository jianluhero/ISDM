package iamrescue.communication;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;

public class BitStream {

	private static final int BITS_PER_BYTE = 8;

	private static final int TRAILING_BITS_ENCODING = 3;

	private boolean[] bits;
	private int size = 0;

	private int currentPointer = 0;
	private int savedPointer = -1;

	private int startPointer;

	private int endPointer;

	public BitStream(boolean[] array) {
		this.bits = array;
		size = bits.length;
	}

	public void markStart() {
		startPointer = currentPointer;
	}
	
	public void moveStart(int bits) {
		startPointer += bits;
	}
	
	public void moveEnd(int bits) {
		endPointer += bits;
	}

	public void markEnd() {
		endPointer = currentPointer;
	}

	public BitStream extractStartToEnd() {
		if (endPointer == -1 || startPointer == -1 || startPointer > endPointer) {
			throw new IllegalStateException(
					"Pointers are inconsistent. Start: " + startPointer
							+ ", end:" + endPointer);
		}
		boolean[] newBits = new boolean[endPointer - startPointer];
		System.arraycopy(bits, startPointer, newBits, 0, newBits.length);
		return new BitStream(newBits);
	}

	public BitStream(byte[] array) {
		bits = new boolean[array.length * BITS_PER_BYTE];

		for (int i = 0; i < array.length; i++)
			append(array[i]);

		int trailingBits = array[0] & 7;

		// remove bits that encode number of trailing bits and trailing bits
		// themselves
		bits = ArrayUtils.subarray(bits, 3, bits.length - trailingBits);
		size -= 3 + trailingBits;
	}

	public BitStream() {
		bits = new boolean[32];
		size = 0;
	}

	public BitStream(List<Boolean> bits) {
		this.bits = new boolean[bits.size()];

		for (Boolean b : bits) {
			append(b);
		}
	}

	public void append(boolean bit) {
		if (size == bits.length) {
			// double the bit array
			boolean[] newBits = new boolean[bits.length * 2];
			System.arraycopy(bits, 0, newBits, 0, bits.length);
			bits = newBits;
		}

		bits[size++] = bit;
	}

	public void append(boolean... bits) {
		for (int i = 0; i < bits.length; i++) {
			append(bits[i]);
		}
	}

	public byte[] toByteArray() {
		// three extra bits to encode the number of bits in the final byte
		// (trailing bits)
		int bitCount = size + TRAILING_BITS_ENCODING;

		int byteCount = (bitCount - 1) / BITS_PER_BYTE + 1;
		byte[] result = new byte[byteCount];

		int bitsInLastByte = bitCount % BITS_PER_BYTE;

		int trailingBitCount = (BITS_PER_BYTE - bitsInLastByte) % BITS_PER_BYTE;

		// encode how many bits there are in the final byte
		// encode the number of trailing bits in the first three bits
		result[0] = setBit(result[0], 0, (trailingBitCount & 1) != 0);
		result[0] = setBit(result[0], 1, (trailingBitCount & 2) != 0);
		result[0] = setBit(result[0], 2, (trailingBitCount & 4) != 0);

		// from 3 to end, encode the actual bit array
		for (int i = TRAILING_BITS_ENCODING; i < size + TRAILING_BITS_ENCODING; i++) {
			result[i / BITS_PER_BYTE] = setBit(result[i / BITS_PER_BYTE], i
					% BITS_PER_BYTE, bits[i - TRAILING_BITS_ENCODING]);
		}

		// TODO debugging check: remove for competition
		// Validate.isTrue(new BitStream(result).equals(this));

		return result;
	}

	private byte setBit(byte b, int i, boolean c) {
		if (c) {
			return setBit(b, i);
		}

		return b;
	}

	public int getTrailingZeros() {
		int count = 0;
		int i = size - 1;

		while (i >= 0 && !bits[i--]) {
			count++;
		}

		return count;
	}

	@Override
	public String toString() {
		return Arrays.toString(getBits());
	}

	private byte setBit(byte b, int bit) {
		return (byte) (b | 1 << bit);
	}

	public BitStream concatenate(BitStream encode) {
		boolean[] concatenated = new boolean[size() + encode.size()];
		System.arraycopy(bits, 0, concatenated, 0, size());
		System.arraycopy(encode.bits, 0, concatenated, size(), encode.size());

		return new BitStream(concatenated);
	}

	public void mark() {
		Validate.isTrue(savedPointer == -1);
		savedPointer = currentPointer;
	}

	public void reset() {
		currentPointer = savedPointer;
		savedPointer = -1;
	}

	/**
	 * Reads bs.length bytes to bs
	 * 
	 * @param bs
	 * @return
	 */
	public void read(byte[] bs) {
		for (int i = 0; i < bs.length; i++) {
			bs[i] = readByte();
		}
	}

	public boolean readBit() {
		if (currentPointer > size - 1) {
			throw new IllegalArgumentException("End of stream reached");
		}

		return bits[currentPointer++];
	}

	public byte readByte() {
		byte b = 0;

		for (int i = 0; i < BITS_PER_BYTE; i++) {
			if (readBit())
				b = setBit(b, i);
		}

		return b;
	}

	/**
	 * Number of bits still available
	 * 
	 * @return
	 */
	public int available() {
		return size() - currentPointer;
	}

	public int size() {
		return size;
	}

	public void append(byte[] encodeByte) {
		for (byte b : encodeByte) {
			append(b);
		}
	}

	public void append(byte b) {
		for (int j = 0; j < BITS_PER_BYTE; j++) {
			append((b & (1 << j)) != 0);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BitStream) {
			BitStream other = (BitStream) obj;

			return Arrays.equals(other.getBits(), getBits());
		}

		return false;
	}

	private boolean[] getBits() {
		return ArrayUtils.subarray(bits, 0, size);
	}
}
