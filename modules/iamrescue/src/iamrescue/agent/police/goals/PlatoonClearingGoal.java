/**
 * 
 */
package iamrescue.agent.police.goals;

import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class PlatoonClearingGoal extends SimpleClearingGoal {

	public PlatoonClearingGoal(EntityID id, ClearingGoalConfiguration config) {
		super(id, .9, .5, config);
	}
}
