package iamrescue.agent.search.agents;

import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntityURN;

public class SearchingPoliceForce extends AbstractSearchingAgent<PoliceForce> {

	@Override
	protected StandardEntityURN getAgentType() {
		return StandardEntityURN.POLICE_FORCE;
	}
}
