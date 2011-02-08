/**
 * 
 */
package iamrescue.belief.provenance;

import java.util.Comparator;

import rescuecore2.worldmodel.Property;

/**
 * @author Sebastian
 * 
 */
public class ProvenanceLogEntry implements Comparable<ProvenanceLogEntry> {
	private int timeStep;
	private IOrigin origin;
	private Property property;

	/**
	 * A comparator that compares log entries based on their time stamps
	 * (earlier times come first).
	 */
	public static final Comparator<ProvenanceLogEntry> TIME_ORIGIN_COMPARATOR = new Comparator<ProvenanceLogEntry>() {

		@Override
		public int compare(ProvenanceLogEntry arg0, ProvenanceLogEntry arg1) {
			if (arg0.getTimeStep() < arg1.getTimeStep()) {
				return -1;
			} else if (arg0.getTimeStep() > arg1.getTimeStep()) {
				return +1;
			} else {
				int originvalue1 = assignValue(arg0.origin);
				int originvalue2 = assignValue(arg1.origin);
				if (originvalue1 < originvalue2) {
					return -1;
				} else if (originvalue1 > originvalue2) {
					return 1;
				} else {
					return arg0.property.toString().compareTo(
							arg1.property.toString());
				}
			}
		}

		private int assignValue(IOrigin origin) {
			if (origin instanceof InferredOrigin) {
				return 1;
			} else if (origin instanceof AgentCommunicationOrigin) {
				return 2;
			} else if (origin instanceof SensedOrigin) {
				return 3;
			} else {
				throw new IllegalArgumentException(
						"Don't know about this origin class: "
								+ origin.getClass());
			}
		}
	};

	/**
	 * 
	 * @param timeStep
	 *            The time step at which this information was originally sensed.
	 * @param origin
	 *            Where this information came from
	 * @param property
	 *            The property associated with this entry (including value)
	 */
	public ProvenanceLogEntry(int timeStep, IOrigin origin, Property property) {
		this.timeStep = timeStep;
		this.origin = origin;
		this.property = property;
	}

	/**
	 * 
	 * @return the timeStep at which this was originally sensed.
	 */
	public int getTimeStep() {
		return timeStep;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "LogEntry:time=" + timeStep + ",origin=" + origin + ",property"
				+ property.toString();
	}

	/**
	 * @return the origin
	 */
	public IOrigin getOrigin() {
		return origin;
	}

	/**
	 * @return the property
	 */
	public Property getProperty() {
		return property;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((origin == null) ? 0 : origin.hashCode());
		result = prime * result
				+ ((property == null) ? 0 : property.hashCode());
		result = prime * result + timeStep;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProvenanceLogEntry other = (ProvenanceLogEntry) obj;
		if (origin == null) {
			if (other.origin != null)
				return false;
		} else if (!origin.equals(other.origin))
			return false;
		if (property == null) {
			if (other.property != null)
				return false;
		} else if (!property.equals(other.property))
			return false;
		if (timeStep != other.timeStep)
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ProvenanceLogEntry o) {
		return TIME_ORIGIN_COMPARATOR.compare(this, o);
	}

}
