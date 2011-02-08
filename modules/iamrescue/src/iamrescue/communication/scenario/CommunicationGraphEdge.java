package iamrescue.communication.scenario;

public abstract class CommunicationGraphEdge {
	private int flow;

	public abstract int getMaximumBandwidth();

	public final void setAssignedFlow(int flow) {
		this.flow = flow;
	}

	public int getFlow() {
		return flow;
	}
}
