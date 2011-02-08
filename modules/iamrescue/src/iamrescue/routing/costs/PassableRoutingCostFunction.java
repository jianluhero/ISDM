/**
 * 
 */
package iamrescue.routing.costs;

import iamrescue.belief.IAMWorldModel;
import iamrescue.execution.command.IPath.BlockedState;
import iamrescue.util.PositionXY;
import rescuecore2.standard.entities.Area;

/**
 * @author Sebastian
 * 
 */
public class PassableRoutingCostFunction extends AbstractRoutingCostFunction {

	/*
	 * public static final PassableRoadCostFunction DEFAULT_TEST_INSTANCE = new
	 * PassableRoadCostFunction( 0, 1, Double.POSITIVE_INFINITY);
	 */

	public static final double DEFAULT_FREE_COST = 0d;
	public static final double DEFAULT_UNKNOWN_COST = 1d;
	public static final double DEFAULT_BLOCKED_COST = Double.POSITIVE_INFINITY;

	private double freeCost;

	private double unknownCost;

	private double blockedCost;

	private IAMWorldModel worldModel;
	
	//private boolean pessimistic;

	public PassableRoutingCostFunction(IAMWorldModel worldModel) {
		this(DEFAULT_FREE_COST, DEFAULT_UNKNOWN_COST, DEFAULT_BLOCKED_COST,
				worldModel);//, false);
	}

	public PassableRoutingCostFunction(double freeCost, double unknownCost,
			double blockedCost, IAMWorldModel worldModel){//, boolean pessimistic) {
		super();
		this.worldModel = worldModel;
		this.freeCost = freeCost;
		this.unknownCost = unknownCost;
		this.blockedCost = blockedCost;
		//this.pessimistic = pessimistic;
	}

	@Override
	public double getTravelCost(Area area, PositionXY from, PositionXY to) {
		BlockedState state = BlockCheckerUtil.getBlockedState(area, worldModel,
				from, to);
		if (state.equals(BlockedState.BLOCKED)) {
			return blockedCost;
		} else if (state.equals(BlockedState.UNKNOWN)) {
			return unknownCost;
		} else {
			return freeCost;
		}
	}
}
