package iamrescue.communication.scenario;

import iamrescue.communication.ISimulationCommunicationConfiguration;

public interface ICommunicationScenarioFactory {

	boolean isApplicableTo(ISimulationCommunicationConfiguration configuration);

	ICommunicationScenario create(
			ISimulationCommunicationConfiguration configuration)
			throws ScenarioInvalidException;

}
