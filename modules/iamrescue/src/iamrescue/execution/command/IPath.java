package iamrescue.execution.command;

import iamrescue.belief.IAMWorldModel;
import iamrescue.routing.costs.IRoutingCostFunction;
import iamrescue.util.PositionXY;

import java.util.List;

import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

/**
 * A Path represents a chain of nodes, roads, rivers, rivernodes, buildings
 * through the map.
 * 
 * @author rs06r
 * 
 */
public interface IPath {
	public static enum BlockedState {
		UNBLOCKED, BLOCKED, UNKNOWN
	}

	/**
	 * Returns the state of this path
	 * 
	 * @return
	 */
	public BlockedState getBlockedState(IAMWorldModel worldModel);

	public PositionXY getStartingPosition();

	public PositionXY getEndPosition();

	public List<PositionXY> getXYPath();

	public String toVerboseString();

	/**
	 * @return List of map entities to visit in order to reach destination.
	 *         These should be fixed objects - roads, nodes, buildings.
	 */
	public List<EntityID> getLocations();

	/**
	 * 
	 * @return True if the path leads to the destination. False indicates no
	 *         path was found.
	 */
	public boolean isValid();

	/**
	 * Describes path using the world model.
	 * 
	 * @param worldModel
	 * @return
	 */

	public String toString(StandardWorldModel worldModel);

	/**
	 * First location on path
	 * 
	 * @return First location on path
	 */
	public EntityID getStart();

	/**
	 * 
	 * @return Last location on path
	 */
	public EntityID getDestination();

	/**
	 * Get the last known cost for this path. This may only be set at path
	 * creation for efficiency. To get the latest cost, use calculateCost
	 * method.
	 * 
	 * @return The last known cost for this path.
	 */
	public double getCost();

	/**
	 * Recomputes the cost of this path. This is useful when the world model has
	 * been updated (e.g., by blockades). Should also store the cost in the
	 * path, so that subsequent calls to getCost return the same result.
	 * 
	 * @param roadCostFunction
	 *            The cost function to use. Can supply the same as used by the
	 *            routing module (getRoutingCostFunction()).
	 * @param worldModel
	 *            The world model to supply blockades,etc.)
	 * @return The current cost of the path
	 */
	public double calculateCost(IRoutingCostFunction roadCostFunction,
			IAMWorldModel worldModel);

	/**
	 * Returns true if it is known that this path contains a burning building.
	 * 
	 * @param model
	 * @return
	 */
	public boolean containsBurningBuilding(StandardWorldModel model);
	
	public IPath removeLastNode();
}