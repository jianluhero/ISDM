package iamrescue.agent.firebrigade;

import iamrescue.agent.AbstractIAMAgent;
import iamrescue.communication.messages.Message;
import iamrescue.execution.command.RestCommand;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;

public class IAMFireCentre extends AbstractIAMAgent {

	@Override
	protected void think(int time, ChangeSet changed) {
		Collection<Message> unreadMessages = getCommunicationModule()
				.getUnreadMessages();

		System.out.println("Centre is thinking " + time);

		for (Message message : unreadMessages) {
			System.out.println("Received message " + message);
			message.markAsRead();
		}

		getExecutionService().execute(new RestCommand());
	}

	@Override
	protected List<StandardEntityURN> getAgentTypes() {
		return Collections.singletonList(StandardEntityURN.FIRE_STATION);
	}

	@Override
	protected void fallback(int time, ChangeSet changed) {
		// TODO Auto-generated method stub

	}

	@Override
	public void postConnect() {
		// TODO Auto-generated method stub

	}
}
