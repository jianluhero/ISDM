package iamrescue.execution.command;

import iamrescue.execution.IExecutionService;

/**
 * Unload a civilian from the ambulance. The executionservice should check if a
 * civilian is currently loaded.
 * 
 * @author rs06r
 * 
 */
public class UnloadCommand extends AmbulanceCommand {
	public void execute(IExecutionService service) {
		service.execute(this);
	}

	public void checkValidity() {

	}
}
