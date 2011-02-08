package iamrescue.agent.ambulanceteam.ambulancetools;

import iamrescue.execution.command.IPath;
import rescuecore2.standard.entities.Human;

public class Task {
	public Human civilian;
	public IPath path;
	
	public Task(Human c, IPath p){
		civilian=c;
		path = p;
	}
}
