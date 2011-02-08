/**
 * 
 */
package iamrescue.agent.police.goals;

import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class PoliceClearingGoal extends SimpleClearingGoal {

    public PoliceClearingGoal(EntityID target, ClearingGoalConfiguration config) {
        super(target, 1, 1, config);
    }

}
