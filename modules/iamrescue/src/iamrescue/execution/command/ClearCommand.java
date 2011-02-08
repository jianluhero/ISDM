package iamrescue.execution.command;

import iamrescue.execution.IExecutionService;

import org.apache.commons.lang.Validate;

import rescuecore2.standard.entities.Blockade;

public class ClearCommand extends PoliceCommand {

	private Blockade blockadeToClear;

	/**
	 * 
	 */
	public ClearCommand() {
		super();
	}

	public ClearCommand(Blockade blockadeToClear) {
		super();
		setBlockadeToClear(blockadeToClear);
	}

	public void setBlockadeToClear(Blockade blockadeToClear) {
		this.blockadeToClear = blockadeToClear;
	}

	public Blockade getBlockadeToClear() {
		return blockadeToClear;
	}

	public void execute(IExecutionService service) {
		service.execute(this);
	}

	public void checkValidity() {
		Validate.notNull(blockadeToClear);
	}

}
