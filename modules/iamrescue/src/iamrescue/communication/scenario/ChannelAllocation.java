package iamrescue.communication.scenario;

import iamrescue.communication.ISimulationCommunicationConfiguration;
import iamrescue.communication.messages.MessageChannel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.Validate;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;


public class ChannelAllocation implements IChannelAllocation {

	private Collection<StandardEntity> senders;
	private Collection<StandardEntity> receivers;
	private MessageChannel channel;
	private Set<EntityID> receiverIDs;
	private Set<EntityID> senderIDs;
	private StandardEntityURN receiverType;
	private EnumSet<StandardEntityURN> senderTypes;

	public ChannelAllocation(StandardEntityURN senderType,
			StandardEntityURN receiverType,
			ISimulationCommunicationConfiguration configuration,
			int radioChannelNumber) {
		this(EnumSet.of(senderType), receiverType, configuration,
				radioChannelNumber);
	}

	public ChannelAllocation(EnumSet<StandardEntityURN> senderTypes,
			StandardEntityURN receiverType,
			ISimulationCommunicationConfiguration configuration,
			int radioChannelNumber) {
		this.senderTypes = senderTypes;
		this.receiverType = receiverType;

		channel = configuration.getRadioChannel(radioChannelNumber);

		Validate.notNull(channel);
		Validate.notNull(channel.getBandwidth());

		senders = new ArrayList<StandardEntity>();

		for (StandardEntityURN senderType : senderTypes) {
			senders.addAll(configuration.getAgentsByType().get(senderType));
		}

		receivers = configuration.getAgentsByType().get(receiverType);

		receiverIDs = new HashSet<EntityID>();
		for (StandardEntity receiver : receivers) {
			receiverIDs.add(receiver.getID());
		}

		senderIDs = new HashSet<EntityID>();
		for (StandardEntity sender : senders) {
			senderIDs.add(sender.getID());
		}
	}

	@Override
	public MessageChannel getChannel() {
		return channel;
	}

	@Override
	public boolean isReceiver(EntityID entityID) {
		return receiverIDs.contains(entityID);
	}

	@Override
	public boolean isSender(EntityID entityID) {
		return senderIDs.contains(entityID);
	}

	@Override
	public EnumSet<StandardEntityURN> getSenderTypes() {
		return senderTypes;
	}

	@Override
	public StandardEntityURN getReceiverType() {
		return receiverType;
	}

	@Override
	public int getAllocatedBandwidth(EntityID entityID) {
		if (isSender(entityID)) {
			return channel.getBandwidth() / senders.size();
		}

		return 0;
	}
}
