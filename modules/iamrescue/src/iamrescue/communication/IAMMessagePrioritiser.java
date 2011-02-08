/**
 * 
 */
package iamrescue.communication;

import iamrescue.agent.ambulanceteam.ambulancetools.AllocationMessage;
import iamrescue.belief.entities.BlockInfoRoad;
import iamrescue.belief.entities.RoutingInfoBlockade;
import iamrescue.communication.messages.AgentStuckMessage;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessagePriority;
import iamrescue.communication.messages.PingMessage;
import iamrescue.communication.messages.updates.AbstractHumanUpdatedMessage;
import iamrescue.communication.messages.updates.BlockadeUpdatedMessage;
import iamrescue.communication.messages.updates.BuildingUpdatedMessage;
import iamrescue.communication.messages.updates.CivilianUpdatedMessage;
import iamrescue.communication.messages.updates.EntityDeletedMessage;
import iamrescue.communication.messages.updates.EntityUpdatedMessage;
import iamrescue.communication.messages.updates.RoadUpdatedMessage;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;
import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class IAMMessagePrioritiser {

	private static final Logger LOGGER = Logger
			.getLogger(IAMMessagePrioritiser.class);

	private static final int LONG_TTL = 20;
	private static final int MEDIUM_TTL = 10;
	private static final int SHORT_TTL = 5;
	private static final int IMMEDIATE_TTL = 1;

	// Up to how many do we consider few burning buildings?
	private static final int FEW_BUILDING_THRESHOLD = 20;

	// CRITICAL

	// Agent stuck
	private static final MessagePriority STUCK_PRIORITY = MessagePriority.CRITICAL;
	private static final int STUCK_MESSAGE_TTL = LONG_TTL;

	// New civilian
	private static final MessagePriority NEW_CIVILIAN_PRIORITY = MessagePriority.CRITICAL;
	private static final int NEW_CIVILIAN_TTL = LONG_TTL;

	// New burning building (when there are few)
	private static final MessagePriority NEW_FEW_BURNING_BUILDING_PRIORITY = MessagePriority.CRITICAL;
	private static final int NEW_FEW_BURNING_BUILDING_TTL = LONG_TTL;

	// VERY_HIGH

	// Blocked neighbours decreased / block deleted
	private static final MessagePriority BLOCK_REMOVED_PRIORITY = MessagePriority.VERY_HIGH;
	private static final int BLOCK_REMOVED_TTL = LONG_TTL;

	// Buried platoon property update
	private static final MessagePriority BURIED_PLATOON_PRIORITY = MessagePriority.VERY_HIGH;
	private static final int BURIED_PLATOON_TTL = MEDIUM_TTL;

	// New building on fire (already many on fire)
	private static final MessagePriority NEW_MANY_BURNING_BUILDING_PRIORITY = MessagePriority.VERY_HIGH;
	private static final int NEW_MANY_BURNING_BUILDING_TTL = LONG_TTL;

	// Building extinguished
	private static final MessagePriority EXTINGUISHED_BUILDING_PRIORITY = MessagePriority.VERY_HIGH;
	private static final int EXTINGUISHED_BUILDING_TTL = LONG_TTL;

	// HIGH

	// Ambulance assignment
	private static final MessagePriority ALLOCATION_PRIORITY = MessagePriority.HIGH;
	private static final int ALLOCATION_MESSAGE_TTL = MEDIUM_TTL;

	// Buried civilian updated
	private static final MessagePriority BURIED_HUMAN_PRIORITY = MessagePriority.HIGH;
	private static final int BURIED_HUMAN_TTL = SHORT_TTL;

	// Burning building property change
	private static final MessagePriority BURNING_BUILDING_PRIORITY = MessagePriority.HIGH;
	private static final int BURNING_BUILDING_TTL = SHORT_TTL;

	// NORMAL

	// Non-buried platoon update
	private static final MessagePriority NORMAL_PLATOON_PRIORITY = MessagePriority.NORMAL;
	private static final int NORMAL_PLATOON_TTL = IMMEDIATE_TTL;

	// LOW

	// Block increased
	private static final MessagePriority BLOCK_INFO_MORE_OR_SAME_PRIORITY = MessagePriority.LOW;
	private static final int BLOCK_INFO_MORE_OR_SAME_TTL = MEDIUM_TTL;

	// Non-buried human update
	private static final MessagePriority NORMAL_HUMAN_PRIORITY = MessagePriority.LOW;
	private static final int NORMAL_HUMAN_TTL = SHORT_TTL;

	// Non-burning building update
	private static final MessagePriority NORMAL_BUILDING_PRIORITY = MessagePriority.LOW;
	private static final int NORMAL_BUILDING_TTL = SHORT_TTL;

	// Non-block related road update
	private static final MessagePriority GENERAL_ROAD_PRIORITY = MessagePriority.LOW;
	private static final int GENERAL_ROAD_TTL = MEDIUM_TTL;

	// VERY_LOW

	// Ping Message
	private static final MessagePriority PING_MESSAGE_PRIORITY = MessagePriority.VERY_LOW;
	private static final int PING_MESSAGE_TTL = IMMEDIATE_TTL;

	private static final MessagePriority NORMAL_BLOCK_PRIORITY = MessagePriority.VERY_LOW;
	private static final int NORMAL_BLOCK_TTL = SHORT_TTL;

	private Set<EntityID> knownCivilians = new FastSet<EntityID>();
	private Set<EntityID> burningBuildings = new FastSet<EntityID>();
	private Map<EntityID, Integer> previousBlockedLength = new FastMap<EntityID, Integer>();
	private Map<EntityID, Integer> previousRoadBlocksLength = new FastMap<EntityID, Integer>();

	public void updateMessagePriority(Message message) {
		// Need to handle:
		// 1. EntityDeletedMessages
		// 2. PingMessages
		// 3. AllocationMessages
		// 4. AgentStuckMessages
		// 5. EntityUpdatedMessages

		if (message instanceof EntityUpdatedMessage) {
			updateEntityUpdateMessagePriority((EntityUpdatedMessage) message);
		} else if (message instanceof EntityDeletedMessage) {
			message.setPriority(BLOCK_REMOVED_PRIORITY);
			message.setTTL(BLOCK_REMOVED_TTL);
		} else if (message instanceof PingMessage) {
			message.setPriority(PING_MESSAGE_PRIORITY);
			message.setTTL(PING_MESSAGE_TTL);
		} else if (message instanceof AllocationMessage) {
			message.setPriority(ALLOCATION_PRIORITY);
			message.setTTL(ALLOCATION_MESSAGE_TTL);
		} else if (message instanceof AgentStuckMessage) {
			message.setPriority(STUCK_PRIORITY);
			message.setTTL(STUCK_MESSAGE_TTL);
		} else {
			LOGGER.error("Could not handle message: " + message);
		}
	}

	private void updateEntityUpdateMessagePriority(EntityUpdatedMessage message) {
		if (message instanceof AbstractHumanUpdatedMessage) {
			// Human
			handleHumanUpdatedMessage((AbstractHumanUpdatedMessage) message);
		} else if (message instanceof BuildingUpdatedMessage) {
			// Updated building
			handleBuildingUpdatedMessage((BuildingUpdatedMessage) message);
		} else if (message instanceof BlockadeUpdatedMessage) {
			handleBlockadeUpdatedMessage((BlockadeUpdatedMessage) message);
		} else if (message instanceof RoadUpdatedMessage) {
			handleRoadUpdatedMessage((RoadUpdatedMessage) message);
		} else {
			LOGGER.error("Could not handle message: " + message);
		}
	}

	private void handleRoadUpdatedMessage(RoadUpdatedMessage message) {
		BlockInfoRoad road = (BlockInfoRoad) message.getObject();
		if (message.getChangedProperties().contains(
				StandardPropertyURN.BLOCKADES.toString())
				&& road.isBlockadesDefined()) {
			// Block related
			List<EntityID> blockades = road.getBlockades();
			Integer previousLength = previousRoadBlocksLength.get(road.getID());
			previousRoadBlocksLength.put(road.getID(), blockades.size());
			if (previousLength == null || blockades.size() > previousLength) {
				// Worse
				message.setPriority(BLOCK_INFO_MORE_OR_SAME_PRIORITY);
				message.setTTL(BLOCK_INFO_MORE_OR_SAME_TTL);
			} else {
				// Cleared
				message.setPriority(BLOCK_REMOVED_PRIORITY);
				message.setTTL(BLOCK_REMOVED_TTL);
			}
		} else {
			// General road update
			message.setPriority(GENERAL_ROAD_PRIORITY);
			message.setTTL(GENERAL_ROAD_TTL);
		}
	}

	private void handleBlockadeUpdatedMessage(BlockadeUpdatedMessage message) {
		RoutingInfoBlockade blockade = (RoutingInfoBlockade) message
				.getObject();
		if (message.getChangedProperties().contains(
				RoutingInfoBlockade.BLOCK_INFO_URN)
				&& blockade.isBlockedEdgesDefined()) {
			int lengthNow = blockade.getBlockedEdges().length;
			previousBlockedLength.put(blockade.getID(), lengthNow);
			Integer previousLength = previousBlockedLength
					.get(blockade.getID());
			if (previousLength == null || lengthNow >= previousLength) {
				// Didn't know about this, same or increased
				message.setPriority(BLOCK_INFO_MORE_OR_SAME_PRIORITY);
				message.setTTL(BLOCK_INFO_MORE_OR_SAME_TTL);
			} else {
				// Length is smaller. Block has been cleared!
				message.setPriority(BLOCK_REMOVED_PRIORITY);
				message.setTTL(BLOCK_REMOVED_TTL);
			}
		} else {
			// General block update. Most likely repair cost.
			message.setPriority(NORMAL_BLOCK_PRIORITY);
			message.setTTL(NORMAL_BLOCK_TTL);
		}
	}

	private void handleBuildingUpdatedMessage(BuildingUpdatedMessage message) {
		// Is this on fire?
		Building building = (Building) message.getObject();
		if (building.isFierynessDefined() && building.getFieryness() >= 1
				&& building.getFieryness() <= 3) {
			// Yes, on fire
			// Do we know this?
			if (burningBuildings.contains(building.getID())) {
				// Yes
				message.setPriority(BURNING_BUILDING_PRIORITY);
				message.setTTL(BURNING_BUILDING_TTL);
			} else {
				// No, not yet
				burningBuildings.add(building.getID());
				if (burningBuildings.size() < FEW_BUILDING_THRESHOLD) {
					message.setPriority(NEW_FEW_BURNING_BUILDING_PRIORITY);
					message.setTTL(NEW_FEW_BURNING_BUILDING_TTL);
				} else {
					message.setPriority(NEW_MANY_BURNING_BUILDING_PRIORITY);
					message.setTTL(NEW_MANY_BURNING_BUILDING_TTL);

				}
			}
		} else {
			// Not burning
			// Was it burning previously?
			if (burningBuildings.contains(building.getID())) {
				// Yes, it has been extinguished
				burningBuildings.remove(building.getID());
				message.setPriority(EXTINGUISHED_BUILDING_PRIORITY);
				message.setTTL(EXTINGUISHED_BUILDING_TTL);
			} else {
				// Just a normal update
				message.setPriority(NORMAL_BUILDING_PRIORITY);
				message.setTTL(NORMAL_BUILDING_TTL);
			}
		}

	}

	private void handleHumanUpdatedMessage(AbstractHumanUpdatedMessage message) {
		if (message instanceof CivilianUpdatedMessage) {
			// Civilian
			// Is it new?
			if (!knownCivilians.contains(message.getObjectID())) {
				// Remember
				knownCivilians.add(message.getObjectID());
				message.setPriority(NEW_CIVILIAN_PRIORITY);
				message.setTTL(NEW_CIVILIAN_TTL);
			} else {
				handleKnownHumanMessage(message, false);
			}
		} else {
			// Platoon agent
			handleKnownHumanMessage(message, true);
		}
	}

	private void handleKnownHumanMessage(AbstractHumanUpdatedMessage message,
			boolean isPlatoon) {
		// Already known
		// Has buriedness changed or is it buried?
		Set<String> changedProperties = message.getChangedProperties();
		Human human = (Human) message.getObject();
		if (changedProperties.contains(StandardPropertyURN.BURIEDNESS
				.toString())
				|| (human.isBuriednessDefined() && human.getBuriedness() > 0 && changedProperties
						.contains(StandardPropertyURN.HP.toString()))) {
			if (isPlatoon) {
				message.setPriority(BURIED_PLATOON_PRIORITY);
				message.setTTL(BURIED_PLATOON_TTL);
			} else {
				message.setPriority(BURIED_HUMAN_PRIORITY);
				message.setTTL(BURIED_HUMAN_TTL);
			}
		} else {
			// Just a normal position update?
			if (isPlatoon) {
				message.setPriority(NORMAL_PLATOON_PRIORITY);
				message.setTTL(NORMAL_PLATOON_TTL);
			} else {
				message.setPriority(NORMAL_HUMAN_PRIORITY);
				message.setTTL(NORMAL_HUMAN_TTL);
			}
		}
	}
}
