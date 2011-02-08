package iamrescue.communication.messages.updates;

import iamrescue.communication.messages.MessagePrefixes;
import iamrescue.communication.messages.codec.IMessageCodec;
import iamrescue.communication.messages.codec.updates.EntityUpdatedMessageCodec;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

/**
 * Used to inform that a building's properties have changed.
 * 
 * @author rs06r
 */
public class BuildingUpdatedMessage extends EntityUpdatedMessage {

	public static final List<String> relevantProperties = new ArrayList<String>();

	private static final Logger LOGGER = Logger
			.getLogger(BuildingUpdatedMessage.class);

	static {
		//relevantProperties.add(StandardPropertyURN.BROKENNESS.toString());
		relevantProperties.add(StandardPropertyURN.FIERYNESS.toString());
		//relevantProperties.add(StandardPropertyURN.BLOCKADES.toString());
		//relevantProperties.add(StandardPropertyURN.TEMPERATURE.toString());
		//relevantProperties.add(StandardPropertyURN.IGNITION.toString());
	}

	@Override
	public List<String> getRelevantProperties() {
		return relevantProperties;
	}

	public BuildingUpdatedMessage(short timestamp) {
		super(timestamp);
	}

	@Override
	public IMessageCodec getCodec() {
		return new EntityUpdatedMessageCodec(relevantProperties) {

			@Override
			public byte getMessagePrefix() {
				return MessagePrefixes.BUILDING_UPDATE_PREFIX;
			}

			@Override
			protected Class<? extends Entity> getObjectClass() {
				return Building.class;
			}

			@Override
			protected Entity createObject(EntityID id) {
				LOGGER.error("Should not have to create new buildings.");
				return Registry.getCurrentRegistry().createEntity(
						StandardEntityURN.BUILDING.toString(), id);
			}

			@Override
			protected EntityUpdatedMessage createMessage(short timeStamp) {
				return new BuildingUpdatedMessage(timeStamp);
			}
		};
	}

	@Override
	protected EntityUpdatedMessage createMessage(short timestamp) {
		return new BuildingUpdatedMessage(timestamp);
	}

	@Override
	protected boolean isCorrectObjectClass(Entity object) {
		return object instanceof Building;
	}

	@Override
	public String getMessageName() {
		return "BuildingUpdatedMessage";
	}
}
