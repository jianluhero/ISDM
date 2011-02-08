package iamrescue.routing.dijkstra;

import iamrescue.routing.IRoutingAlgorithm;
import iamrescue.routing.dijkstra.SimpleGraph.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.jgrapht.util.FibonacciHeap;
import org.jgrapht.util.FibonacciHeapNode;

import cern.colt.Arrays;

public class DijkstrasShortestPath implements IRoutingAlgorithm {

	// private boolean[] expanded;
	private double[] costs;
	private int[] predecessors;
	private FibonacciHeapNode<Integer>[] heapNodes;
	private boolean[] terminalNodes;
	private SimpleGraph graph;

	private FibonacciHeap<Integer> heap;

	public DijkstrasShortestPath(SimpleGraph graph, int source) {
		this(graph, Collections.singletonList(source), Collections
				.singletonList(0.0));
	}

	@SuppressWarnings("unchecked")
	public DijkstrasShortestPath(SimpleGraph graph,
			List<Integer> potentialSources, List<Double> initialCosts) {

		if (potentialSources.size() != initialCosts.size()) {
			throw new IllegalArgumentException("Sources and costs "
					+ "must have the same number of elements.");
		}
		// Initialise
		// expanded = new boolean[graph.getNodes().size()];
		costs = new double[graph.getNodes().size()];
		predecessors = new int[graph.getNodes().size()];
		heapNodes = new FibonacciHeapNode[graph.getNodes().size()];
		terminalNodes = new boolean[graph.getNodes().size()];

		for (int i = 0; i < costs.length; i++) {
			// expanded[i] = false;
			costs[i] = Double.POSITIVE_INFINITY;
			predecessors[i] = -1;
			terminalNodes[i] = false;
		}

		this.graph = graph;
		heap = new FibonacciHeap<Integer>();

		for (int i = 0; i < potentialSources.size(); i++) {
			int source = potentialSources.get(i);
			double cost = initialCosts.get(i);
			FibonacciHeapNode<Integer> firstNode = new FibonacciHeapNode<Integer>(
					source, cost);
			heapNodes[source] = firstNode;
			costs[source] = cost;
			heap.insert(firstNode, cost);
		}
	}

	public PathSolution getShortestPath(int destination) {
		return getShortestPath(Collections.singletonList(destination),
				new ArrayList<Double>());
	}

	public void forceFullCompute() {
		calculateShortestPath(new ArrayList<Integer>(), new ArrayList<Double>());
	}

	public double getCost(int index) {
		return costs[index];
	}

	public int getPredecessor(int index) {
		return predecessors[index];
	}

	private int calculateShortestPath(List<Integer> targetSet,
			List<Double> targetCosts) {

		Map<Integer, Double> targetExtras = new FastMap<Integer, Double>();

		for (int i = 0; i < targetSet.size(); i++) {
			int target = targetSet.get(i);
			terminalNodes[target] = true;
			if (targetCosts.size() > 0) {
				double cost = targetCosts.get(i);
				if (cost > 0) {
					targetExtras.put(target, cost);
				}
			}
		}

		int bestSolution = -1;
		double bestCost = Double.POSITIVE_INFINITY;

		while (heap.size() > 0) {
			FibonacciHeapNode<Integer> min = heap.removeMin();

			int index = min.getData();
			double cost = min.getKey();

			if (cost >= bestCost) {
				break;
			}

			// 
			if (terminalNodes[index]) {
				if (targetExtras.size() == 0) {
					bestSolution = index;
					// System.out.println("found");
					break;
				} else {
					Double extraCost = targetExtras.get(index);
					if (extraCost == null || cost + extraCost < bestCost) {
						if (extraCost == null) {
							bestCost = cost;
						} else {
							bestCost = cost + extraCost;
						}
						bestSolution = index;
					}
				}
			}

			Node graphNode = graph.getNodes().get(index);
			heapNodes[index] = null;

			// For all neighbours
			for (int i = 0; i < graphNode.getNeighbours().size(); i++) {
				Node neighbour = graphNode.getNeighbours().get(i);
				int neighbourIndex = neighbour.getID();

				// Still in heap or unvisited?
				if (costs[neighbourIndex] == Double.POSITIVE_INFINITY
						|| heapNodes[neighbourIndex] != null) {
					double costToNeighbour = cost + graphNode.getCosts().get(i);

					// Better cost?
					if (costToNeighbour < costs[neighbourIndex]) {
						costs[neighbourIndex] = costToNeighbour;
						predecessors[neighbourIndex] = index;

						FibonacciHeapNode<Integer> heapNode = heapNodes[neighbourIndex];

						// Check if we have previously inserted this
						if (heapNode == null) {
							heapNode = new FibonacciHeapNode<Integer>(
									neighbourIndex, costToNeighbour);
							heapNodes[neighbourIndex] = heapNode;
							heap.insert(heapNode, costToNeighbour);
						} else {
							heap.decreaseKey(heapNode, costToNeighbour);
						}
					}

				}
			}
		}

		for (int target : targetSet) {
			terminalNodes[target] = false;
		}

		return bestSolution;
	}

	/**
	 * @param targetSet
	 * @return
	 */
	public PathSolution getShortestPath(List<Integer> targetSet,
			List<Double> targetCosts) {
		int bestSolution = -1;
		double bestCost = Double.POSITIVE_INFINITY;

		if (targetCosts.size() == 0) {
			for (int target : targetSet) {
				if (costs[target] < bestCost) {
					bestCost = costs[target];
					bestSolution = target;
				}
			}
		} else {
			for (int i = 0; i < targetSet.size(); i++) {
				int target = targetSet.get(i);
				double extra = targetCosts.get(i);
				double total = costs[target] + extra;
				if (total < bestCost) {
					bestCost = total;
					bestSolution = target;
				}
			}
		}

		if (bestSolution == -1
				|| (heap.size() > 0 && bestCost > heap.min().getKey())) {
			bestSolution = calculateShortestPath(targetSet, targetCosts);
		}

		if (bestSolution == -1) {
			return new PathSolution(Double.POSITIVE_INFINITY, new int[0]);
		}

		List<Integer> path = new LinkedList<Integer>();
		int pointer = bestSolution;
		while (pointer != -1) {
			path.add(pointer);
			pointer = predecessors[pointer];
		}
		int[] pathIDs = new int[path.size()];
		int counter = path.size() - 1;
		for (int id : path) {
			pathIDs[counter] = id;
			counter--;
		}

		double cost = costs[bestSolution];
		if (targetCosts.size() > 0) {
			for (int i = 0; i < targetSet.size(); i++) {
				if (targetSet.get(i) == bestSolution) {
					cost += targetCosts.get(i);
					break;
				}
			}
		}

		// System.out.println(Arrays.toString(pathIDs));

		return new PathSolution(cost, pathIDs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see newrouting.IRoutingAlgorithm#getShortestPath(java.util.List,
	 * java.util.List)
	 */
	// @Override
	/*
	 * public PathSolution getShortestPath(List<Integer> targetSet, List<Double>
	 * targetCosts) { for (Double d : targetCosts) { if (d != 0) { throw new
	 * IllegalArgumentException( "Still need to implement arbitrary costs for "
	 * + "normal Dijkstra. Use Bidirectional instead."); } } Set<Integer>
	 * targets = new FastSet<Integer>(); targets.addAll(targetSet); return
	 * getShortestPath(targetSet); }
	 */
}
