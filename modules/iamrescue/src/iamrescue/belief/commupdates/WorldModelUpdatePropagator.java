/**
 * 
 */
package iamrescue.belief.commupdates;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.communication.messages.updates.AmbulanceTeamUpdatedMessage;
import iamrescue.communication.messages.updates.BlockadeUpdatedMessage;
import iamrescue.communication.messages.updates.BuildingUpdatedMessage;
import iamrescue.communication.messages.updates.CivilianUpdatedMessage;
import iamrescue.communication.messages.updates.EntityDeletedMessage;
import iamrescue.communication.messages.updates.EntityUpdatedMessage;
import iamrescue.communication.messages.updates.FireBrigadeUpdatedMessage;
import iamrescue.communication.messages.updates.PoliceForceUpdatedMessage;
import iamrescue.communication.messages.updates.RoadUpdatedMessage;
import iamrescue.communication.scenario.IAMCommunicationModule;
import iamrescue.routing.WorldModelConverter;

import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

/**
 * 
 * Standard implementation of IWorldModelUpdatePropagator.
 * 
 * @author Sebastian
 * 
 */
public class WorldModelUpdatePropagator implements IWorldModelUpdatePropagator {

	private IAMWorldModel worldModel;
	private IAMCommunicationModule commModule;
	private final Map<StandardEntityURN, IUpdateMessageFactory> factoryMap = new FastMap<StandardEntityURN, IUpdateMessageFactory>();;
	private ISimulationTimer timer;
	private WorldModelConverter converter;

	private static final Logger LOGGER = Logger
			.getLogger(WorldModelUpdatePropagator.class);

	/**
	 * Creates a new propagator. This sends information about perceived changes
	 * to the world to other agents.
	 * 
	 * @param worldModel
	 *            The world model to use.
	 * @param commModule
	 *            The comms module to send updates to.
	 * @param timer
	 *            The simulation timer.
	 */
	public WorldModelUpdatePropagator(IAMWorldModel worldModel,
			IAMCommunicationModule commModule, ISimulationTimer timer,
			WorldModelConverter converter) {
		this.worldModel = worldModel;
		this.commModule = commModule;
		this.timer = timer;
		this.converter = converter;
		initialise();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * belief.commupdates.IWorldModelUpdatePropagator#sendUpdates(rescuecore2
	 * .worldmodel.ChangeSet)
	 */
	@Override
	public void sendUpdates(ChangeSet changed, EntityID myID) {
		Set<EntityID> changedEntities = changed.getChangedEntities();
		for (EntityID entityID : changedEntities) {
			Set<Property> properties = changed.getChangedProperties(entityID);
			StandardEntity entity = worldModel.getEntity(entityID);
			if (entity == null) {
				LOGGER.warn("I am trying to send an update "
						+ "about an entity I don't know about! ID " + entityID);
			} else {
				// First check it's a civilian or myself - assume the others
				// will communicate their own info
				if (entity instanceof Human) {
					if (!(entity instanceof Civilian)) {
						if (!(entity.getID().equals(myID))) {
							// Ignore
							continue;
						}
					}
				}

				IUpdateMessageFactory factory = factoryMap.get(entity
						.getStandardURN());
				if (factory != null) {
					EntityUpdatedMessage message = factory.createUpdateMessage(
							entity, properties);
					if (message.getChangedProperties().size() > 0) {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Changed entity: " + entity
									+ ", properties: " + properties
									+ ", message: " + message);
						}
						if (timer.getTime() < 4) {
							// Increase TTL for early messages, since we want
							// those to be propagated, and there might be
							// congestion.
							message.setTTL(10);
						} else {
							message.setTTL(5);
						}
						commModule.enqueueRadioMessageToOwnTeam(message);
					} else {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Changed entity: " + entity
									+ ", properties: " + properties
									+ ", but not interested "
									+ "in changed property.");
						}
					}
				} else {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("No factory found for " + entity
								+ ", properties: " + properties);
					}
				}
			}
		}

		// Also get deleted entities
		Set<EntityID> deletedEntities = changed.getDeletedEntities();
		for (EntityID entityID : deletedEntities) {
			EntityDeletedMessage deletedMsg = new EntityDeletedMessage(
					entityID, (short) timer.getTime());
			commModule.enqueueRadioMessageToOwnTeam(deletedMsg);
		}
	}

	/**
	 * Sets up message factories.
	 */
	private void initialise() {
		IUpdateMessageFactory civilianFactory = new AbstractUpdateMessageFactory() {

			@Override
			protected EntityUpdatedMessage createMessage() {
				return new CivilianUpdatedMessage((short) timer.getTime());
			}
		};

		IUpdateMessageFactory policeFactory = new AbstractUpdateMessageFactory() {

			@Override
			protected EntityUpdatedMessage createMessage() {
				return new PoliceForceUpdatedMessage((short) timer.getTime());
			}
		};

		IUpdateMessageFactory fireFactory = new AbstractUpdateMessageFactory() {

			@Override
			protected EntityUpdatedMessage createMessage() {
				return new FireBrigadeUpdatedMessage((short) timer.getTime());
			}
		};

		IUpdateMessageFactory ambulanceFactory = new AbstractUpdateMessageFactory() {

			@Override
			protected EntityUpdatedMessage createMessage() {
				return new AmbulanceTeamUpdatedMessage((short) timer.getTime());
			}
		};

		IUpdateMessageFactory buildingFactory = new AbstractUpdateMessageFactory() {

			@Override
			protected EntityUpdatedMessage createMessage() {
				return new BuildingUpdatedMessage((short) timer.getTime());
			}
		};

		IUpdateMessageFactory roadFactory = new AbstractUpdateMessageFactory() {

			@Override
			protected EntityUpdatedMessage createMessage() {
				return new RoadUpdatedMessage((short) timer.getTime());
			}
		};

		IUpdateMessageFactory blockadeFactory = new AbstractUpdateMessageFactory() {

			@Override
			protected EntityUpdatedMessage createMessage() {
				return new BlockadeUpdatedMessage((short) timer.getTime());// ,
				// converter);
			}
		};

		factoryMap.put(StandardEntityURN.AMBULANCE_TEAM, ambulanceFactory);
		factoryMap.put(StandardEntityURN.POLICE_FORCE, policeFactory);
		factoryMap.put(StandardEntityURN.FIRE_BRIGADE, fireFactory);
		factoryMap.put(StandardEntityURN.CIVILIAN, civilianFactory);
		factoryMap.put(StandardEntityURN.BUILDING, buildingFactory);
		factoryMap.put(StandardEntityURN.ROAD, roadFactory);
		factoryMap.put(StandardEntityURN.BLOCKADE, blockadeFactory);
	}
}
