/**
 * 
 */
package iamrescue.routing.queries;

import java.util.Collection;

/**
 * 
 * Represents a single shortest path query from the start location to *the
 * closest* of the destination locations.
 * 
 * @author Sebastian
 * 
 */
public interface IRoutingQuery {
	/**
	 * 
	 * @return THe starting location.
	 */
	public IRoutingLocation getStartLocation();

	/**
	 * 
	 * @return The possible destinations. When run as a query, the closest to
	 *         the start will be used.
	 */
	public Collection<IRoutingLocation> getDestinationLocations();
}
