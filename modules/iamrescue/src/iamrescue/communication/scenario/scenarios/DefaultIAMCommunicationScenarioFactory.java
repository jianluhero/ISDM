package iamrescue.communication.scenario.scenarios;

import iamrescue.communication.ISimulationCommunicationConfiguration;
import iamrescue.communication.scenario.ICommunicationScenario;
import iamrescue.communication.scenario.ICommunicationScenarioFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultIAMCommunicationScenarioFactory implements
		ICommunicationScenarioFactory {

	private static Log log = LogFactory
			.getLog(DefaultIAMCommunicationScenarioFactory.class);

	@Override
	public ICommunicationScenario create(
			ISimulationCommunicationConfiguration configuration) {
		return new DefaultIAMCommunicationsScenario(configuration);
	}

	@Override
	public boolean isApplicableTo(
			ISimulationCommunicationConfiguration configuration) {
		return true;
	}
}
