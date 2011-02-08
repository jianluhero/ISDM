package iamrescue.communication.scenario.scenarios;

import iamrescue.communication.ISimulationCommunicationConfiguration;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.scenario.ICommunicationScenario;
import iamrescue.communication.scenario.ICommunicationScenarioFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;

public class FallbackScenarioFactory implements ICommunicationScenarioFactory {

	private static final Logger LOGGER = Logger
			.getLogger(FallbackScenarioFactory.class);

	@Override
	public ICommunicationScenario create(
			final ISimulationCommunicationConfiguration configuration) {

		List<MessageChannel> channels = configuration.getRadioChannels();

		int agents = 0;
		for (Entry<StandardEntityURN, Collection<StandardEntity>> entry : configuration
				.getAgentsByType().entrySet()) {
			agents += entry.getValue().size();
		}

		if (channels.size() == 0
				|| configuration.getMaxListenChannelCount() == 0) {
			LOGGER.warn("No channels found. No communication possible");
			return new NoCommunicationScenario(configuration.getAgentType(),
					configuration.getVoiceChannels());
		} else {
			// Find channel with highest bandwidth
			int highestBandwidth = Integer.MIN_VALUE;
			MessageChannel selected = null;
			for (MessageChannel messageChannel : channels) {
				if (messageChannel.getBandwidth() > highestBandwidth) {
					highestBandwidth = messageChannel.getBandwidth();
					selected = messageChannel;
				}
			}
			/*
			 * SingleChannelScenario scenario = new SingleChannelScenario(
			 * selected, agents, configuration.getAgentType());
			 */

			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Setting up single channel "
						+ "communication on channel " + selected.toString()
						+ " shared between " + agents + " agents.");
			}

			return new SingleChannelScenario(selected, agents, configuration
					.getAgentType(), configuration.getVoiceChannels());
		}
	}

	@Override
	public boolean isApplicableTo(
			ISimulationCommunicationConfiguration configuration) {
		LOGGER.info("Using fallback scenario.");
		return true;
	}

}
