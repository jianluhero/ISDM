/**
 * 
 */
package iamrescue.belief.provenance;

import iamrescue.agent.ISimulationTimer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * @author Sebastian
 * 
 */
public class FastProvenanceInformation implements IProvenanceInformation {

	private static final Logger LOGGER = Logger
			.getLogger(FastProvenanceInformation.class);

	private static List<ProvenanceLogEntry> EMPTY_LIST = new ArrayList<ProvenanceLogEntry>();

	private ProvenanceLogEntry previous = null;
	private ProvenanceLogEntry latest = null;
	private ProvenanceLogEntry latestDefined = null;

	private List<ProvenanceLogEntry> infoList = new ArrayList<ProvenanceLogEntry>(
			2);

	private boolean listValid = false;

	/**
	 * 
	 */
	public FastProvenanceInformation() {

	}

	public void addEntry(ProvenanceLogEntry entry) {

		if (latest == null) {
			latest = entry;
			updateLatestDefined();
			return;
		}

		int compared = ProvenanceLogEntry.TIME_ORIGIN_COMPARATOR.compare(entry,
				latest);

		if (compared > 0) {
			// This becomes latest
			previous = latest;
			latest = entry;
			updateLatestDefined();
			listValid = false;
		} else if (compared < 0) {
			if (previous == null) {
				previous = entry;
				updateLatestDefined();
			} else {
				int comparedPrevious = ProvenanceLogEntry.TIME_ORIGIN_COMPARATOR
						.compare(entry, previous);
				if (comparedPrevious > 0) {
					previous = entry;
					updateLatestDefined();
					listValid = false;
				}
			}
		}
	}

	private void updateLatestDefined() {
		if (latest != null && latest.getProperty().isDefined()) {
			latestDefined = latest;
		} else if (previous != null && previous.getProperty().isDefined()) {
			latestDefined = previous;
		}
	}

	public ProvenanceLogEntry getLatest() {
		return latest;
	}

	public Iterator<ProvenanceLogEntry> getAllLatestFirst() {
		if (!listValid) {
			listValid = true;
			if (latest == null) {
				return EMPTY_LIST.iterator();
			} else if (previous == null) {
				return Collections.singletonList(latest).iterator();
			} else {
				if (infoList.size() == 0) {
					infoList.add(latest);
					infoList.add(previous);
				} else {
					infoList.set(0, latest);
					infoList.set(1, previous);
				}
				return infoList.iterator();
			}
		}
		return infoList.iterator();
	}

	@Override
	public ProvenanceLogEntry getLastDefined() {
		return latestDefined;
	}
}
