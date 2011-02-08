package iamrescue.communication;

import iamrescue.agent.ISimulationTimer;
import iamrescue.communication.messages.MessageChannel;
import rescuecore2.connection.Connection;
import rescuecore2.connection.ConnectionException;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;


public class RescueCore2OutgoingMessageService extends AOutgoingMessageService {

	private Connection connection;
	private EntityID id;
	private ISimulationTimer timer;

	public RescueCore2OutgoingMessageService(EntityID id,
			Connection connection, ISimulationTimer timer) {
		this.connection = connection;
		this.id = id;
		this.timer = timer;
	}

	@Override
	public void sendMessage(byte[] message, MessageChannel channel) {
		// TODO check if these commands are correct
		try {
			connection.sendMessage(new AKSpeak(id, timer.getTime(), channel
					.getChannelNumber(), message));
		} catch (ConnectionException e) {
			e.printStackTrace();
		}
	}

	// public void sendRadioMessage(byte[] message, MessageChannel channel) {
	//		
	// }
	//
	// public void sendShoutMessage(byte[] message) {
	// // TODO check if these commands are correct
	// try {
	// connection.sendMessage(new AKSay(id, timer.getTime(), message));
	// } catch (ConnectionException e) {
	// e.printStackTrace();
	// }
	// }
}
