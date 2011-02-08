/**
 * 
 */
package iamrescue.belief.commupdates;

import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.provenance.AgentCommunicationOrigin;
import iamrescue.belief.provenance.IPropertyMerger;
import iamrescue.belief.provenance.ProvenanceLogEntry;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.updates.EntityDeletedMessage;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.EntityRefListProperty;

/**
 * @author Sebastian
 * 
 */
public class EntityDeletedHandler implements IMessageHandler {

	private static final Logger LOGGER = Logger
			.getLogger(EntityDeletedHandler.class);
	private IAMWorldModel worldModel;
	private IPropertyMerger propertyMerger;

	public EntityDeletedHandler(IAMWorldModel worldModel,
			IPropertyMerger propertyMerger) {
		this.worldModel = worldModel;
		this.propertyMerger = propertyMerger;
	}

	@Override
	public boolean canHandle(Message message) {
		return (message instanceof EntityDeletedMessage);
	}

	@Override
	public boolean handleMessage(Message message) {
		EntityDeletedMessage updateMsg = (EntityDeletedMessage) message;

		message.markAsRead();

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Updating world model with " + updateMsg);
		}

		StandardEntity existingEntity = worldModel.getEntity(updateMsg.getId());

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Existing entity: " + updateMsg);
		}

		if (existingEntity != null) {

			if (existingEntity instanceof Blockade) {
				// Need to unlink blockades property
				Blockade blockade = (Blockade) existingEntity;
				if (blockade.isPositionDefined()) {
					EntityID position = blockade.getPosition();
					Entity entity = worldModel.getEntity(position);
					Area area = ((Area) entity);
					EntityRefListProperty currentBlockades = area
							.getBlockadesProperty();

					EntityRefListProperty newBlockades = currentBlockades
							.copy();

					if (newBlockades.isDefined()) {
						// Remove block
						List<EntityID> value = newBlockades.getValue();
						List<EntityID> arrayList = new ArrayList<EntityID>(
								value);
						arrayList.remove(blockade.getID());
						newBlockades.setValue(arrayList);
					}

					ProvenanceLogEntry entry = new ProvenanceLogEntry(updateMsg
							.getTimeStamp(), AgentCommunicationOrigin
							.get(updateMsg.getChannel()), newBlockades);

					worldModel.storeProvenance(updateMsg.getId(), entry);

					// update current value in belief base
					Property decidedValue = propertyMerger
							.decideValue(worldModel.getProvenance(updateMsg
									.getId(), newBlockades.getURN()));

					Property existingProperty = area.getBlockadesProperty();
					existingProperty.takeValue(decidedValue);
				}
			}

			// Now remove entity
			worldModel.removeEntity(existingEntity);
			return true;
		} else {
			return false;
		}
	}
}
