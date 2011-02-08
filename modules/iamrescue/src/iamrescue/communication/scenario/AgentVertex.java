package iamrescue.communication.scenario;

import rescuecore2.standard.entities.StandardEntityURN;

public abstract class AgentVertex implements CommunicationGraphVertex {
	private StandardEntityURN type;
	private int number;

	public AgentVertex(StandardEntityURN type, int number) {
		this.type = type;
		this.number = number;
	}

	public int getNumber() {
		return number;
	}

	public StandardEntityURN getType() {
		return type;
	}

}
