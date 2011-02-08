/**
 * 
 */
package iamrescue.routing.queries;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Sebastian
 * 
 */
public class RoutingQuery implements IRoutingQuery {

	private IRoutingLocation start;
	private Collection<IRoutingLocation> destinations;

	public RoutingQuery(IRoutingLocation start,
			Collection<IRoutingLocation> destinations) {
		this.start = start;
		this.destinations = destinations;
	}

	public RoutingQuery(IRoutingLocation start, IRoutingLocation destination) {
		this.start = start;
		this.destinations = Collections.singleton(destination);
	}

	public Collection<IRoutingLocation> getDestinationLocations() {
		return destinations;
	}

	public IRoutingLocation getStartLocation() {
		return start;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Routing Query: from " + start.toString() + " to "
				+ destinations.toString();
	}

}
