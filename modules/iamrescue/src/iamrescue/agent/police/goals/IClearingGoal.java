package iamrescue.agent.police.goals;

import rescuecore2.standard.entities.PoliceForce;

public interface IClearingGoal {

	public double getCurrentUtility();

	public boolean isDone();

	public void evaluateCurrentState();

	public double getCost(PoliceForce agent);

}
