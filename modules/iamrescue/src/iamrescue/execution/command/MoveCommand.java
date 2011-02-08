package iamrescue.execution.command;

import iamrescue.execution.IExecutionService;

import org.apache.commons.lang.Validate;

public class MoveCommand extends AgentCommand {
	private IPath path;

	public MoveCommand(IPath path) {
		super();
		setPath(path);
	}

	public MoveCommand() {
		super();
	}

	public IPath getPath() {
		return path;
	}

	public void setPath(IPath path) {
		this.path = path;
		if (!path.isValid()) {
			throw new IllegalArgumentException("Path " + path + " is invalid.");
		}
	}

	public void execute(IExecutionService service) {
		service.execute(this);
	}

	public void checkValidity() {
		Validate.notNull(path);
		Validate.notEmpty(path.getLocations());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MoveCommand other = (MoveCommand) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}
	
	

}
