/**
 * 
 */
package iamrescue.routing.costs;

import iamrescue.belief.IAMWorldModel;
import iamrescue.execution.command.IPath.BlockedState;

/**
 * @author Sebastian
 * 
 */
public class StatusRoutingCostFunction extends PassableRoutingCostFunction {
	public StatusRoutingCostFunction(BlockedState state,
			IAMWorldModel worldModel) {
		super(state.equals(BlockedState.UNBLOCKED) ? 1 : 0, state
				.equals(BlockedState.UNKNOWN) ? 1 : 0, state
				.equals(BlockedState.BLOCKED) ? 1 : 0, worldModel);
	}
}
