package iamrescue.communication.scenario;

public class IntraAgentEdge extends CommunicationGraphEdge {

	@Override
	public int getMaximumBandwidth() {
		return Integer.MAX_VALUE;
	}

}
