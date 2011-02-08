/**
 * 
 */
package iamrescue.agent.police.goals;

import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class FireClearingGoal extends SimpleClearingGoal {

	public FireClearingGoal(EntityID id, ClearingGoalConfiguration config) {
		super(id, .8, .75, config);
	}
}
