/**
 * 
 */
package iamrescue.belief;

import iamrescue.belief.commupdates.IMessageHandler;
import iamrescue.communication.messages.AgentStuckMessage;
import iamrescue.communication.messages.Message;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.EntityListener;
import rescuecore2.worldmodel.Property;

/**
 * @author Sebastian
 * 
 */
public class StuckMemory implements IMessageHandler, EntityListener {

	private Map<EntityID, StuckInfo> entityStuckMap = new FastMap<EntityID, StuckInfo>();
	private Map<EntityID, Set<EntityID>> positionStuckMap = new FastMap<EntityID, Set<EntityID>>();

	@Override
	public boolean canHandle(Message message) {
		return message instanceof AgentStuckMessage;
	}

	public void enableAutoUpdate(IAMWorldModel worldModel) {
		Collection<StandardEntity> policeForces = worldModel
				.getEntitiesOfType(StandardEntityURN.POLICE_FORCE);
		for (StandardEntity police : policeForces) {
			police.addEntityListener(this);
		}
	}

	@Override
	public boolean handleMessage(Message message) {
		message.markAsRead();
		AgentStuckMessage stuckMessage = (AgentStuckMessage) message;
		EntityID stuck = stuckMessage.getStuckAgent();
		EntityID area = stuckMessage.getArea();
		int timeStep = message.getTimestepReceived();
		StuckInfo stuckInfo = entityStuckMap.get(stuck);
		boolean alreadyExisted = false;
		if (stuckInfo != null) {
			alreadyExisted = true;
			if (stuckInfo.position.equals(area)) {
				// No need to do anything
				return false;
			} else {
				Set<EntityID> stuckAgents = positionStuckMap
						.get(stuckInfo.position);
				if (stuckAgents.size() == 1) {
					positionStuckMap.remove(stuckInfo.position);
				} else {
					stuckAgents.remove(stuck);
				}
			}
		}
		// Add info
		stuckInfo = new StuckInfo(stuck, area, timeStep);
		entityStuckMap.put(stuck, stuckInfo);
		Set<EntityID> alreadyStuck = positionStuckMap.get(area);
		if (alreadyStuck == null) {
			alreadyStuck = new FastSet<EntityID>();
			positionStuckMap.put(area, alreadyStuck);
		}
		alreadyStuck.add(stuck);
		if (alreadyExisted) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void propertyChanged(Entity e, Property p, Object oldValue,
			Object newValue) {
		if (e instanceof PoliceForce) {
			if (p.isDefined()
					&& p.getURN().equals(StandardPropertyURN.POSITION)) {
				EntityID id = (EntityID) p.getValue();
				Set<EntityID> stuck = positionStuckMap.get(id);
				if (stuck != null) {
					for (EntityID entityID : stuck) {
						entityStuckMap.remove(entityID);
					}
					positionStuckMap.remove(id);
				}
			}
		}
	}

	public Set<EntityID> getStuckAgents() {
		return entityStuckMap.keySet();
	}

	public StuckInfo getStuckInfo(EntityID stuckAgentID) {
		return entityStuckMap.get(stuckAgentID);
	}

	public static class StuckInfo {
		private EntityID id;
		private EntityID position;
		private int timeStepFirstReceived;

		public StuckInfo(EntityID id, EntityID position,
				int timeStepFirstReceived) {
			super();
			this.id = id;
			this.position = position;
			this.timeStepFirstReceived = timeStepFirstReceived;
		}

		public EntityID getID() {
			return id;
		}

		public EntityID getPosition() {
			return position;
		}

		public int getTimeStepFirstReceived() {
			return timeStepFirstReceived;
		}
	}
}
