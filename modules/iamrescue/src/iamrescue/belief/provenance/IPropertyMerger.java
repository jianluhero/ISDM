/**
 * 
 */
package iamrescue.belief.provenance;

import iamrescue.belief.provenance.IProvenanceInformation;
import rescuecore2.worldmodel.Property;

/**
 * @author Sebastian
 *
 */
public interface IPropertyMerger {

	/**
	 * @param provenance
	 * @return
	 */
	Property decideValue(IProvenanceInformation provenance);

}
