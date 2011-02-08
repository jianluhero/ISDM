/**
 * 
 */
package iamrescue.belief.provenance;

import java.util.Collection;

import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public interface IProvenanceStore {
	/**
	 * Returns provenance information about the given entity/property pair.
	 * 
	 * @param id
	 *            The ID of the object
	 * @param property
	 *            The property in question
	 * @return Provenance information about this pair (or null if unavailable)
	 */
	public IProvenanceInformation getProvenance(EntityID id,
			String propertyURN);

	/**
	 *Gets all known properties about this entity
	 * 
	 * @param id
	 *            The id of the entity
	 * @return All properties about which there is provenance information (empty
	 *         if none is known).
	 */
	public Collection<String> getKnownProperties(EntityID id);

	/**
	 * Stores given provenance information.
	 * 
	 * @param id
	 *            ID of the object
	 * @param entry
	 *            The entry to store.
	 */
	public void storeProvenance(EntityID id, ProvenanceLogEntry entry);
}
