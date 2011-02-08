/**
 * 
 */
package iamrescue.belief.provenance;

import rescuecore2.worldmodel.Property;

/**
 * @author Sebastian
 *
 */
public class LatestProperty implements IPropertyMerger{

	/* (non-Javadoc)
	 * @see belief.provenance.IPropertyMerger#decideValue(belief.provenance.IProvenanceInformation)
	 */
	@Override
	public Property decideValue(IProvenanceInformation provenance) {
	//	FullHistoryProvenanceInformation info = (FullHistoryProvenanceInformation) provenance;
		ProvenanceLogEntry latest = provenance.getLatest();
		return latest.getProperty();
	}

}
