package iamrescue.agent.search.agents;

import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.StandardEntityURN;

public class SearchingAmbulanceTeam extends
		AbstractSearchingAgent<AmbulanceTeam> {

	@Override
	protected StandardEntityURN getAgentType() {
		return StandardEntityURN.AMBULANCE_TEAM;
	}
}
