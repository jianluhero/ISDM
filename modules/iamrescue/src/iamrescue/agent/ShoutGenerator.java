package iamrescue.agent;

import iamrescue.belief.IAMWorldModel;
import iamrescue.communication.messages.updates.BuildingUpdatedMessage;
import iamrescue.communication.messages.updates.CivilianUpdatedMessage;
import iamrescue.communication.messages.updates.EntityUpdatedMessage;
import iamrescue.communication.scenario.IAMCommunicationModule;
import iamrescue.execution.RC2ExecutionService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.EntityListener;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.WorldModel;
import rescuecore2.worldmodel.WorldModelListener;

public class ShoutGenerator implements EntityListener,
		WorldModelListener<StandardEntity> {

	private IAMWorldModel worldModel;
	private IAMCommunicationModule communicationModule;

	private static final int MAX_MESSAGES_TO_SEND = 50;

	private Map<EntityID, EntityUpdatedMessage> civilianMessages = new FastMap<EntityID, EntityUpdatedMessage>();
	private Map<EntityID, EntityUpdatedMessage> buildingMessages = new FastMap<EntityID, EntityUpdatedMessage>();
	private Set<EntityID> refuges = new FastSet<EntityID>();

	private Set<Entity> updated = new FastSet<Entity>();
	private List<EntityUpdatedMessage> lastList = new ArrayList<EntityUpdatedMessage>();

	private Random random = new Random(System.currentTimeMillis());
	private RC2ExecutionService executionService;
	
	//private static final double SHOUT_PROBABILITY = 0.5;

	public ShoutGenerator(IAMWorldModel worldModel,
			IAMCommunicationModule communicationModule){
			//RC2ExecutionService executionService) {
		this.worldModel = worldModel;
		this.communicationModule = communicationModule;
		Collection<StandardEntity> refugeColl = worldModel
				.getEntitiesOfType(StandardEntityURN.REFUGE);
		for (StandardEntity standardEntity : refugeColl) {
			refuges.add(standardEntity.getID());
		}
		Collection<StandardEntity> buildings = worldModel
				.getEntitiesOfType(StandardEntityURN.BUILDING,
						StandardEntityURN.REFUGE,
						StandardEntityURN.AMBULANCE_CENTRE,
						StandardEntityURN.FIRE_STATION,
						StandardEntityURN.POLICE_OFFICE);
		for (StandardEntity standardEntity : buildings) {
			standardEntity.addEntityListener(this);
		}
		worldModel.addWorldModelListener(this);
		//this.executionService = executionService;
	}

	public void generateShouts() {
		// Shout for stuck
	//	if (executionService.getConsecutiveRandomSteps()
		
		
		for (Entity e : updated) {
			if (e instanceof Civilian) {
				Civilian c = (Civilian) e;
				if (c.isPositionDefined()) {
					// Only communicate if we know position
					if (refuges.contains(c.getPosition())) {
						civilianMessages.remove(c.getID());
					} else {
						CivilianUpdatedMessage msg = new CivilianUpdatedMessage(
								(short) worldModel
										.getProvenance(
												c.getID(),
												StandardPropertyURN.POSITION
														.toString())
										.getLatest().getTimeStep());
						msg.setObject(c);
						msg.addUpdatedProperty(c.getBuriednessProperty());
						msg.addUpdatedProperty(c.getXProperty());
						msg.addUpdatedProperty(c.getYProperty());
						msg.addUpdatedProperty(c.getPositionProperty());
						msg.addUpdatedProperty(c.getHPProperty());
						civilianMessages.put(c.getID(), msg);
					}
				}
			} else if (e instanceof Building) {
				Building b = (Building) e;
				if (b.isFierynessDefined()) {
					// Only care about buildings on fire
					if (b.getFieryness() >= 1 && b.getFieryness() <= 3) {
						BuildingUpdatedMessage msg = new BuildingUpdatedMessage(
								(short) worldModel.getProvenance(
										b.getID(),
										StandardPropertyURN.FIERYNESS
												.toString()).getLatest()
										.getTimeStep());
						msg.setObject(b);
						msg.addUpdatedProperty(b.getFierynessProperty());
						buildingMessages.put(b.getID(), msg);
					} else {
						buildingMessages.remove(b.getID());
					}
				}
			}
		} // End of updates

		List<EntityUpdatedMessage> allMessages;

		if (updated.size() > 0) {

			updated.clear();
			// Send messages
			allMessages = new ArrayList<EntityUpdatedMessage>(civilianMessages
					.size()
					+ buildingMessages.size());

			allMessages.addAll(civilianMessages.values());
			allMessages.addAll(buildingMessages.values());

			lastList = allMessages;

		} else {
			allMessages = lastList;
		}

		Collections.shuffle(allMessages, random);

		for (int i = 0; i < allMessages.size() && i < MAX_MESSAGES_TO_SEND; i++) {
			EntityUpdatedMessage message = allMessages.get(i);
			message.setTTL(1);
			communicationModule.enqueueVocalMessage(message);
		}
	}

	@Override
	public void propertyChanged(Entity e, Property p, Object oldValue,
			Object newValue) {
		updated.add(e);
	}

	@Override
	public void entityAdded(WorldModel<? extends StandardEntity> model,
			StandardEntity e) {
		if (e instanceof Civilian) {
			e.addEntityListener(this);
			updated.add(e);
		}
	}

	@Override
	public void entityRemoved(WorldModel<? extends StandardEntity> model,
			StandardEntity e) {
		// TODO Auto-generated method stub

	}
}
