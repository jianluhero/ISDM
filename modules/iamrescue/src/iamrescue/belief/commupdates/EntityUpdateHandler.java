/**
 * 
 */
package iamrescue.belief.commupdates;

import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.provenance.AgentCommunicationOrigin;
import iamrescue.belief.provenance.IPropertyMerger;
import iamrescue.belief.provenance.ProvenanceLogEntry;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.updates.EntityUpdatedMessage;

import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.EntityRefListProperty;

/**
 * @author Sebastian
 * 
 */
public class EntityUpdateHandler implements IMessageHandler {

	private static final Logger LOGGER = Logger
			.getLogger(EntityUpdateHandler.class);
	private IAMWorldModel worldModel;
	private IPropertyMerger propertyMerger;

	// private ISpatialIndex spatial;

	public EntityUpdateHandler(IAMWorldModel worldModel,
			IPropertyMerger propertyMerger) {// , ISpatialIndex spatial) {
		this.worldModel = worldModel;
		this.propertyMerger = propertyMerger;
		// this.spatial = spatial;
	}

	@Override
	public boolean canHandle(Message message) {
		return (message instanceof EntityUpdatedMessage);
	}

	@Override
	public boolean handleMessage(Message message) {
		boolean updated = false;
		EntityUpdatedMessage updateMsg = (EntityUpdatedMessage) message;

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Updating world model with " + updateMsg);
		}

		StandardEntity existingEntity = worldModel.getEntity(updateMsg
				.getObjectID());

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Existing entity: " + updateMsg);
		}

		if (existingEntity == null) {
			// Did I know about this in the past?
			Collection<String> provenanceProperties = worldModel
					.getProvenanceProperties(updateMsg.getObjectID());
			if (provenanceProperties.size() > 0) {
				LOGGER.debug("Ignoring message about an object "
						+ "that was previously deleted from belief base: "
						+ updateMsg);
				updateMsg.markAsRead();
				return updated;
			}

			// Create entity
			worldModel.addEntity(updateMsg.getObject());
			existingEntity = worldModel.getEntity(updateMsg.getObjectID());
		}

		/*
		 * Integer x = null; Integer y = null; boolean positionIncluded = false;
		 */

		for (Property p : updateMsg.getProperties()) {
			// update provenance
			ProvenanceLogEntry entry = new ProvenanceLogEntry(updateMsg
					.getTimestamp(), AgentCommunicationOrigin.get(updateMsg
					.getChannel()), p.copy());

			worldModel.storeProvenance(updateMsg.getObjectID(), entry);

			// update current value in belief base
			Property decidedValue = propertyMerger.decideValue(worldModel
					.getProvenance(updateMsg.getObjectID(), p.getURN()));

			Property existingProperty = existingEntity.getProperty(p.getURN());

			// Check if blockades need to be removed:
			if (existingProperty.getURN().equals(StandardPropertyURN.BLOCKADES)) {
				if (existingProperty.isDefined() && decidedValue.isDefined()) {
					EntityRefListProperty blocks = (EntityRefListProperty) existingProperty;
					EntityRefListProperty blocksNew = (EntityRefListProperty) decidedValue;
					for (EntityID id : blocks.getValue()) {
						if (!(blocksNew.getValue().contains(id))) {
							worldModel.removeEntity(id);
						}
					}
				}
			}

			if (existingProperty.isDefined()) {
				if (!decidedValue.isDefined()) {
					updated = true;
				} else {
					// Both defined. Check if they are not equal.
					if (!IAMWorldModel.checkIfPropertiesEqual(existingProperty,
							decidedValue)) {
						updated = true;
					}
				}
			} else {
				if (decidedValue.isDefined()) {
					updated = true;
				}
			}

			existingProperty.takeValue(decidedValue);

			/*
			 * if (existingEntity instanceof Human && p.isDefined()) { if
			 * (p.getURN().equals(StandardPropertyURN.X.toString())) { x =
			 * (Integer) p.getValue(); } else if
			 * (p.getURN().equals(StandardPropertyURN.Y.toString())) { y =
			 * (Integer) p.getValue(); } else if (p.getURN().equals(
			 * StandardPropertyURN.POSITION.toString())) { positionIncluded =
			 * true; } }
			 */
		}

		// If x and y were communicated but not position, try to infer this
		/*
		 * if (existingEntity instanceof Human && !positionIncluded) { if (x !=
		 * null && y != null) { Collection<StandardEntity> query = spatial
		 * .query(SpatialQueryFactory.queryWithinDistance( new
		 * PositionXY(x.intValue(), y.intValue()), 0, Building.class)); if
		 * (query.size() == 0) {
		 * LOGGER.warn("Could not infer location of entity " +
		 * existingEntity.getFullDescription()); } else if (query.size() > 1) {
		 * StandardEntity first = query.iterator().next(); LOGGER.warn("Entity "
		 * + existingEntity.getFullDescription() +
		 * " has multiple potential locations. " + "Choosing first: " +
		 * first.getFullDescription()); Property existingPosition =
		 * existingEntity.getProperty(StandardPropertyURN.POSITION .toString());
		 * 
		 * } } }
		 */

		updateMsg.markAsRead();

		return updated;
	}
}