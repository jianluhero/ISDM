/**
 * 
 */
package iamrescue.util.comparators;

import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.provenance.IProvenanceInformation;

import java.util.Comparator;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;

/**
 * @author Simon
 */
public class RoadComparator implements Comparator<StandardEntity> {

	private IAMWorldModel model;

	public RoadComparator(IAMWorldModel model) {
		this.model = model;
	}

	public int compare(StandardEntity se1, StandardEntity se2) {
		IProvenanceInformation time1 = model.getProvenance(se1.getID(),
				StandardPropertyURN.BLOCKADES);
		IProvenanceInformation time2 = model.getProvenance(se2.getID(),
				StandardPropertyURN.BLOCKADES);

		if (time1 == null || time1.getLastDefined() == null) {
			if (time2 == null || time2.getLastDefined() == null) {
				return Double.compare(se1.getID().getValue(), se2.getID()
						.getValue());
			} else {
				return -1;
			}
		} else {
			if (time2 == null || time2.getLastDefined() == null) {
				return 1;
			} else {
				int intT1 = time1.getLastDefined().getTimeStep();
				int intT2 = time2.getLastDefined().getTimeStep();
				if (intT1 == intT2) {
					return Double.compare(se1.getID().getValue(), se2.getID()
							.getValue());
				} else {
					return Double.compare(intT1, intT2);
				}
			}
		}
	}

}
