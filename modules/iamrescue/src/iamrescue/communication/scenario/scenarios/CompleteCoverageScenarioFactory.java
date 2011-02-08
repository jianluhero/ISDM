package iamrescue.communication.scenario.scenarios;

import iamrescue.communication.ISimulationCommunicationConfiguration;
import iamrescue.communication.scenario.ICommunicationScenarioFactory;
import iamrescue.communication.scenario.ICommunicationScenario;

import org.apache.log4j.Logger;

public class CompleteCoverageScenarioFactory implements
		ICommunicationScenarioFactory {

	private static final Logger LOGGER = Logger
			.getLogger(CompleteCoverageScenarioFactory.class);

	@Override
	public ICommunicationScenario create(
			ISimulationCommunicationConfiguration configuration) {
		return new CompleteCoverageScenario(configuration);
	}

	@Override
	public boolean isApplicableTo(
			ISimulationCommunicationConfiguration configuration) {

		// Can everyone listen to every channel?
		int channelCount = configuration.getChannelCount();

		//int maxListenChannelCountCentre = configuration
		//		.getMaxListenChannelCountCentre();
		int maxListenChannelCountPlatoon = configuration
				.getMaxListenChannelCountPlatoon();

		// Need at least two channels (including voice)

		if (channelCount >= 2
				&& maxListenChannelCountPlatoon >= (channelCount - 1)) {
			return true;
		} else {
			return false;
		}
	}
}
