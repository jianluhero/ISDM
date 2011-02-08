/**
 * 
 */
package iamrescue.belief.provenance;

import java.util.Iterator;

/**
 * @author Sebastian
 * 
 */
public interface IProvenanceInformation {

	public void addEntry(ProvenanceLogEntry entry);

	public ProvenanceLogEntry getLatest();

	public ProvenanceLogEntry getLastDefined();

	public Iterator<ProvenanceLogEntry> getAllLatestFirst();
}
