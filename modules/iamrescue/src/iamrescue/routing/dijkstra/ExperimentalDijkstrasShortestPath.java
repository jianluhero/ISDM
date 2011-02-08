package iamrescue.routing.dijkstra;

import iamrescue.routing.IRoutingAlgorithm;
import iamrescue.routing.dijkstra.SimpleGraph.Node;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javolution.util.FastSet;

import org.jgrapht.util.FibonacciHeap;
import org.jgrapht.util.FibonacciHeapNode;

public class ExperimentalDijkstrasShortestPath implements IRoutingAlgorithm {

	// private boolean[] expanded;
	private ExperimentalDijkstraSearchNode[] nodes;
	private SimpleGraph graph;

	private FibonacciHeap<Integer> heap;

	public ExperimentalDijkstrasShortestPath(SimpleGraph graph, int source,
			ExperimentalDijkstraSearchNode[] nodes) {
		this(graph, Collections.singletonList(source), Collections
				.singletonList(0.0), nodes);
	}

	@SuppressWarnings("unchecked")
	public ExperimentalDijkstrasShortestPath(SimpleGraph graph,
			List<Integer> potentialSources, List<Double> initialCosts,
			ExperimentalDijkstraSearchNode[] nodes) {

		if (potentialSources.size() != initialCosts.size()) {
			throw new IllegalArgumentException("Sources and costs "
					+ "must have the same number of elements.");
		}

		this.nodes = nodes;

		this.graph = graph;
		heap = new FibonacciHeap<Integer>();

		for (int i = 0; i < potentialSources.size(); i++) {
			int source = potentialSources.get(i);
			double cost = initialCosts.get(i);
			FibonacciHeapNode<Integer> firstNode = new FibonacciHeapNode<Integer>(
					source, cost);
			nodes[source].setValues(cost, -1, firstNode);
			heap.insert(firstNode, cost);
		}
	}

	public PathSolution getShortestPath(int destination) {
		return getShortestPath(Collections.singleton(destination));
	}

	private int calculateShortestPath(Set<Integer> targetSet) {

		for (int target : targetSet) {
			nodes[target].setTerminal(true);
		}

		int bestSolution = -1;

		while (heap.size() > 0) {
			FibonacciHeapNode<Integer> min = heap.removeMin();

			int index = min.getData();
			double cost = min.getKey();

			// 
			if (nodes[index].isTerminalNode()) {
				bestSolution = index;
				// System.out.println("found");
				break;
			}

			Node graphNode = graph.getNodes().get(index);

			// Need this???
			nodes[index].markVisited();

			// For all neighbours
			for (int i = 0; i < graphNode.getNeighbours().size(); i++) {
				Node neighbour = graphNode.getNeighbours().get(i);
				int neighbourIndex = neighbour.getID();

				// Still in heap or unvisited?
				if (nodes[neighbourIndex].getCost() == Double.POSITIVE_INFINITY
						|| nodes[neighbourIndex].getHeapNode() != null) {
					double costToNeighbour = cost + graphNode.getCosts().get(i);

					// Better cost?
					if (costToNeighbour < nodes[neighbourIndex].getCost()) {

						FibonacciHeapNode<Integer> heapNode = nodes[neighbourIndex]
								.getHeapNode();

						// Check if we have previously inserted this
						if (heapNode == null) {
							heapNode = new FibonacciHeapNode<Integer>(
									neighbourIndex, costToNeighbour);
							heap.insert(heapNode, costToNeighbour);
						} else {
							heap.decreaseKey(heapNode, costToNeighbour);
						}

						nodes[neighbourIndex].setValues(costToNeighbour, index,
								heapNode);
					}
				}
			}
		}

		for (int target : targetSet) {
			nodes[target].setTerminal(false);
		}

		return bestSolution;
	}

	/**
	 * @param targetSet
	 * @return
	 */
	public PathSolution getShortestPath(Set<Integer> targetSet) {
		int bestSolution = -1;
		double bestCost = Double.POSITIVE_INFINITY;

		for (int target : targetSet) {
			double cost = nodes[target].getCost();
			if (cost < bestCost) {
				bestCost = cost;
				bestSolution = target;
			}
		}

		if (bestSolution == -1) {
			bestSolution = calculateShortestPath(targetSet);
		}

		if (bestSolution == -1) {
			return new PathSolution(Double.POSITIVE_INFINITY, new int[0]);
		}

		List<Integer> path = new LinkedList<Integer>();
		int pointer = bestSolution;
		while (pointer != -1) {
			path.add(pointer);
			pointer = nodes[pointer].getPredecessor();
		}
		int[] pathIDs = new int[path.size()];
		int counter = path.size() - 1;
		for (int id : path) {
			pathIDs[counter] = id;
			counter--;
		}
		return new PathSolution(nodes[bestSolution].getCost(), pathIDs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see newrouting.IRoutingAlgorithm#getShortestPath(java.util.List,
	 * java.util.List)
	 */
	@Override
	public PathSolution getShortestPath(List<Integer> targetSet,
			List<Double> targetCosts) {
		for (Double d : targetCosts) {
			if (d != 0) {
				throw new IllegalArgumentException(
						"Still need to implement arbitrary costs for "
								+ "normal Dijkstra. Use Bidirectional instead.");
			}
		}
		Set<Integer> targets = new FastSet<Integer>();
		targets.addAll(targetSet);
		return getShortestPath(targets);
	}
}
