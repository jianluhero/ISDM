package iamrescue.communication.scenario;

public class IntraChannelEdge extends CommunicationGraphEdge {

	private int bandwidth;

	public IntraChannelEdge(int bandwidth) {
		this.bandwidth = bandwidth;
	}

	@Override
	public int getMaximumBandwidth() {
		return bandwidth;
	}

}
