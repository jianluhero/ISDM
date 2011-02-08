package iamrescue.communication.scenario.scenarios;

import iamrescue.communication.ISimulationCommunicationConfiguration;
import iamrescue.communication.scenario.ICommunicationScenarioFactory;
import iamrescue.communication.scenario.ICommunicationScenario;

import org.apache.log4j.Logger;

public class ManyLowBandwidthScenarioFactory implements
		ICommunicationScenarioFactory {

	private static final Logger LOGGER = Logger
			.getLogger(ManyLowBandwidthScenarioFactory.class);

	@Override
	public ICommunicationScenario create(
			ISimulationCommunicationConfiguration configuration) {
		return new ManyLowBandwidthScenario(configuration);
	}

	@Override
	public boolean isApplicableTo(
			ISimulationCommunicationConfiguration configuration) {

		if (configuration.getRadioChannels().size() > 3
				&& configuration.getMaxListenChannelCountCentre() > 2) {
			return true;
		} else {
			return false;
		}
	}
}
