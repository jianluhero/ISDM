package iamrescue.communication.messages.updates;

import iamrescue.belief.entities.BlockInfoRoad;
import iamrescue.communication.messages.MessagePrefixes;
import iamrescue.communication.messages.codec.IMessageCodec;
import iamrescue.communication.messages.codec.updates.EntityUpdatedMessageCodec;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

/**
 * Used to inform that a road's properties have changed.
 * 
 * @author rs06r
 */
public class RoadUpdatedMessage extends EntityUpdatedMessage {

	public static final List<String> relevantProperties = new ArrayList<String>();

	public static final Logger LOGGER = Logger
			.getLogger(RoadUpdatedMessage.class);

	static {
		relevantProperties.add(StandardPropertyURN.BLOCKADES.toString());
		relevantProperties.add(BlockInfoRoad.HAS_BEEN_PASSED_URN);
	}

	@Override
	public List<String> getRelevantProperties() {
		return relevantProperties;
	}

	public RoadUpdatedMessage(short timestamp) {
		super(timestamp);
	}

	@Override
	public IMessageCodec getCodec() {
		return new EntityUpdatedMessageCodec(relevantProperties) {

			@Override
			public byte getMessagePrefix() {
				return MessagePrefixes.ROAD_UPDATE_PREFIX;
			}

			@Override
			protected Class<? extends Entity> getObjectClass() {
				return Road.class;
			}

			@Override
			protected Entity createObject(EntityID id) {
				LOGGER.error("Should not have to create new roads.");
				return Registry.getCurrentRegistry().createEntity(
						StandardEntityURN.ROAD.toString(), id);
			}

			@Override
			protected EntityUpdatedMessage createMessage(short timeStamp) {
				return new RoadUpdatedMessage(timeStamp);
			}
		};
	}

	@Override
	protected EntityUpdatedMessage createMessage(short timestamp) {
		return new RoadUpdatedMessage(timestamp);
	}

	@Override
	protected boolean isCorrectObjectClass(Entity object) {
		return object instanceof Road;
	}

	@Override
	public String getMessageName() {
		return "RoadUpdatedMessage";
	}
}
