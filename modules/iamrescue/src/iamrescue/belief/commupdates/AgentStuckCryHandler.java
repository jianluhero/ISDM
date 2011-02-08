/**
 * 
 */
package iamrescue.belief.commupdates;

import iamrescue.belief.IAMWorldModel;
import iamrescue.communication.messages.CivilianCryForHelpMessage;
import iamrescue.communication.messages.Message;

import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;

import rescuecore2.config.Config;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Simon
 * 
 */
public class AgentStuckCryHandler implements IMessageHandler {

	private static final String DISTANCE_KEY = "comms.channels.0.range";
	private static final Logger LOGGER = Logger
			.getLogger(AgentStuckCryHandler.class);
	private IAMWorldModel worldModel;
	private EntityID myID;

	private int range;

	// private ISpatialIndex spatial;

	public AgentStuckCryHandler(IAMWorldModel worldModel, Config config,
			EntityID myID) {
		this.worldModel = worldModel;
		range = config.getIntValue(DISTANCE_KEY, 30000);
		this.myID = myID;
	}

	@Override
	public boolean canHandle(Message message) {
		return (message instanceof CivilianCryForHelpMessage);
	}

	@Override
	public boolean handleMessage(Message message) {
		CivilianCryForHelpMessage civMsg = (CivilianCryForHelpMessage) message;

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Updating search list with " + civMsg);
		}

		EntityID civ = civMsg.getSenderAgentID();
		// first check if we know about this civilian already
		Collection<StandardEntity> civs = worldModel
				.getEntitiesOfType(StandardEntityURN.CIVILIAN);
		for (Iterator<StandardEntity> it = civs.iterator(); it.hasNext();) {
			if (it.next().getID().getValue() == civ.getValue()) {
				civMsg.markAsRead();
				return true;
			}
		}

		// is a new civilian so need to add a set of high priority buildings to
		// search
		Collection<StandardEntity> buildings = worldModel.getObjectsInRange(
				myID, range);
		worldModel.updateHighPriorityBuildings(buildings);

		civMsg.markAsRead();

		return true;
	}
}