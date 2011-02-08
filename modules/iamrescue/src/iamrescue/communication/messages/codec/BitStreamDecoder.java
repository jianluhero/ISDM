package iamrescue.communication.messages.codec;

import iamrescue.communication.BitStream;
import iamrescue.communication.messages.codec.property.PropertyCodec;
import iamrescue.communication.messages.codec.property.PropertyEncoderStore;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

public class BitStreamDecoder {
	private BitStream bitStream;
	private ICommunicationBeliefBaseAdapter beliefBase;
	private int currentTimestep;
	private PropertyEncoderStore encoders;

	public BitStreamDecoder(BitStream bitStream,
			ICommunicationBeliefBaseAdapter beliefBase, int currentTimeStep) {
		this.bitStream = bitStream;
		this.beliefBase = beliefBase;
		this.currentTimestep = currentTimeStep;
		this.encoders = beliefBase.getEncoders();
	}

	public ICommunicationBeliefBaseAdapter getBeliefBase() {
		return beliefBase;
	}

	public int readInt() {
		byte[] bs = readBytes(CodecUtils.INT_SIZE);
		return CodecUtils.decodeInt(bs, 0);

	}

	public short readShort() {
		byte[] bs = readBytes(CodecUtils.SHORT_SIZE);
		return CodecUtils.decodeShort(bs, 0);
	}

	public byte readByte() {
		byte[] bs = readBytes(CodecUtils.BYTE_SIZE);
		return CodecUtils.decodeByte(bs, 0);
	}

	public int readNumber() {
		// read the prefix that determines the type of the number
		boolean firstBit = bitStream.readBit();
		if (firstBit == BitStreamEncoder.BYTE_PREFIX[0]) {
			return readByte();
		} else {
			boolean secondBit = bitStream.readBit();
			if (secondBit == BitStreamEncoder.SHORT_PREFIX[1]) {
				return readShort();
			} else {
				return readInt();
			}
		}
	}

	public int[] readIntArray() {
		byte length = readByte();
		int[] array = new int[length];

		for (int i = 0; i < array.length; i++) {
			array[i] = readNumber();
		}

		return array;
	}

	public short peekShort() {
		bitStream.mark();
		short s = readShort();
		bitStream.reset();
		return s;
	}

	public int peekInt() {
		bitStream.mark();
		int s = readInt();
		bitStream.reset();
		return s;
	}

	public int peekNumber() {
		bitStream.mark();
		int number = readNumber();
		bitStream.reset();
		return number;
	}

	private byte[] readBytes(int size) {
		byte[] bs = new byte[size];
		bitStream.read(bs);
		return bs;
	}

	public byte[] readByteArray() {
		byte length = readByte();
		byte[] array = new byte[length];

		for (int i = 0; i < array.length; i++) {
			array[i] = readByte();
		}

		return array;
	}

	public byte[] readByteArraySpecial() {
		byte maxB = readByte();

		if (maxB == Byte.MIN_VALUE)
			return new byte[0];

		BigInteger max = BigInteger.valueOf(maxB);

		BigInteger num = new BigInteger(readByteArray());
		List<Byte> decoded = new ArrayList<Byte>();

		while (num.compareTo(BigInteger.ZERO) > 0) {
			// (num - 1) % max
			decoded.add(num.mod(max).subtract(BigInteger.ONE).byteValue());
			// num /= max
			num = num.divide(max);
		}

		return ArrayUtils.toPrimitive(decoded.toArray(new Byte[0]));
	}

	public boolean readBoolean() {
		return bitStream.readBit();
	}

	public Property readProperty(Entity object, String propertyKey) {
		PropertyCodec propertyEncoder = encoders.get(propertyKey);
		return propertyEncoder.decode(object, this);
	}

	/**
	 * This method returns an entity with the specified object class. It will
	 * attempt to read an entity id from the stream and find the proper object
	 * in the belief base. If the object does not exist in the beliefbase, the
	 * method returns null, and the bytestream pointer will not have been
	 * changed
	 * 
	 * @param objectClass
	 * @return
	 */
	public Entity readEntityByID() {
		Entity object;

		boolean isShort = readBoolean();

		if (isShort) {
			short shortID = readShort();
			object = beliefBase.getObjectByShortID(shortID);
			if (object == null)
				throw new IllegalArgumentException(
						"Could not find object with shortID: " + shortID
								+ " in the beliefbase");
		} else {
			int id = peekNumber() - Short.MIN_VALUE;
			object = beliefBase.getObjectByID(id);
			if (object != null) {
				int readID = readNumber() - Short.MIN_VALUE;
				assert id == readID;
			}
		}

		return object;
	}

	public EntityID readEntityID() {
		boolean isShort = readBoolean();
		if (isShort) {
			return beliefBase.getShortIndex().getEntityID(readShort());
		} else {
			return new EntityID(readNumber() - Short.MIN_VALUE);
		}
	}
}
