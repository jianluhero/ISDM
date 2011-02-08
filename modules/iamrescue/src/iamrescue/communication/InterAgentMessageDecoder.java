package iamrescue.communication;

import iamrescue.communication.compression.CompressorException;
import iamrescue.communication.compression.IByteArrayCompressor;
import iamrescue.communication.compression.NullCompressor;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.messages.codec.ICommunicationBeliefBaseAdapter;
import iamrescue.communication.messages.codec.IMessageCodec;
import iamrescue.communication.messages.codec.UnknownMessageFormatException;
import iamrescue.communication.util.ByteArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;

import rescuecore2.worldmodel.EntityID;

public class InterAgentMessageDecoder implements IDecoder {

	private static final List<Message> EMPTY_LIST = new FastList<Message>();

	private static final Logger LOGGER = Logger
			.getLogger(InterAgentMessageDecoder.class);

	private final Map<Byte, IMessageCodec<?>> decoderTable = new HashMap<Byte, IMessageCodec<?>>();

	private Map<EntityID, Set<ByteArray>> alreadyReceived = new FastMap<EntityID, Set<ByteArray>>();
	private int lastTimeStep = -1;

	private IByteArrayCompressor compressor = new NullCompressor();

	public void setCompressor(IByteArrayCompressor compressor) {
		this.compressor = compressor;
	}

	public IByteArrayCompressor getCompressor() {
		return compressor;
	}

	public List<Message> decode(EntityID senderAgentID, MessageChannel channel,
			int timestep, byte[] rawData,
			ICommunicationBeliefBaseAdapter beliefBase)
			throws UnknownMessageFormatException {

		if (channel.getOverallFailureProbability() > 0) {

			// Possibly expecting redundant messages. Need to filter these.

			if (timestep > lastTimeStep) {
				for (Entry<EntityID, Set<ByteArray>> entry : alreadyReceived
						.entrySet()) {
					entry.getValue().clear();
				}
				lastTimeStep = timestep;
			}

			Set<ByteArray> already = alreadyReceived.get(senderAgentID);
			if (already == null) {
				already = new FastSet<ByteArray>();
				alreadyReceived.put(senderAgentID, already);
			}
			ByteArray array = new ByteArray(rawData);
			if (!already.add(array)) {
				return EMPTY_LIST;
			}
		}

		BitStream uncompressedData;
		try {
			uncompressedData = compressor.decompress(rawData);
		} catch (CompressorException e) {
			throw new UnknownMessageFormatException(e);
		}

		return decode(senderAgentID, channel, timestep, uncompressedData,
				beliefBase);
	}

	public List<Message> decode(EntityID senderAgentID, MessageChannel channel,
			int timestep, BitStream uncompressedData,
			ICommunicationBeliefBaseAdapter beliefBase) {

		List<Message> result = new ArrayList<Message>();
		StopWatch watch = new StopWatch();

		if (uncompressedData.available() == 0) {
			LOGGER.error("No content");
		}

		while (uncompressedData.available() > 0) {

			byte msgTypeId;
			try {
				// because of an unknown problem, messages are received with
				// trailing zeros. This might result in a problem while
				// attempting to read the message id from the stream, as there
				// might be less than 8 trailing zeros left in the stream
				msgTypeId = uncompressedData.readByte();
			} catch (IllegalArgumentException e) {
				LOGGER.debug("Couldn't read message type id");
				LOGGER.debug(uncompressedData);
				// return all successfully decoded messages
				return result;
			}

			IMessageCodec<?> decoder = decoderTable.get(msgTypeId);

			if (decoder == null) {
				LOGGER.error("Message type unknown, "
						+ "no codec registered for messagetype " + msgTypeId);

				List<Byte> list = new ArrayList<Byte>();
				boolean allZero = true;

				while (uncompressedData.available() > 0) {
					boolean bit = uncompressedData.readBit();
					list.add(bit ? (byte) 1 : (byte) 0);
					allZero &= !bit;
				}

				if (allZero) {
					LOGGER.warn("Received trailing zeros");
				} else {
					LOGGER.error("Remaining bits " + list);
					LOGGER.error("Successfully decoded messages " + result);
					LOGGER.error("Data: " + uncompressedData);
				}
			} else
				try {
					watch.start();
					Message msg = decoder.decode(senderAgentID, channel,
							timestep, uncompressedData, beliefBase);
					watch.stop();
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Decoded new message " + msg);
						LOGGER.trace("Decoding of message with type "
								+ msgTypeId + " took " + watch.getTime()
								+ " ms.");
					}
					if (watch.getTime() > 100)
						LOGGER.warn("Decoding of message with type "
								+ msgTypeId + " took " + watch.getTime()
								+ " ms.");

					result.add(msg);
					watch.reset();
				} catch (Exception e) {
					LOGGER.error("Error while decoding message of type "
							+ msgTypeId, e);
					LOGGER.error("Sender: " + senderAgentID);
					LOGGER.error("Channel: " + channel);
					LOGGER.error("Timestep: " + timestep);
					LOGGER.error("Data: " + uncompressedData);
					break;
				}
		}

		return result;

	}

	public void registerCodec(IMessageCodec<?> decoder) {
		IMessageCodec<?> messageCodec = decoderTable.get(decoder
				.getMessagePrefix());
		if (messageCodec != null && messageCodec != decoder)
			throw new IllegalArgumentException("Decoder with prefix "
					+ decoder.getMessagePrefix()
					+ " has already been registered. Are all messageprefixes"
					+ " distinct?");

		decoderTable.put(decoder.getMessagePrefix(), decoder);
		LOGGER.info("Registered MessageCodec " + decoder.getClass());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.IDecoder#canDecode(rescuecore2.worldmodel.EntityID
	 * , iamrescue.communication.messages.MessageChannel, int, byte[],
	 * iamrescue.communication.messages.codec.ICommunicationBeliefBaseAdapter)
	 */
	@Override
	public boolean canDecode(EntityID senderAgentID, MessageChannel channel,
			int timestep, byte[] messageContents,
			ICommunicationBeliefBaseAdapter beliefBase) {
		return beliefBase.isRescueEntity(senderAgentID);
	}
}
