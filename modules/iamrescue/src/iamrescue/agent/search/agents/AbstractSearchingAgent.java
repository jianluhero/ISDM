package iamrescue.agent.search.agents;

import iamrescue.agent.AbstractIAMAgent;

import java.util.Collections;
import java.util.List;

import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;

/**
 * This agent only searches.
 * 
 * @author Sebastian
 * 
 * @param <E>
 */
public abstract class AbstractSearchingAgent<E extends Human> extends
		AbstractIAMAgent<E> {

	protected void think(int time, ChangeSet changed) {
		doDefaultSearch();
	}

	protected void postConnect() {
		super.postConnect();
//		showSearchViewer();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.agent.AbstractIAMAgent#fallback(int,
	 * rescuecore2.worldmodel.ChangeSet)
	 */
	@Override
	protected void fallback(int time, ChangeSet changed) {
		// TODO Auto-generated method stub

	}

	@Override
	protected List<StandardEntityURN> getAgentTypes() {
		return Collections.singletonList(getAgentType());
	}

	protected abstract StandardEntityURN getAgentType();

}
