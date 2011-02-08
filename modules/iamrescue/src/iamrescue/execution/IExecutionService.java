package iamrescue.execution;

import rescuecore2.messages.AbstractCommand;
import iamrescue.execution.command.ClearCommand;
import iamrescue.execution.command.DigOutCommand;
import iamrescue.execution.command.ExtinguishCommand;
import iamrescue.execution.command.IIAMAgentCommand;
import iamrescue.execution.command.LoadCommand;
import iamrescue.execution.command.MoveCommand;
import iamrescue.execution.command.RandomMoveCommand;
import iamrescue.execution.command.RandomStepCommand;
import iamrescue.execution.command.RestCommand;
import iamrescue.execution.command.UnloadCommand;

public interface IExecutionService {
	/**
	 * Adds a fully instantiated AgentCommand to the command buffer. The buffer
	 * is flushed to the simulator when flushCommands() is called.
	 * 
	 * @param command
	 * @throws UnknownCommandException
	 *             is thrown
	 * @throws IllegalStateException
	 */

	void performCommand(IIAMAgentCommand command)
			throws UnknownCommandException, IllegalStateException;

	void execute(DigOutCommand rescueCommand);

	void execute(UnloadCommand unloadCommand);

	void execute(LoadCommand loadCommand);

	void execute(ClearCommand clearCommand);

	void execute(ExtinguishCommand extinguishCommand);

	void execute(MoveCommand moveCommand);

	void execute(RandomMoveCommand randomMoveCommand);

	/**
	 * @param randomStepCommand
	 */
	void execute(RandomStepCommand randomStepCommand);

	/**
	 * Sends all buffered commands to the simulator
	 */
	void flushCommands();

	void execute(RestCommand restCommand);

	/**
	 * Commands submitted in this timestep
	 */
	boolean NotSubmittedCommand();

	/**
	 * 
	 * @return All commands that were last submitted to the server. Should store
	 *         exactly the commands submitted at the last flush command.
	 */
	IIAMAgentCommand getLastSubmittedCommand();

	int getLastRandomMoveTime();

	AbstractCommand getEnqueuedCommand();
	
	int getConsecutiveRandomSteps();

}
