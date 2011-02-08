package iamrescue.communication.messages.updates;

import java.util.ArrayList;
import java.util.List;

import rescuecore2.standard.entities.StandardPropertyURN;

/**
 * Used to inform that an agent's position has been changed.
 * 
 * @author rs06r
 */
public abstract class AbstractHumanUpdatedMessage extends EntityUpdatedMessage {

	public static final List<String> relevantProperties = new ArrayList<String>();

	static {
		relevantProperties.add(StandardPropertyURN.POSITION.toString());
		relevantProperties.add(StandardPropertyURN.BURIEDNESS.toString());
		// relevantProperties.add(StandardPropertyURN.DAMAGE.toString());
		relevantProperties.add(StandardPropertyURN.HP.toString());
		// relevantProperties.add(StandardPropertyURN.STAMINA);
		relevantProperties.add(StandardPropertyURN.X.toString());
		relevantProperties.add(StandardPropertyURN.Y.toString());
	}

	@Override
	public List<String> getRelevantProperties() {
		return relevantProperties;
	}

	public AbstractHumanUpdatedMessage(short timestamp) {
		super(timestamp);
	}
}
