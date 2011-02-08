package iamrescue.routing.costs;

import iamrescue.execution.command.IPath;
import iamrescue.util.PositionXY;

import java.util.List;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.StandardWorldModel;

public interface IRoutingCostFunction {

	/**
	 * Calculates the cost of moving through the given area, starting from the
	 * position given by "from" to the position given by "to".
	 * 
	 * @param area
	 *            Area to traverse
	 * @param from
	 *            Starting position
	 * @param to
	 *            Destination
	 * @return Cost of travel
	 */
	public double getTravelCost(Area area, PositionXY from, PositionXY to);

	/**
	 * Calculates the entire cost of the path.
	 * 
	 * @param path
	 *            The path.
	 * @param worldModel
	 *            The world model (to extract info about path).
	 * @return The cost of the path.
	 */
	public double getCost(IPath path, StandardWorldModel worldModel);
}
