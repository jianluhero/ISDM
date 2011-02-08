/**
 * 
 */
package iamrescue.belief.provenance;

import iamrescue.communication.messages.MessageChannel;

/**
 * @author Sebastian
 * 
 */
public class AgentCommunicationOrigin implements IOrigin {

	private int channelNumber;

	/**
	 * @param channelNumber
	 */
	public AgentCommunicationOrigin(int channelNumber) {
		this.channelNumber = channelNumber;
	}

	/**
	 * @return the channelNumber
	 */
	public int getChannelNumber() {
		return channelNumber;
	}

	/**
	 * @param channel
	 * @return
	 */
	public static AgentCommunicationOrigin get(MessageChannel channel) {
		return new AgentCommunicationOrigin(channel.getChannelNumber());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + channelNumber;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AgentCommunicationOrigin other = (AgentCommunicationOrigin) obj;
		if (channelNumber != other.channelNumber)
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "AgentCommunicationOrigin " + channelNumber;
	}

}
