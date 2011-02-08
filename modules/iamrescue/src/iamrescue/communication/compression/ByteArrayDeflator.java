package iamrescue.communication.compression;

import iamrescue.communication.BitStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ByteArrayDeflator implements IByteArrayCompressor {

	private static final Log log = LogFactory.getLog(ByteArrayDeflator.class);

	public byte[] compress(BitStream input) throws CompressorException {
		Validate.notNull(input);

		// Create the compressor with highest level of compression
		Deflater compressor = new Deflater();
		compressor.setLevel(Deflater.BEST_COMPRESSION);

		// Give the compressor the data to compress
		byte[] inputArray = input.toByteArray();
		compressor.setInput(inputArray);
		compressor.finish();

		// Create an expandable byte array to hold the compressed data.
		// You cannot use an array that's the same size as the orginal because
		// there is no guarantee that the compressed data will be smaller than
		// the uncompressed data.
		ByteArrayOutputStream bos = new ByteArrayOutputStream(inputArray.length);

		// Compress the data
		byte[] buf = new byte[256];
		while (!compressor.finished()) {
			int count = compressor.deflate(buf);
			bos.write(buf, 0, count);
		}
		try {
			bos.close();
		} catch (IOException e) {
			throw new CompressorException(e);
		}

		// Get the compressed data
		return bos.toByteArray();
	}

	public BitStream decompress(byte[] compressedData)
			throws CompressorException {
		// Create the decompressor and give it the data to compress
		Inflater decompressor = new Inflater();
		decompressor.setInput(compressedData);

		// Create an expandable byte array to hold the decompressed data
		ByteArrayOutputStream bos = new ByteArrayOutputStream(
				compressedData.length);

		// Decompress the data
		byte[] buf = new byte[1024];
		while (!decompressor.finished()) {
			try {
				int count = decompressor.inflate(buf);
				bos.write(buf, 0, count);
			} catch (DataFormatException e) {
				log.error("An exception occurred: " + e.toString());
				throw new CompressorException(e);
			}
		}
		try {
			bos.close();
		} catch (IOException e) {
			log.error("An exception occurred: " + e.toString());
		}

		// Get the decompressed data
		byte[] byteArray = bos.toByteArray();

		return new BitStream(byteArray);
	}

}
