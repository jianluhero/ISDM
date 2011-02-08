package iamrescue.communication.scenario;

import iamrescue.communication.messages.MessageChannel;

import java.util.EnumSet;

import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;


public interface IChannelAllocation {

	MessageChannel getChannel();

	boolean isReceiver(EntityID entityID);

	EnumSet<StandardEntityURN> getSenderTypes();

	StandardEntityURN getReceiverType();

	int getAllocatedBandwidth(EntityID entityID);

	boolean isSender(EntityID entityID);
}
