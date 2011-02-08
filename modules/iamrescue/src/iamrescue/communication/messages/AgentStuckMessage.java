/**
 * 
 */
package iamrescue.communication.messages;

import iamrescue.belief.ShortIDIndex;
import iamrescue.communication.messages.codec.AbstractMessageCodec;
import iamrescue.communication.messages.codec.BitStreamDecoder;
import iamrescue.communication.messages.codec.BitStreamEncoder;
import iamrescue.communication.messages.codec.IMessageCodec;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class AgentStuckMessage extends Message {

	private EntityID area;
	private EntityID stuckAgent;

	/**
	 * 
	 */
	public AgentStuckMessage(EntityID stuckAgent, EntityID area) {
		this.stuckAgent = stuckAgent;
		this.area = area;
	}

	@Override
	public Message copy() {
		AgentStuckMessage stuckMessage = new AgentStuckMessage(stuckAgent, area);
		copyProperties(stuckMessage);
		return stuckMessage;
	}

	public EntityID getArea() {
		return area;
	}

	public EntityID getStuckAgent() {
		return stuckAgent;
	}

	@Override
	public IMessageCodec getCodec() {
		return new AbstractMessageCodec() {

			@Override
			public byte getMessagePrefix() {
				return MessagePrefixes.AGENT_STUCK_MESSAGE_PREFIX;
			}

			@Override
			protected Message decodeMessage(BitStreamDecoder decoder) {
				ShortIDIndex shortIndex = decoder.getBeliefBase()
						.getShortIndex();
				EntityID stuckAgent = shortIndex.getEntityID(decoder
						.readShort());
				EntityID area = shortIndex.getEntityID(decoder.readShort());
				AgentStuckMessage stuckMessage = new AgentStuckMessage(
						stuckAgent, area);
				return stuckMessage;
			}

			@Override
			protected void encodeMessage(Message message,
					BitStreamEncoder encoder) {
				ShortIDIndex shortIndex = encoder.getBeliefBase()
						.getShortIndex();
				encoder.appendShort(shortIndex.getShortID(stuckAgent));
				encoder.appendShort(shortIndex.getShortID(area));
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.Message#getMessageContentsAsString()
	 */
	@Override
	public String getMessageContentsAsString() {
		return "stuckAgent=" + getStuckAgent() + ",area=" + getArea();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.communication.messages.Message#getMessageName()
	 */
	@Override
	public String getMessageName() {
		return "AgentStuckMessage";
	}
}