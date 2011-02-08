/**
 * 
 */
package iamrescue.belief.provenance;

import iamrescue.agent.ISimulationTimer;

import java.util.Iterator;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * @author Sebastian
 * 
 */
public class FullHistoryProvenanceInformation implements IProvenanceInformation {

	private static final Logger LOGGER = Logger
			.getLogger(FullHistoryProvenanceInformation.class);

	private ISimulationTimer timer;

	private int maxSize = -1;

	private TreeSet<ProvenanceLogEntry> log;

	/**
	 * 
	 */
	public FullHistoryProvenanceInformation() {
		log = new TreeSet<ProvenanceLogEntry>();
	}

	public FullHistoryProvenanceInformation(int maxSize) {
		if (maxSize <= 0) {
			throw new IllegalArgumentException(
					"Max size must be greater than zero.");
		}
		log = new TreeSet<ProvenanceLogEntry>();
		this.maxSize = maxSize;
	}

	public void addEntry(ProvenanceLogEntry entry) {
		log.add(entry);

		/*
		 * if (log.size() > 1 && entry.getTimeStep() <= log.get(log.size() -
		 * 2).getTimeStep()) { Collections.sort(log,
		 * ProvenanceLogEntry.TIME_ORIGIN_COMPARATOR); }
		 */
		if (log.size() > maxSize) {
			log.remove(log.first());
		}
	}

	public ProvenanceLogEntry getLatest() {
		return log.last();
	}

	public Iterator<ProvenanceLogEntry> getAllLatestFirst() {
		return log.descendingIterator();
	}

	@Override
	public ProvenanceLogEntry getLastDefined() {
		Iterator<ProvenanceLogEntry> descendingIterator = log
				.descendingIterator();
		while (descendingIterator.hasNext()) {
			ProvenanceLogEntry entry = descendingIterator.next();
			if (entry.getProperty().isDefined()) {
				return entry;
			}
		}
		return null;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("FullHistoryProvenance[");
		// for (ProvenanceLogEntry entry : getAll()) {
		// // sb.append(entry.)
		// }

		// sb.append();
		return sb.toString();
	}

}
