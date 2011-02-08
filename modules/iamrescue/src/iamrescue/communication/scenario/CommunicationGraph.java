package iamrescue.communication.scenario;

import iamrescue.communication.messages.MessageChannel;
import iamrescue.communication.messages.MessageChannelType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.lang.Validate;

import rescuecore2.standard.entities.StandardEntityURN;
import edu.uci.ics.jung.algorithms.flows.EdmondsKarpMaxFlow;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraDistance;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class CommunicationGraph {

	private DirectedSparseGraph<CommunicationGraphVertex, CommunicationGraphEdge> graph = new DirectedSparseGraph<CommunicationGraphVertex, CommunicationGraphEdge>();

	private EnumMap<StandardEntityURN, AgentReceivingVertex> receivingVertices = new EnumMap<StandardEntityURN, AgentReceivingVertex>(
			StandardEntityURN.class);

	private EnumMap<StandardEntityURN, AgentSendingVertex> sendingVertices = new EnumMap<StandardEntityURN, AgentSendingVertex>(
			StandardEntityURN.class);

	private Set<StandardEntityURN> platoons = new HashSet<StandardEntityURN>();
	private Set<StandardEntityURN> centres = new HashSet<StandardEntityURN>();

	private Map<MessageChannel, MessageChannelVertex> channelReceivingVertices = new HashMap<MessageChannel, MessageChannelVertex>();

	private Map<MessageChannel, MessageChannelVertex> channelSendingVertices = new HashMap<MessageChannel, MessageChannelVertex>();

	private Transformer<CommunicationGraphEdge, Number> transformer;

	public Collection<StandardEntityURN> getPlatoons() {
		return platoons;
	}

	public Collection<StandardEntityURN> getCentres() {
		return centres;
	}

	public Collection<MessageChannel> getChannels() {
		return channelReceivingVertices.keySet();
	}

	public void addChannel(MessageChannel channel) {
		Validate.isTrue(channel.getType() == MessageChannelType.RADIO);
		MessageChannelVertex sendingVertex = new MessageChannelVertex(channel);
		MessageChannelVertex receivingVertex = new MessageChannelVertex(channel);

		channelReceivingVertices.put(channel, receivingVertex);
		channelSendingVertices.put(channel, sendingVertex);
		graph.addVertex(sendingVertex);
		graph.addVertex(receivingVertex);
		graph.addEdge(new IntraChannelEdge(channel.getBandwidth()),
				receivingVertex, sendingVertex);
	}

	public void addPlatoon(StandardEntityURN type, int number) {
		addAgentVertices(type, number);
		platoons.add(type);
	}

	public void addCentre(StandardEntityURN type, int number) {
		addAgentVertices(type, number);
		centres.add(type);
	}

	private void addAgentVertices(StandardEntityURN type, int number) {
		AgentSendingVertex sendingVertex = new AgentSendingVertex(type, number);
		AgentReceivingVertex receivingVertex = new AgentReceivingVertex(type,
				number);
		sendingVertices.put(type, sendingVertex);
		receivingVertices.put(type, receivingVertex);
		graph.addVertex(sendingVertex);
		graph.addVertex(receivingVertex);
		graph.addEdge(new IntraAgentEdge(), receivingVertex, sendingVertex);
	}

	// public void addIncomingChannel(StandardEntityURN type,
	// MessageChannel channel) {
	// graph.addEdge(new IncomingMessageChannelAssignment(),
	// getChannelVertex(channel), getAgentReceivingVertex(type));
	// }
	//
	// public void addOutgoingChannel(StandardEntityURN type,
	// MessageChannel channel, int bytes) {
	// graph.addEdge(new OutgoingMessageChannelAssignment(bytes),
	// getAgentSendingVertex(type), getChannelVertex(channel));
	// }

	public int getMaximumDelay() {
		Collection<StandardEntityURN> platoons = getPlatoons();

		DijkstraDistance<CommunicationGraphVertex, CommunicationGraphEdge> distance = new DijkstraDistance<CommunicationGraphVertex, CommunicationGraphEdge>(
				graph, new Transformer<CommunicationGraphEdge, Double>() {
					public Double transform(CommunicationGraphEdge input) {
						if (input instanceof OutgoingMessageChannelAssignment)
							return 1.0;

						return 0.0;
					}
				});

		int maxDistance = 0;

		for (StandardEntityURN platoon1 : platoons) {
			for (StandardEntityURN platoon2 : platoons) {
				Number value = distance.getDistance(
						getAgentSendingVertex(platoon1),
						getAgentReceivingVertex(platoon2));

				if (value == null) {
					System.out.println("Not connected");
					continue;
				}

				maxDistance = Math.max(maxDistance, value.intValue());
			}
		}

		return maxDistance;
	}

	private CommunicationGraphVertex getSendingChannelVertex(
			MessageChannel channel) {
		Validate.isTrue(channelSendingVertices.containsKey(channel));
		return channelSendingVertices.get(channel);
	}

	private CommunicationGraphVertex getReceivingChannelVertex(
			MessageChannel channel) {
		Validate.isTrue(channelReceivingVertices.containsKey(channel));
		return channelReceivingVertices.get(channel);
	}

	private AgentSendingVertex getAgentSendingVertex(StandardEntityURN type) {
		AgentSendingVertex agentSendingVertex = sendingVertices.get(type);
		Validate.notNull(agentSendingVertex);
		return agentSendingVertex;
	}

	private AgentReceivingVertex getAgentReceivingVertex(StandardEntityURN type) {
		AgentReceivingVertex agentReceivingVertex = receivingVertices.get(type);
		Validate.notNull(agentReceivingVertex);
		return agentReceivingVertex;
	}

	public Collection<MessageChannel> getIncomingMessageChannels(
			StandardEntityURN type) {
		AgentVertex agentVertex = getAgentReceivingVertex(type);
		Collection<CommunicationGraphVertex> predecessors = graph
				.getPredecessors(agentVertex);
		return getChannels(predecessors);
	}

	public int getAllocatedBandWidth(MessageChannel channel) {
		MessageChannelVertex messageChannelVertex = channelReceivingVertices
				.get(channel);

		Collection<CommunicationGraphEdge> inEdges = graph
				.getInEdges(messageChannelVertex);

		int allocatedBandwidth = 0;

		for (CommunicationGraphEdge assignment : inEdges) {
			allocatedBandwidth += assignment.getFlow();
		}

		return allocatedBandwidth;
	}

	private Collection<MessageChannel> getChannels(
			Collection<CommunicationGraphVertex> vertices) {
		Collection<MessageChannel> result = new ArrayList<MessageChannel>();
		for (CommunicationGraphVertex predecessor : vertices) {
			MessageChannelVertex vertex = (MessageChannelVertex) predecessor;
			result.add(vertex.getChannel());
		}

		return result;
	}

	public Collection<MessageChannel> getOutgoingMessageChannels(
			StandardEntityURN type) {
		AgentVertex agentVertex = getAgentSendingVertex(type);
		Collection<CommunicationGraphVertex> successors = graph
				.getSuccessors(agentVertex);
		return getChannels(successors);
	}

	public int getIncomingChannelCount(StandardEntityURN type) {
		return graph.getPredecessorCount(getAgentReceivingVertex(type));
	}

	public int getOutgoingChannelCount(StandardEntityURN type) {
		return graph.getSuccessorCount(getAgentSendingVertex(type));
	}

	public int computeMaximumFlow() {
		for (MessageChannelVertex vertex : channelSendingVertices.values()) {
			for (AgentReceivingVertex agentVertex : receivingVertices.values()) {
				graph.addEdge(new IncomingMessageChannelAssignment(), vertex,
						agentVertex);
			}
		}

		for (AgentSendingVertex agentVertex : sendingVertices.values()) {
			for (MessageChannelVertex vertex : channelReceivingVertices
					.values()) {
				graph.addEdge(new OutgoingMessageChannelAssignment(),
						agentVertex, vertex);
			}
		}

		SourceSinkVertex source = new SourceSinkVertex();
		graph.addVertex(source);
		SourceSinkVertex sink = new SourceSinkVertex();
		graph.addVertex(sink);

		for (StandardEntityURN platoon : platoons) {
			graph.addEdge(new SourceSinkEdge(), source,
					getAgentSendingVertex(platoon));
		}

		for (StandardEntityURN platoon : platoons) {
			graph.addEdge(new SourceSinkEdge(),
					getAgentReceivingVertex(platoon), sink);
		}

		HashMap<CommunicationGraphEdge, Number> flows = new HashMap<CommunicationGraphEdge, Number>();
		EdmondsKarpMaxFlow<CommunicationGraphVertex, CommunicationGraphEdge> edmondsKarpMaxFlow = new EdmondsKarpMaxFlow<CommunicationGraphVertex, CommunicationGraphEdge>(
				graph, source, sink, getEdgeCapacityTransformer(), flows,
				new Factory<CommunicationGraphEdge>() {
					@Override
					public CommunicationGraphEdge create() {
						return new IncomingMessageChannelAssignment();
					}
				});

		edmondsKarpMaxFlow.evaluate();

		for (CommunicationGraphEdge edge : flows.keySet()) {
			edge.setAssignedFlow(flows.get(edge).intValue());
		}

		return edmondsKarpMaxFlow.getMaxFlow();
	}

	public int getMaximumFlow(StandardEntityURN source, StandardEntityURN sink) {
		EdmondsKarpMaxFlow<CommunicationGraphVertex, CommunicationGraphEdge> edmondsKarpMaxFlow = new EdmondsKarpMaxFlow<CommunicationGraphVertex, CommunicationGraphEdge>(
				graph, getAgentSendingVertex(source),
				getAgentReceivingVertex(sink), getEdgeCapacityTransformer(),
				new HashMap<CommunicationGraphEdge, Number>(),
				new Factory<CommunicationGraphEdge>() {
					@Override
					public CommunicationGraphEdge create() {
						return new IncomingMessageChannelAssignment();
					}
				});

		edmondsKarpMaxFlow.evaluate();

		return edmondsKarpMaxFlow.getMaxFlow();
	}

	private Transformer<CommunicationGraphEdge, Number> getEdgeCapacityTransformer() {
		if (transformer == null) {
			transformer = new Transformer<CommunicationGraphEdge, Number>() {
				@Override
				public Number transform(CommunicationGraphEdge assignment) {
					return assignment.getMaximumBandwidth();
				}
			};
		}
		return transformer;
	}

	/*public static void main(String[] args) {
		MessageChannel channel1 = MessageChannel.get(10,
				MessageChannelType.RADIO);
		channel1.setBandwidth(40);

		MessageChannel channel2 = MessageChannel.get(2,
				MessageChannelType.RADIO);
		channel2.setBandwidth(30);

		MessageChannel channel3 = MessageChannel.get(3,
				MessageChannelType.RADIO);
		channel3.setBandwidth(100);

		MessageChannel channel4 = MessageChannel.get(5,
				MessageChannelType.RADIO);
		channel4.setBandwidth(60);

		CommunicationGraph communicationGraph = new CommunicationGraph();
		communicationGraph.addChannel(channel1);
		communicationGraph.addChannel(channel2);
		// communicationGraph.addChannel(channel3);
		// communicationGraph.addChannel(channel4);

		communicationGraph.addCentre(StandardEntityURN.AMBULANCE_CENTRE, 5);
		communicationGraph.addPlatoon(StandardEntityURN.FIRE_BRIGADE, 20);
		// communicationGraph.addCentre(StandardEntityURN.FIRE_STATION, 1);

		// // FB <1-4> FS <2-3> AC
		// communicationGraph.addOutgoingChannel(StandardEntityURN.FIRE_BRIGADE,
		// channel1, 53);
		// communicationGraph.addIncomingChannel(StandardEntityURN.FIRE_STATION,
		// channel1);
		//
		// communicationGraph.addOutgoingChannel(StandardEntityURN.FIRE_STATION,
		// channel2, 85);
		// communicationGraph.addIncomingChannel(
		// StandardEntityURN.AMBULANCE_CENTRE, channel2);
		//
		// communicationGraph.addOutgoingChannel(
		// StandardEntityURN.AMBULANCE_CENTRE, channel3, 20);
		// communicationGraph.addIncomingChannel(StandardEntityURN.FIRE_STATION,
		// channel3);
		//
		// communicationGraph.addOutgoingChannel(StandardEntityURN.FIRE_STATION,
		// channel4, 15);
		// communicationGraph.addIncomingChannel(StandardEntityURN.FIRE_BRIGADE,
		// channel4);

		System.out
				.println(communicationGraph
						.getIncomingMessageChannels(StandardEntityURN.AMBULANCE_CENTRE));

		// System.out.println(communicationGraph.getAllocatedBandWidth(channel));

		System.out.println(communicationGraph.getMaximumFlow(
				StandardEntityURN.FIRE_BRIGADE,
				StandardEntityURN.AMBULANCE_CENTRE));

		System.out.println(communicationGraph.getMaximumFlow(
				StandardEntityURN.AMBULANCE_CENTRE,
				StandardEntityURN.FIRE_BRIGADE));

		System.out.println(communicationGraph.computeMaximumFlow());

		System.out.println(communicationGraph.getMaximumDelay());

		CommunicationGraphGUI.show(communicationGraph);
	}

	public static void main1(String[] args) {
		MessageChannel channel1 = MessageChannel.get(10,
				MessageChannelType.RADIO);
		channel1.setBandwidth(30);

		CommunicationGraph communicationGraph = new CommunicationGraph();
		communicationGraph.addChannel(channel1);

		communicationGraph.addPlatoon(StandardEntityURN.AMBULANCE_CENTRE, 5);

		// communicationGraph.addIncomingChannel(
		// StandardEntityURN.AMBULANCE_CENTRE, channel1);
		//
		// communicationGraph.addOutgoingChannel(
		// StandardEntityURN.AMBULANCE_CENTRE, channel1, 20);

		System.out
				.println(communicationGraph
						.getIncomingMessageChannels(StandardEntityURN.AMBULANCE_CENTRE));

		// System.out.println(communicationGraph.getAllocatedBandWidth(channel));

		System.out.println(communicationGraph.getMaximumFlow(
				StandardEntityURN.AMBULANCE_CENTRE,
				StandardEntityURN.AMBULANCE_CENTRE));

		System.out.println(communicationGraph.getMaximumDelay());

		System.out.println(communicationGraph.computeMaximumFlow());

		CommunicationGraphGUI.show(communicationGraph);
	}*/

	public DirectedSparseGraph<CommunicationGraphVertex, CommunicationGraphEdge> getGraph() {
		return graph;
	}

	public void clearAllocations() {
		Collection<CommunicationGraphEdge> edges = graph.getEdges();

		Collection<CommunicationGraphEdge> edgesToRemove = new ArrayList<CommunicationGraphEdge>();

		for (CommunicationGraphEdge edge : edges) {
			if (!(edge instanceof IntraAgentEdge || edge instanceof IntraChannelEdge)) {
				edgesToRemove.add(edge);
			}
		}

		for (CommunicationGraphEdge edge : edgesToRemove) {
			graph.removeEdge(edge);
		}

		Validate.isTrue(graph.getEdgeCount() == receivingVertices.size());
	}

	public int getSenderCount(MessageChannel channel) {
		return graph.getPredecessorCount(getReceivingChannelVertex(channel));
	}

	public int getReceiverCount(MessageChannel channel) {
		return graph.getSuccessorCount(getSendingChannelVertex(channel));
	}
}
