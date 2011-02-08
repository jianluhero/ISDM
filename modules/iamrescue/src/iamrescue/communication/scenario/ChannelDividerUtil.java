package iamrescue.communication.scenario;

import iamrescue.communication.messages.MessageChannel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javolution.util.FastMap;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public class ChannelDividerUtil {
	public static Map<MessageChannel, List<EntityID>> divideAgents(
			List<MessageChannel> channels, List<EntityID> platoons,
			List<EntityID> centres, int relativeCentreWeight) {

		if (channels.size() == 0) {
			return new FastMap<MessageChannel, List<EntityID>>();
		}

		int[] centreAllocations = new int[centres.size()];
		int[] platoonAllocations = new int[platoons.size()];
		int[] totalAssigned = new int[channels.size()];
		int[] bandwidths = new int[channels.size()];
		for (int i = 0; i < channels.size(); i++) {
			totalAssigned[i] = 0;
			bandwidths[i] = channels.get(i).getEffectiveBandwidth();
		}
		// Assign centres first
		for (int i = 0; i < centres.size(); i++) {
			int best = -1;
			int bestIndex = -1;
			for (int j = 0; j < channels.size(); j++) {
				int newBandwidth = bandwidths[j]
						/ (totalAssigned[j] + relativeCentreWeight);
				if (newBandwidth > best) {
					best = newBandwidth;
					bestIndex = j;
				}
			}
			centreAllocations[i] = bestIndex;
			totalAssigned[bestIndex] += relativeCentreWeight;
		}

		// Now other agents
		for (int i = 0; i < platoons.size(); i++) {
			int best = -1;
			int bestIndex = -1;
			for (int j = 0; j < channels.size(); j++) {
				int newBandwidth = bandwidths[j] / (totalAssigned[j] + 1);
				if (newBandwidth > best) {
					best = newBandwidth;
					bestIndex = j;
				}
			}
			platoonAllocations[i] = bestIndex;
			totalAssigned[bestIndex]++;
		}

		// Build result
		Map<MessageChannel, List<EntityID>> result = new FastMap<MessageChannel, List<EntityID>>();
		for (int i = 0; i < channels.size(); i++) {
			result.put(channels.get(i), new ArrayList<EntityID>());
		}
		for (int i = 0; i < centres.size(); i++) {
			result.get(channels.get(centreAllocations[i])).add(centres.get(i));
		}
		for (int i = 0; i < platoons.size(); i++) {
			result.get(channels.get(platoonAllocations[i]))
					.add(platoons.get(i));
		}
		return result;
	}

	public static List<StandardEntity> flattenMap(
			Map<StandardEntityURN, Collection<StandardEntity>> map) {
		List<StandardEntity> list = new ArrayList<StandardEntity>();
		for (Entry<StandardEntityURN, Collection<StandardEntity>> entry : map
				.entrySet()) {
			list.addAll(entry.getValue());
		}
		return list;
	}

	public static List<EntityID> convertToIDs(List<StandardEntity> entities) {
		List<EntityID> list = new ArrayList<EntityID>(entities.size());
		for (StandardEntity entity : entities) {
			list.add(entity.getID());
		}
		return list;
	}

	public static int findMinimumBandwidth(List<List<MessageChannel>> allocation) {
		if (allocation.size() == 0) {
			return 0;
		}

		int minimum = Integer.MAX_VALUE;

		for (List<MessageChannel> list : allocation) {
			int bandwidth = computeTotalBandwidth(list);
			if (bandwidth < minimum) {
				minimum = bandwidth;
			}
		}

		return minimum;
	}

	public static int computeTotalBandwidth(List<MessageChannel> channels) {
		int bandwidth = 0;
		for (MessageChannel messageChannel : channels) {
			bandwidth += messageChannel.getEffectiveBandwidth();
		}
		return bandwidth;
	}
}
