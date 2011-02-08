package iamrescue.communication;

import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.messages.MessageChannelConfiguration;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import rescuecore2.config.Config;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public interface ISimulationCommunicationConfiguration {

	/**
	 * Total number of available channels
	 * 
	 * @return
	 */
	int getChannelCount();

	Config getConfig();

	/**
	 * Number of available radio channels
	 * 
	 * @return
	 */
	int getRadioChannelCount();

	/**
	 * returns the list of available channels. See {@link MessageChannel} for
	 * more details
	 */
	List<MessageChannel> getChannels();

	/**
	 * The maximum number of channels a platoon can listen to
	 * 
	 * @return
	 */
	int getMaxListenChannelCountPlatoon();

	/**
	 * The maximum number of channels a centre can listen to
	 * 
	 * @return
	 */
	int getMaxListenChannelCountCentre();

	/**
	 * The maximum number of channels *this* agent can listen to
	 * 
	 * @return
	 */
	int getMaxListenChannelCount();

	Map<StandardEntityURN, Collection<StandardEntity>> getAgentsByType();

	StandardEntityURN getAgentType();

	/**
	 * Returns a subset of radio channels given a set of indices. The ordering
	 * should be the same as getRadioChannels()
	 * 
	 * @param i
	 * @return
	 */
	List<MessageChannel> getRadioChannels(int... index);

	/**
	 * Returns a list of radio channels. Different calls are guaranteed to
	 * return the same ordering
	 * 
	 * @return
	 */
	List<MessageChannel> getRadioChannels();

	List<MessageChannel> getVoiceChannels();

	MessageChannel getRadioChannel(int i);

	EntityID getEntityID();

	Map<StandardEntityURN, Collection<StandardEntity>> getCentresByType();

	Map<StandardEntityURN, Collection<StandardEntity>> getPlatoonsByType();

	MessageChannelConfiguration getChannelConfiguration();
}
