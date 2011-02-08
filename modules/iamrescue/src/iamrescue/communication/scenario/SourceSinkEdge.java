package iamrescue.communication.scenario;

public class SourceSinkEdge extends CommunicationGraphEdge {

	@Override
	public int getMaximumBandwidth() {
		return Integer.MAX_VALUE;
	}

}
