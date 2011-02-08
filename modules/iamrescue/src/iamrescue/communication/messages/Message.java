package iamrescue.communication.messages;

import iamrescue.communication.BitStream;
import iamrescue.communication.messages.codec.ICommunicationBeliefBaseAdapter;
import iamrescue.communication.messages.codec.IMessageCodec;
import rescuecore2.worldmodel.EntityID;

/**
 * Abstract class that represents messages that are sent over the communication
 * network.
 * 
 * @author Ruben Stranders
 * 
 */
public abstract class Message {
	private int timestepReceived;

	private MessagePriority priority = MessagePriority.NORMAL;

	private EntityID senderAgentID;

	private MessageChannel channel;

	private boolean read = false;

	// private boolean sent;

	// default time to live is 1
	private int ttl = 1;

	private int repeats = 1;

	private BitStream encoded = null;

	public final MessagePriority getPriority() {
		return priority;
	}

	public void setEncoded(BitStream encoded) {
		this.encoded = encoded;
	}

	public final void setPriority(MessagePriority priority) {
		this.priority = priority;
	}

	public final void setSenderAgentID(EntityID senderAgentID) {
		this.senderAgentID = senderAgentID;
	}

	public final EntityID getSenderAgentID() {
		return senderAgentID;
	}

	public final void setTimestepReceived(int timestepReceived) {
		this.timestepReceived = timestepReceived;
	}

	public final int getTimestepReceived() {
		return timestepReceived;
	}

	public final MessageChannel getChannel() {
		return channel;
	}

	public final void setChannel(MessageChannel channel) {
		this.channel = channel;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(getMessageName());
		sb.append('[');
		sb.append(getMessageContentsAsString());
		sb.append("] - ");
		sb.append(getMessageProperties());
		return sb.toString();
	}

	public abstract String getMessageName();

	public abstract String getMessageContentsAsString();

	public StringBuffer getMessageProperties() {
		StringBuffer sb = new StringBuffer();
		sb.append("timestepreceived=");
		sb.append(getTimestepReceived());
		sb.append(",priority=");
		sb.append(getPriority());
		sb.append(",senderAgentID=");
		sb.append(getSenderAgentID());
		sb.append(",channel=");
		sb.append(getChannel());
		sb.append(",ttl=");
		sb.append(getTTL());
		sb.append(",repeats=");
		sb.append(getRepeats());
		sb.append(",encoded=");
		if (this.encoded == null) {
			sb.append("notyet");
		} else {
			sb.append(encoded.size());
			sb.append(" bits = ");
			sb.append(encoded.size() / 8.0);
			sb.append(" bytes");
		}
		return sb;
	}

	public String toShortString() {
		return getMessageName() + "[s:" + senderAgentID + "]";
	}

	public BitStream encode(ICommunicationBeliefBaseAdapter beliefBase) {
		if (encoded == null) {
			encoded = getCodec().encode(this, beliefBase);
		}
		return encoded;
	}

	protected void invalidateEncoded() {
		encoded = null;
	}

	public abstract IMessageCodec getCodec();

	public final boolean isRead() {
		return read;
	}

	public final void markAsRead() {
		read = true;
	}

	public final void markAsSent() {
		// one instance has been sent
		repeats--;
	}

	public final boolean isSent() {
		// all messages are sent
		return repeats <= 0;
	}

	public final void setTTL(int ttl) {
		this.ttl = ttl;
	}

	public final int getTTL() {
		return ttl;
	}

	protected void copyProperties(Message message) {
		message.setPriority(priority);
		message.setTTL(ttl);
		message.setEncoded(encoded);
	}

	public final void setRepeats(int repeats) {
		this.repeats = repeats;
	}

	public final int getRepeats() {
		return repeats;
	}

	public abstract Message copy();

}
