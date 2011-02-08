package iamrescue.agent.search.agents;

import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.StandardEntityURN;

public class SearchingFireBrigade extends AbstractSearchingAgent<FireBrigade> {

	@Override
	protected StandardEntityURN getAgentType() {
		return StandardEntityURN.FIRE_BRIGADE;
	}
}
