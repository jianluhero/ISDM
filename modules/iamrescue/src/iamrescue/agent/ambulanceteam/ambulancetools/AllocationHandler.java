package iamrescue.agent.ambulanceteam.ambulancetools;

import iamrescue.agent.ambulanceteam.IAMAmbulanceTeam;
import iamrescue.belief.commupdates.IMessageHandler;
import iamrescue.communication.messages.Message;

import java.util.Set;

import javolution.util.FastSet;

import org.apache.log4j.Logger;

public class AllocationHandler implements IMessageHandler {
	private Set<AllocationMessage> heard = new FastSet<AllocationMessage>();
	private static final Logger LOGGER = Logger
			.getLogger(IAMAmbulanceTeam.class);
	int owner;
	RScheduler scheduler;

	public AllocationHandler(int owner, RScheduler scheduler) {// ,
		// ISpatialIndex
		// spatial) {
		this.owner = owner;
		this.scheduler = scheduler;
		// this.spatial = spatial;
	}

	@Override
	public boolean canHandle(Message message) {
		return (message instanceof AllocationMessage);
	}

	@Override
	public boolean handleMessage(Message message) {
		AllocationMessage allocationMessage = (AllocationMessage) message;

		int timestamp = allocationMessage.getTime();
		// count += AladdinInterAgentConstants.ALLOCATION_SIZE;

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(" : Received Allocation Update!!!!!! of timeStamp: "
					+ timestamp + "at "
					+ allocationMessage.getTimestepReceived());
		}
		int[] victimsID = scheduler.decodeStrategy(allocationMessage.getTask());

		int[][] nextAllocations = scheduler.nextAllocations;
		int agentIndex = scheduler.getMyTeamIndex(allocationMessage
				.getSenderAgentID().getValue());

		for (int i = 0; i < victimsID.length; i++) {
			nextAllocations[agentIndex][i] = victimsID[i];
		}

		allocationMessage.markAsRead();

		if (heard.contains(allocationMessage)) {
			return false;
		} else {
			heard.add(allocationMessage);
			return true;
		}

	}

}
