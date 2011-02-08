package iamrescue.communication;

import iamrescue.agent.ISimulationTimer;
import iamrescue.communication.failuredetection.SentMessageMemory;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.MessageChannel;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

public class ScheduledOutgoingMessageService extends AOutgoingMessageSelector {

	private IMessagingSchedule scheduler;
	private ISimulationTimer timer;

	private static final Logger LOGGER = Logger
			.getLogger(ScheduledOutgoingMessageService.class);

	public ScheduledOutgoingMessageService(ISimulationTimer timer,
			IOutgoingMessageService outgoingMessageService, IEncoder encoder,
			IMessagingSchedule scheduler, SentMessageMemory memory) {
		super(outgoingMessageService, encoder, memory);
		this.timer = timer;
		this.scheduler = scheduler;
	}

	@Override
	public void sendMessages(Map<MessageChannel, List<Message>> radioMessageQs) {

		for (Entry<MessageChannel, List<Message>> entry : radioMessageQs
				.entrySet()) {
			int allocatedMessageCount = scheduler.getAllocatedMessagesCount(
					entry.getKey(), timer.getTime());

			int allocatedMessageSize = scheduler.getAllocatedMessagesSize(entry
					.getKey(), timer.getTime());

			int allocatedTotalBandwidth = scheduler.getAllocatedTotalBandwidth(
					entry.getKey(), timer.getTime());

			int allocatedMaximumRepetitions = scheduler.getMaximumRepetitions(
					entry.getKey(), timer.getTime());

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Allocated message count:" + allocatedMessageCount
						+ ", message size: " + allocatedMessageSize
						+ ", bandwidth: " + allocatedTotalBandwidth
						+ ", on channel: " + entry.getKey() + ". Messages: "
						+ entry.getValue());
			}

			super.sendMessages(entry.getValue(), entry.getKey(),
					allocatedMessageCount, allocatedMessageSize,
					allocatedTotalBandwidth, allocatedMaximumRepetitions, timer
							.getTime());
		}
	}
}
