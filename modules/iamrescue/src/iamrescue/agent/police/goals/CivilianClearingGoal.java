/**
 * 
 */
package iamrescue.agent.police.goals;

import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class CivilianClearingGoal extends SimpleClearingGoal {

	public CivilianClearingGoal(EntityID id, ClearingGoalConfiguration config) {
		super(id, .7, .6, config);
	}
}
