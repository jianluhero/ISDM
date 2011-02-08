package iamrescue.agent.firebrigade;

public class FireStrategyState {

	/**
	 * Determine what the agent is doing.
	 * @author heatherpacker
	 *
	 */
	public enum AgentState {
		FREE, // Agent just started, reset its state, or finished with a target,
			  // which mean that they will try to get a target the next turn
		TRAVELLING, // Travelling to the target
		EXTINGUISHING, // Extinguishing the target
	}
	
	
	private AgentState agentState;
	private int timeAtTarget = 0; 	// how many timesteps the agent has been at
									// its current target
	private int timePerTarget = 5;	// how many timesteps to spend at each
									// target
	
	public FireStrategyState(int timePerTarget){
		this.agentState = AgentState.FREE;
		this.timePerTarget = timePerTarget;
	}
	
	public int addTimeAtTarget(){
		this.timeAtTarget++;
		return timeAtTarget;
	}

	public void setState(AgentState agentState){
		this.agentState = agentState;
	}
	
	public AgentState getAgentState(){
		return this.agentState;
	}
	
	public int getTimePerTarget(){
		return this.timePerTarget;
	}
	
	public void resetTimePerTarget(){
		this.timePerTarget = 0;
	}
}
