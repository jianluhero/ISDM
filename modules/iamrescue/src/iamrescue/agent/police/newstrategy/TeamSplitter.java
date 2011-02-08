package iamrescue.agent.police.newstrategy;

import iamrescue.belief.IAMWorldModel;
import iamrescue.util.comparators.EntityIDComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public class TeamSplitter {
	public static StandardEntityURN getMyPreferredTarget(
			IAMWorldModel worldModel, EntityID myID, int policeProportion,
			int ambulanceProportion, int fireProportion) {
		Collection<StandardEntity> police = worldModel
				.getEntitiesOfType(StandardEntityURN.POLICE_FORCE);

		List<StandardEntity> list = new ArrayList<StandardEntity>(police);

		Collections.sort(list, EntityIDComparator.DEFAULT_INSTANCE);

		double total = policeProportion + ambulanceProportion + fireProportion;

		int toFire = (int) (list.size() * fireProportion / total);
		int toAmbulance = (int) (list.size() * ambulanceProportion / total);
		// int toRefuge = list.size() - toFire - toAmbulance;

		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).equals(myID)) {
				if (i < toFire) {
					return StandardEntityURN.FIRE_BRIGADE;
				} else if (i < toAmbulance) {
					return StandardEntityURN.AMBULANCE_TEAM;
				} else {
					return StandardEntityURN.POLICE_FORCE;
				}
			}
		}
		return StandardEntityURN.POLICE_FORCE;

	}
}
