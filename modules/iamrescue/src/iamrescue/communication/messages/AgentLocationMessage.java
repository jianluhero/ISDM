package iamrescue.communication.messages;

import iamrescue.communication.messages.codec.IMessageCodec;
import iamrescue.communication.messages.updates.EntityUpdatedMessage;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;

/**
 * Used to inform that an agent's position has been changed.
 * 
 * @author rs06r
 */
public class AgentLocationMessage extends EntityUpdatedMessage {

	public static final List<String> relevantProperties = new ArrayList<String>();

	static {
		relevantProperties.add(StandardPropertyURN.POSITION.toString());
	}

	@Override
	public List<String> getRelevantProperties() {
		return relevantProperties;
	}

	public AgentLocationMessage(short timestamp) {
		super(timestamp);
	}

	@Override
	public IMessageCodec getCodec() {
		throw new NotImplementedException();
	}

	@Override
	protected EntityUpdatedMessage createMessage(short timestamp) {
		return new AgentLocationMessage(timestamp);
	}

	@Override
	protected boolean isCorrectObjectClass(Entity object) {
		return object instanceof Human;
	}

	@Override
	public String getMessageName() {
		return "AgentLocationMessage";
	}
}
