/**
 * 
 */
package iamrescue.belief.commupdates;

import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.provenance.AgentCommunicationOrigin;
import iamrescue.belief.provenance.IPropertyMerger;
import iamrescue.belief.provenance.ProvenanceLogEntry;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.updates.BlockadeUpdatedMessage;
import iamrescue.communication.messages.updates.EntityUpdatedMessage;
import iamrescue.communication.messages.updates.RoadUpdatedMessage;

import java.util.List;

import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.properties.EntityRefListProperty;
import rescuecore2.worldmodel.properties.EntityRefProperty;

/**
 * @author Sebastian
 * 
 */
public class BlockConsistencyHandler implements IMessageHandler {

	private IAMWorldModel worldModel;
	private IPropertyMerger propertyMerger;

	/**
	 * @param worldModel
	 * @param propertyMerger
	 */
	public BlockConsistencyHandler(IAMWorldModel worldModel,
			IPropertyMerger propertyMerger) {
		this.worldModel = worldModel;
		this.propertyMerger = propertyMerger;
	}

	@Override
	public boolean canHandle(Message message) {
		return (message instanceof RoadUpdatedMessage || message instanceof BlockadeUpdatedMessage);
	}

	@Override
	public boolean handleMessage(Message message) {
		boolean updated = false;
		EntityUpdatedMessage update = (EntityUpdatedMessage) message;
		if (update instanceof RoadUpdatedMessage) {
			if (update.getChangedProperties().contains(
					StandardPropertyURN.BLOCKADES.toString())) {
				// Do we know all of them?
				Road road = (Road) worldModel.getEntity(update.getObjectID());
				EntityRefListProperty property = road.getBlockadesProperty();
				if (property.isDefined()) {
					List<EntityID> blockades = property.getValue();
					for (EntityID blockID : blockades) {
						Blockade block = (Blockade) worldModel
								.getEntity(blockID);
						if (block == null) {
							// Need to create new block.
							block = (Blockade) Registry.getCurrentRegistry()
									.createEntity(
											StandardEntityURN.BLOCKADE
													.toString(), blockID);
							worldModel.addEntity(block);
							updated = true;
						}
						if (!block.isPositionDefined()) {
							EntityRefProperty positionProperty = new EntityRefProperty(
									StandardPropertyURN.POSITION.toString());
							positionProperty.setValue(update.getObjectID());
							ProvenanceLogEntry entry = new ProvenanceLogEntry(
									update.getTimestamp(),
									AgentCommunicationOrigin.get(update
											.getChannel()), positionProperty
											.copy());

							worldModel.storeProvenance(blockID, entry);
							block.setPosition(update.getObjectID());
							updated = true;
						}

					}
				}

			}
		}
		return updated;
	}
}
