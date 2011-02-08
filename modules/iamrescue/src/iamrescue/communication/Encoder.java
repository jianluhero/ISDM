package iamrescue.communication;

import iamrescue.communication.compression.CompressorException;
import iamrescue.communication.compression.IByteArrayCompressor;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.codec.ICommunicationBeliefBaseAdapter;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Encoder implements IEncoder {

	private static Log log = LogFactory.getLog(Encoder.class);

	private IByteArrayCompressor compressor;

	private ICommunicationBeliefBaseAdapter beliefBase;

	// private static final boolean CONCATENATE_MESSAGES = false;

	public Encoder(IByteArrayCompressor compressor,
			ICommunicationBeliefBaseAdapter beliefBase) {
		this.compressor = compressor;
		this.beliefBase = beliefBase;
	}

	public IByteArrayCompressor getCompressor() {
		return compressor;
	}

	public byte[] encodeMessages(Collection<Message> messages, int maxLength) {
		BitStream encodedString = new BitStream();

		int i = 0;

		try {
			for (Message message : messages) {
				BitStream afterConcatenation = encodedString
						.concatenate(message.encode(beliefBase));

				// check if the compressed size of these messages exceeds the
				// maximum allowable size.
				if (compressor.compress(afterConcatenation).length > maxLength)
					break;
				else {
					encodedString = afterConcatenation;
					message.markAsSent();
				}

				i++;
			}

			if (encodedString.available() == 0) {
				return new byte[0];
			}

			byte[] compress = compressor.compress(encodedString);

			// int compressionPercentage = (int) ((1 - compress.length
			// / (double) encodedString.length) * 100);

			return compress;
		} catch (CompressorException e) {
			log.error("Error encoding messages due to exception in compressor",
					e);
			return new byte[0];
		}
	}
}
