package iamrescue.communication.scenario;

import iamrescue.agent.ISimulationTimer;
import iamrescue.communication.IMessagingSchedule;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public interface ICommunicationScenario {

	/**
	 * Returns the channels to communicate with other centers
	 * 
	 * @return
	 */
	List<MessageChannel> getChannelsToOtherTeams();

	/**
	 * Regardless of my actual type, what is the role I should be playing? I.e.
	 * if there is no Ambulance Center, an Ambulance might take this role.
	 * 
	 * @return
	 */
	StandardEntityURN getMyRole();

	/**
	 * Returns the channels to communicate with platoons that are associated
	 * with me
	 * 
	 * @return
	 */
	List<MessageChannel> getChannelsToOwnTeam();

	IMessagingSchedule getScheduler();

	/**
	 * Returns a list of channels this agent should listen to
	 * 
	 * @return
	 */
	List<MessageChannel> getChannelsToSubscribeTo();

	/**
	 * Should distribute messages to the various teams on the channels.
	 * 
	 * @return
	 */
	Map<MessageChannel, List<Message>> distributeMessages(
			List<Message> messagesToOwnTeam,
			List<Message> messagesToOtherTeams, ISimulationTimer timer);

	void reinitialiseTeam(Collection<EntityID> toIgnore);

	Collection<EntityID> getMyCentres();

	boolean amICentre();
}
