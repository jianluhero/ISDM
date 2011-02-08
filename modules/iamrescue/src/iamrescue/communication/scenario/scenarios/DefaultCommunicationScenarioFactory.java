package iamrescue.communication.scenario.scenarios;

import iamrescue.communication.ISimulationCommunicationConfiguration;
import iamrescue.communication.scenario.ICommunicationScenarioFactory;
import iamrescue.communication.scenario.ICommunicationScenario;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;


public class DefaultCommunicationScenarioFactory implements
		ICommunicationScenarioFactory {

	private static Log log = LogFactory
			.getLog(DefaultCommunicationScenarioFactory.class);

	@Override
	public ICommunicationScenario create(
			ISimulationCommunicationConfiguration configuration) {
		return new DefaultCommunicationScenario(configuration);
	}

	@Override
	public boolean isApplicableTo(
			ISimulationCommunicationConfiguration configuration) {
		// this scenario is applicable to a simulation with at least 9 channels,
		// and at least one agent of every type

		if (configuration.getRadioChannelCount() < 9) {
			log.error("DefaultCommunicationScenario not applicable: "
					+ "need at least 9 radio channels");
			return false;
		}

		if (configuration.getMaxListenChannelCountPlatoon() < 1) {
			log
					.error("DefaultCommunicationScenario not applicable: "
							+ "platoons should be able to listen to at least one channel");
			return false;
		}

		if (configuration.getMaxListenChannelCountCentre() < 2) {
			log
					.error("DefaultCommunicationScenario not applicable: "
							+ "centres should be able to listen to at least two channels");
			return false;
		}

		Map<StandardEntityURN, Collection<StandardEntity>> agentsByType = configuration
				.getAgentsByType();

		boolean acceptable = true;

		acceptable &= agentsByType.get(StandardEntityURN.AMBULANCE_CENTRE)
				.size() >= 1;
		acceptable &= agentsByType.get(StandardEntityURN.AMBULANCE_TEAM).size() >= 1;
		acceptable &= agentsByType.get(StandardEntityURN.FIRE_BRIGADE).size() >= 1;
		acceptable &= agentsByType.get(StandardEntityURN.FIRE_STATION).size() >= 1;
		acceptable &= agentsByType.get(StandardEntityURN.POLICE_FORCE).size() >= 1;
		acceptable &= agentsByType.get(StandardEntityURN.POLICE_OFFICE).size() >= 1;

		if (!acceptable) {
			log.info("DefaultCommunicationScenario not applicable: simulation "
					+ "does not have one agent of every type");
		}

		return acceptable;
	}
}
