package iamrescue.execution.command;

import iamrescue.execution.IExecutionService;

public class RestCommand extends AgentCommand {

	@Override
	public void checkValidity() {
		
	}

	@Override
	public void execute(IExecutionService service) {
		service.execute(this);
	}

}
