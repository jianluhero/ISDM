package iamrescue.execution.command;

import iamrescue.execution.IExecutionService;
import rescuecore2.standard.entities.Human;

public class DigOutCommand extends AmbulanceCommand {

	private Human object;

	public DigOutCommand(Human object) {
		this.object = object;
	}

	public Human getObjectToDigOut() {
		return object;
	}

	public void execute(IExecutionService service) {
		service.execute(this);
	}

	public void checkValidity() {
		assert object != null;
	}
}
