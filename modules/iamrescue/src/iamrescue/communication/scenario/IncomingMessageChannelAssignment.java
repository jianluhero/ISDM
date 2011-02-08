package iamrescue.communication.scenario;

public class IncomingMessageChannelAssignment extends CommunicationGraphEdge {

	@Override
	public int getMaximumBandwidth() {
		return Integer.MAX_VALUE;
	}

}
