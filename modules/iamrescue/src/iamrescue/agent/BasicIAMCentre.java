package iamrescue.agent;

import iamrescue.agent.firebrigade.FastFirePredictor;

import java.util.ArrayList;
import java.util.List;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;

/**
 * A sample centre agent.
 */
public class BasicIAMCentre extends AbstractIAMAgent<Building> {
	@Override
	public String toString() {
		return "Basic IAM Centre";
	}

	@Override
	protected void fallback(int time, ChangeSet changed) {
		// TODO Auto-generated method stub

	}

	@Override
	protected List<StandardEntityURN> getAgentTypes() {
		List<StandardEntityURN> list = new ArrayList<StandardEntityURN>();
		list.add(StandardEntityURN.AMBULANCE_CENTRE);
		list.add(StandardEntityURN.FIRE_STATION);
		list.add(StandardEntityURN.POLICE_OFFICE);
		return list;

	}

	@Override
	protected void think(int time, ChangeSet changed) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void postConnect() {
		// TODO Auto-generated method stub
		super.postConnect();

//		this.showFireImportanceModel(new FastFirePredictor(getTimer(),
	//			getWorldModel(), me().getID()));
	}
}
