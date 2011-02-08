package iamrescue.communication.scenario;

public class OutgoingMessageChannelAssignment extends CommunicationGraphEdge {

	@Override
	public int getMaximumBandwidth() {
		return Integer.MAX_VALUE;
	}
}
