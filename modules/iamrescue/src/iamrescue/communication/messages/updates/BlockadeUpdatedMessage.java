package iamrescue.communication.messages.updates;

import iamrescue.belief.entities.RoutingInfoBlockade;
import iamrescue.communication.messages.MessagePrefixes;
import iamrescue.communication.messages.codec.IMessageCodec;
import iamrescue.communication.messages.codec.updates.EntityUpdatedMessageCodec;

import java.util.ArrayList;
import java.util.List;

import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

/**
 * Used to inform that an agent's position has been changed.
 * 
 * @author rs06r
 */
public class BlockadeUpdatedMessage extends EntityUpdatedMessage {

	public static final List<String> relevantProperties = new ArrayList<String>();

	static {
		// relevantProperties.add(StandardPropertyURN.APEXES.toString());
		relevantProperties.add(StandardPropertyURN.POSITION.toString());
		//relevantProperties.add(StandardPropertyURN.X.toString());
		//relevantProperties.add(StandardPropertyURN.Y.toString());
		relevantProperties.add(RoutingInfoBlockade.BLOCK_INFO_URN);
		relevantProperties.add(StandardPropertyURN.REPAIR_COST.toString());
	}

	// private WorldModelConverter converter;

	@Override
	public List<String> getRelevantProperties() {
		return relevantProperties;
	}

	public BlockadeUpdatedMessage(short timestamp) {// , WorldModelConverter
		// coverter) {
		super(timestamp);
		// this.converter = coverter;
	}

	// @Override
	// public boolean providesOwnCodec(String propertyURN) {
	// if (propertyURN.equals(RoutingInfoBlockade.BLOCK_INFO_URN)) {
	// return true;
	// } else {
	// return false;
	// }
	// }

	// @Override
	// public PropertyCodec getOwnCodec(String propertyURN) {
	// if (propertyURN.equals(RoutingInfoBlockade.BLOCK_INFO_URN)) {
	// return new BlockedNeighboursCodec(converter);
	// } else {
	// return super.getOwnCodec(propertyURN);
	// }
	// }

	@Override
	public IMessageCodec getCodec() {
		// final WorldModelConverter finalConverter = converter;
		return new EntityUpdatedMessageCodec(relevantProperties) {

			@Override
			public byte getMessagePrefix() {
				return MessagePrefixes.BLOCKADE_UPDATE_PREFIX;
			}

			@Override
			protected Class<? extends Entity> getObjectClass() {
				return Blockade.class;
			}

			@Override
			protected Entity createObject(EntityID id) {
				return Registry.getCurrentRegistry().createEntity(
						StandardEntityURN.BLOCKADE.toString(), id);
			}

			@Override
			protected EntityUpdatedMessage createMessage(short timeStamp) {
				return new BlockadeUpdatedMessage(timeStamp);// ,
				// finalConverter);
			}
		};
	}

	@Override
	protected EntityUpdatedMessage createMessage(short timestamp) {
		return new BlockadeUpdatedMessage(timestamp);// , converter);
	}

	@Override
	protected boolean isCorrectObjectClass(Entity object) {
		return object instanceof Blockade;
	}

	@Override
	public String getMessageName() {
		return "BlockadeUpdatedMessage";
	}
}
