/**
 * 
 */
package iamrescue.routing.costs;

import iamrescue.belief.IAMWorldModel;
import iamrescue.execution.command.IPath.BlockedState;
import iamrescue.util.PositionXY;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;

/**
 * @author Sebastian
 * 
 */
public class SimpleDistanceRoutingCostFunction extends
		AbstractRoutingCostFunction {

	// public static final SimpleDistanceRoutingCostFunction DEFAULT_INSTANCE =
	// new SimpleDistanceRoutingCostFunction();
	public static final SimpleDistanceRoutingCostFunction DEFAULT_NO_BLOCK_INSTANCE = new SimpleDistanceRoutingCostFunction(
			null, true);

	private boolean ignoreBlocks = false;

	private double burningPenalty = 0;

	protected IAMWorldModel worldModel;

	/**
	 * 
	 */
	public SimpleDistanceRoutingCostFunction(IAMWorldModel worldModel) {
		this(worldModel, false);
		this.worldModel = worldModel;
	}

	public SimpleDistanceRoutingCostFunction(IAMWorldModel worldModel, 
			boolean ignoreBlocks) {
		this.ignoreBlocks = ignoreBlocks;
		this.worldModel = worldModel;
	}

	/**
	 * @param burningPenalty
	 *            the burningPenalty to set
	 */
	public void setBurningPenalty(double burningPenalty) {
		this.burningPenalty = burningPenalty;
	}

	/**
	 * @return the burningPenalty
	 */
	public double getBurningPenalty() {
		return burningPenalty;
	}

	@Override
	public double getTravelCost(Area area, PositionXY from, PositionXY to) {
		double cost = 0;
		if (burningPenalty > 0 && area instanceof Building) {
			Building b = (Building) area;
			if (b.isFierynessDefined()) {
				int fieryness = b.getFieryness();
				if (fieryness >= 1 && fieryness <= 3) {
					cost += burningPenalty;
				}
			}
		}
		if (ignoreBlocks) {
			cost += from.distanceTo(to);
		} else {
			if (BlockCheckerUtil.getBlockedState(area, worldModel, from, to)
					.equals(BlockedState.BLOCKED)) {
				cost = Double.POSITIVE_INFINITY;
			} else {
				cost += from.distanceTo(to);
			}
		}
		return cost;
	}
}
