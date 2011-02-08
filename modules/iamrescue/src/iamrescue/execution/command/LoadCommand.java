package iamrescue.execution.command;

import iamrescue.execution.IExecutionService;

import org.apache.commons.lang.Validate;

import rescuecore2.standard.entities.Civilian;

/**
 * Load a civilian onto the ambulance
 * 
 * @author rs06r
 * 
 */
public class LoadCommand extends AmbulanceCommand {

	private Civilian civilianToLoad;
	
	public LoadCommand(Civilian civilian) {
		this.civilianToLoad = civilian;
	}
	
	public LoadCommand() {
	
	}

	public void setCivilianToLoad(Civilian civilianToLoad) {
		this.civilianToLoad = civilianToLoad;
	}

	public Civilian getCivilianToLoad() {
		return civilianToLoad;
	}

	public void execute(IExecutionService service) {
		service.execute(this);
	}

	public void checkValidity() {
		Validate.notNull(civilianToLoad);

	}
}
