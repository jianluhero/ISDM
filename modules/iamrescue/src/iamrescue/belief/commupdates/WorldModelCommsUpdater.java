/**
 * 
 */
package iamrescue.belief.commupdates;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.provenance.IPropertyMerger;
import iamrescue.belief.provenance.LatestProperty;
import iamrescue.communication.ICommunicationModule;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.config.Config;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class WorldModelCommsUpdater implements IWorldModelCommsUpdater {

	private ICommunicationModule commsModule;
	private IAMWorldModel worldModel;
	private List<IMessageHandler> msgListeners;
	private ISimulationTimer timer;
	private IPropertyMerger propertyMerger = new LatestProperty();
	private Set<MessageChannel> myTeamChannels = new FastSet<MessageChannel>();
	private Config config;
	private boolean hasCentreRole = false;
	private EntityID myID;

	// After how many time steps do we remove messages that haven't been read?
	private static final int TIME_OUT = 10;

	private static final Logger LOGGER = Logger
			.getLogger(WorldModelCommsUpdater.class);

	public WorldModelCommsUpdater(IAMWorldModel worldModel,
			ICommunicationModule commsModule, ISimulationTimer timer,
			Config config, EntityID myID) {
		this.worldModel = worldModel;
		this.config = config;
		this.commsModule = commsModule;
		this.msgListeners = new FastList<IMessageHandler>();
		this.timer = timer;
		this.myID = myID;
		registerDefaultHandlers();
	}

	/**
	 * 
	 */
	private void registerDefaultHandlers() {
		addUpdateHandler(new EntityUpdateHandler(worldModel, propertyMerger));
		addUpdateHandler(new EntityDeletedHandler(worldModel, propertyMerger));
		addUpdateHandler(new BlockConsistencyHandler(worldModel, propertyMerger));
		addUpdateHandler(new CivilianCryForHelpHandler(worldModel, config, myID));
		addUpdateHandler(worldModel.getStuckMemory());
	}

	public void setCentreRole(List<MessageChannel> myTeamChannels) {
		this.myTeamChannels.clear();
		this.myTeamChannels.addAll(myTeamChannels);
		hasCentreRole = true;
	}

	public void unsetCentreRole() {
		this.myTeamChannels.clear();
		this.hasCentreRole = false;
	}

	public List<Message> update() {
		List<Message> updateMessages = new ArrayList<Message>();
		int iterations = hasCentreRole ? 2 : 1;
		do {
			Collection<Message> messages = commsModule.getUnreadMessages();
			for (Message message : messages) {
				if (iterations == 2
						&& !myTeamChannels.contains(message.getChannel())) {
					// If two iterations and still on first iteration, ignore
					// channels other than own
					continue;
				}
				for (IMessageHandler handler : msgListeners) {
					if (handler.canHandle(message)) {
						if (handler.handleMessage(message)) {
							if (iterations == 1 && hasCentreRole) {
								// Only store updates on last iteration
								updateMessages.add(message);
							}
						}
					}
				}
				if (iterations == 1
						&& message.getTimestepReceived() + TIME_OUT <= timer
								.getTime()) {
					message.markAsRead();
				}
			}
			iterations--;
		} while (iterations > 0);
		// Forward all unread messages that were not from own channels
		/*
		 * if (hasCentreRole) { Collection<Message> unreadMessages =
		 * commsModule.getUnreadMessages(); for (Message message :
		 * unreadMessages) { if (!myTeamChannels.contains(message.getChannel()))
		 * { updateMessages.add(message); } } }
		 */
		return updateMessages;
	}

	public void addUpdateHandler(IMessageHandler handler) {
		msgListeners.add(handler);
	}
}
