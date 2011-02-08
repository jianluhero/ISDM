/**
 * 
 */
package iamrescue.routing.queries;

import iamrescue.util.PositionXY;
import rescuecore2.worldmodel.EntityID;

/**
 * Defines a single location to route to or from.
 * 
 * @author Sebastian
 * 
 */
public interface IRoutingLocation {
	public EntityID getID();

	public boolean hasPositionDefined();

	public PositionXY getPositionXY();

}
