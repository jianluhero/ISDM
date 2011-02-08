package iamrescue.communication.scenario;

import iamrescue.communication.ISimulationCommunicationConfiguration;
import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.messages.MessageChannelType;
import iamrescue.util.CombinatorialIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.Validate;

import rescuecore2.standard.entities.StandardEntityURN;

public class ChannelAllocationAlgorithm {

	private ISimulationCommunicationConfiguration configuration;

	public ChannelAllocationAlgorithm(
			ISimulationCommunicationConfiguration configuration) {
		this.configuration = configuration;
	}

	public boolean validate(CommunicationGraph graph) {
		Collection<StandardEntityURN> centres = graph.getCentres();

		// check centres are listening to an allowable number of channels
		for (StandardEntityURN type : centres) {
			int incomingChannelCount = graph.getIncomingChannelCount(type);

			if (incomingChannelCount != configuration
					.getMaxListenChannelCountCentre()) {
				System.out.println("centre has wrong number of channels "
						+ incomingChannelCount);
				return false;
			}
		}

		Collection<StandardEntityURN> platoons = graph.getPlatoons();
		// check platoons are listening to an allowable number of channels
		for (StandardEntityURN type : platoons) {
			int incomingChannelCount = graph.getIncomingChannelCount(type);

			if (incomingChannelCount != configuration
					.getMaxListenChannelCountPlatoon()) {
				System.out.println("platoon has too many channels "
						+ incomingChannelCount);
				return false;
			}
		}

		for (MessageChannel channel : graph.getChannels()) {
			if (graph.getSenderCount(channel) > 0) {
				if (graph.getReceiverCount(channel) == 0) {
					System.out.println("Channel " + channel
							+ " has senders but no receivers");
					return false;
				}
			}

			if (graph.getReceiverCount(channel) > 0) {
				if (graph.getSenderCount(channel) == 0) {
					System.out.println("Channel " + channel
							+ " has receivers but no senders");
					return false;
				}
			}

			int allocatedBandWidth = graph.getAllocatedBandWidth(channel);

			if (allocatedBandWidth > channel.getBandwidth()) {
				System.out.println(channel.getBandwidth());
				System.out.println(allocatedBandWidth);
				return false;
			}
		}

		return true;
	}

	public void run() {
		// for each incoming combination
		List<MessageChannel> channels = configuration.getChannels();

		List<StandardEntityURN> centres = new ArrayList<StandardEntityURN>(
				configuration.getCentresByType().keySet());

		List<StandardEntityURN> platoons = new ArrayList<StandardEntityURN>(
				configuration.getPlatoonsByType().keySet());

		List<StandardEntityURN> agents = new ArrayList<StandardEntityURN>();
		agents.addAll(centres);
		agents.addAll(platoons);

		int totalOutgoingEdges = centres.size()
				* configuration.getMaxListenChannelCountCentre()
				+ platoons.size()
				* configuration.getMaxListenChannelCountPlatoon();

		CommunicationGraph graph = new CommunicationGraph();

		for (MessageChannel channel : channels) {
			if (channel.getType() != MessageChannelType.VOICE)
				graph.addChannel(channel);
		}

		for (StandardEntityURN platoon : platoons) {
			graph.addPlatoon(platoon, configuration.getAgentsByType().get(
					platoon).size());
		}

		Validate.isTrue(graph.getPlatoons().containsAll(platoons));
		Validate.isTrue(platoons.containsAll(graph.getPlatoons()));

		for (StandardEntityURN centre : centres) {
			graph.addCentre(centre, configuration.getCentresByType()
					.get(centre).size());
		}

		Validate.isTrue(graph.getCentres().containsAll(centres));
		Validate.isTrue(centres.containsAll(graph.getCentres()));

		CombinatorialIterator<MessageChannel> incoming = new CombinatorialIterator<MessageChannel>(
				channels, totalOutgoingEdges);
		System.out.println(totalOutgoingEdges);

		for (List<MessageChannel> incomingChannels : incoming) {
			graph.clearAllocations();

			System.out.println(incomingChannels.size());

			Iterator<MessageChannel> iterator = incomingChannels.iterator();
			Map<StandardEntityURN, Set<MessageChannel>> assignment = getAssignment(
					iterator, centres, platoons);

			if (assignment == null) {
				System.out.println(incomingChannels);
				System.out.println(assignment);
				continue;
			}

			// for (StandardEntityURN type : assignment.keySet()) {
			// for (MessageChannel channel : assignment.get(type)) {
			// graph.addIncomingChannel(type, channel);
			// }
			// }

			// CommunicationGraphGUI.show(graph);

			Validate.isTrue(!iterator.hasNext());

			if (!validate(graph)) {
				for (StandardEntityURN urn : platoons) {
					System.out.println(graph.getIncomingChannelCount(urn));
				}

				for (StandardEntityURN urn : centres) {
					System.out.println(graph.getIncomingChannelCount(urn));
				}

				CommunicationGraphGUI.show(graph);
			}

			Validate.isTrue(validate(graph));
		}

		// greedily maximise minimum flow
	}

	private Map<StandardEntityURN, Set<MessageChannel>> getAssignment(
			Iterator<MessageChannel> iterator, List<StandardEntityURN> centres,
			List<StandardEntityURN> platoons) {
		Map<StandardEntityURN, Set<MessageChannel>> result = new HashMap<StandardEntityURN, Set<MessageChannel>>();

		for (int i = 0; i < centres.size(); i++) {
			result.put(centres.get(i), new HashSet<MessageChannel>());
			for (int j = 0; j < configuration.getMaxListenChannelCountCentre(); j++) {
				result.get(centres.get(i)).add(iterator.next());
			}

			if (result.get(centres.get(i)).size() != configuration
					.getMaxListenChannelCountCentre())
				return null;
		}

		for (int i = 0; i < platoons.size(); i++) {
			result.put(platoons.get(i), new HashSet<MessageChannel>());
			for (int j = 0; j < configuration.getMaxListenChannelCountPlatoon(); j++) {
				result.get(platoons.get(i)).add(iterator.next());
			}

			if (result.get(platoons.get(i)).size() != configuration
					.getMaxListenChannelCountPlatoon())
				return null;
		}

		return result;
	}

	public static void main(String[] args) {
		// ISimulationCommunicationConfiguration configuration = new
		// TestCommunicationScenarioDetector()
		// .createConfiguration(StandardEntityURN.AMBULANCE_CENTRE);
		//
		// ChannelAllocationAlgorithm channelAllocationAlgorithm = new
		// ChannelAllocationAlgorithm(
		// configuration);
		//
		// channelAllocationAlgorithm.run();
	}
}
