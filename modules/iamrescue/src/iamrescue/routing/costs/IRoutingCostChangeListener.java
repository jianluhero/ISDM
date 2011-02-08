/**
 * 
 */
package iamrescue.routing.costs;

import rescuecore2.standard.entities.Area;

/**
 * @author Sebastian
 * 
 */
public interface IRoutingCostChangeListener {
	public void routingCostChanged(Area area);

	public void allRoutingCostsChanged();
}
