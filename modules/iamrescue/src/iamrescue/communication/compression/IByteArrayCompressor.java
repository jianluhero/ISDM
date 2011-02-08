package iamrescue.communication.compression;

import iamrescue.communication.BitStream;

public interface IByteArrayCompressor {

	byte[] compress(BitStream bits) throws CompressorException;

	BitStream decompress(byte[] array) throws CompressorException;

}
