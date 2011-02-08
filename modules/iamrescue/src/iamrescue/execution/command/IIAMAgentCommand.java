package iamrescue.execution.command;

import iamrescue.execution.IExecutionService;

public interface IIAMAgentCommand {
	void execute(IExecutionService service);

	/**
	 * If needed, implement this method to perform checks to ensure validity of
	 * the command
	 */
	void checkValidity();
}
