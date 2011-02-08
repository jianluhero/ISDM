package iamrescue.communication.scenario.scenarios;

import iamrescue.communication.ISimulationCommunicationConfiguration;
import iamrescue.communication.scenario.ICommunicationScenario;
import iamrescue.communication.scenario.ICommunicationScenarioDetector;
import iamrescue.communication.scenario.ICommunicationScenarioFactory;
import iamrescue.communication.scenario.ScenarioInvalidException;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class DefaultCommunicationScenarioDetector implements
		ICommunicationScenarioDetector {

	private static final Logger LOGGER = Logger
			.getLogger(DefaultCommunicationScenarioDetector.class);

	private ISimulationCommunicationConfiguration configuration;
	private ICommunicationScenario scenario;

	private static List<ICommunicationScenarioFactory> scenarios = new ArrayList<ICommunicationScenarioFactory>();

	static {
		scenarios.add(new DefaultIAMCommunicationScenarioFactory());
		scenarios.add(new DefaultCommunicationScenarioFactory());
		scenarios.add(new CompleteCoverageScenarioFactory());
		scenarios.add(new FallbackScenarioFactory());
	}

	public DefaultCommunicationScenarioDetector(
			ISimulationCommunicationConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public ICommunicationScenario getScenario() {
		if (scenario == null) {
			for (ICommunicationScenarioFactory factory : scenarios) {
				if (factory.isApplicableTo(configuration)) {
					try {
						scenario = factory.create(configuration);
						LOGGER.info("Selected scenario " + scenario);
						break;
					} catch (Exception e) {
						LOGGER.error("Could not create scenario: "
								+ e.getMessage());
						e.printStackTrace();
					}
				}
			}

			if (scenario == null) {
				LOGGER.error("No communication scenario found "
						+ "that is applicable to this state of the simulation");
				return new NoCommunicationScenario(
						configuration.getAgentType(), configuration
								.getVoiceChannels());

				/*
				 * throw new IllegalStateException(
				 * "No communication scenario found " +
				 * "that is applicable to this state of the simulation");
				 */
			}
		}

		return scenario;
	}

}
