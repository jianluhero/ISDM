package iamrescue.communication.scenario;


/**
 * Detects the type of communication scenario is applicable to the state of the
 * world.
 * 
 * @author rs06r
 * 
 */
public interface ICommunicationScenarioDetector {

	ICommunicationScenario getScenario();

}
