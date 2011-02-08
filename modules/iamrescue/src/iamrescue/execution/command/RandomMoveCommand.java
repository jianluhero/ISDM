/**
 * 
 */
package iamrescue.execution.command;

import iamrescue.execution.IExecutionService;

/**
 * @author Sebastian
 * 
 */
public class RandomMoveCommand extends AgentCommand {

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.execution.command.IIAMAgentCommand#checkValidity()
	 */
	@Override
	public void checkValidity() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.execution.command.IIAMAgentCommand#execute(iamrescue.execution
	 * .IExecutionService)
	 */
	@Override
	public void execute(IExecutionService service) {
		service.execute(this);

	}

}
