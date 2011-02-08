package iamrescue.routing.dijkstra;

import iamrescue.routing.IRoutingAlgorithm;

import iamrescue.routing.dijkstra.SimpleGraph.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections15.list.FastArrayList;
import org.jgrapht.util.FibonacciHeap;
import org.jgrapht.util.FibonacciHeapNode;


public class MultiDirectionalDijkstrasShortestPath implements IRoutingAlgorithm {

	// private boolean[] expanded;
	private double[] costs;
	private int[] predecessors;
	private FibonacciHeapNode<Integer>[] heapNodes;
	private SimpleGraph graph;

	private double[] targetCosts;
	private int[] targetPredecessors;
	private FibonacciHeapNode<Integer>[] targetHeapNodes;
	private FibonacciHeap<Integer> targetHeap;

	private FibonacciHeap<Integer> heap;
	private boolean[] done;
	private boolean[] targetDone;

	private boolean findOptimal = true;

	public MultiDirectionalDijkstrasShortestPath(SimpleGraph graph, int source) {
		this(graph, Collections.singletonList(source), Collections
				.singletonList(0.0));
	}

	@SuppressWarnings("unchecked")
	public MultiDirectionalDijkstrasShortestPath(SimpleGraph graph,
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
		targetCosts = new double[graph.getNodes().size()];
		targetPredecessors = new int[graph.getNodes().size()];
		targetHeapNodes = new FibonacciHeapNode[graph.getNodes().size()];
		done = new boolean[graph.getNodes().size()];
		targetDone = new boolean[graph.getNodes().size()];

		for (int i = 0; i < costs.length; i++) {
			done[i] = false;
			costs[i] = Double.POSITIVE_INFINITY;
			predecessors[i] = -1;
		}

		this.graph = graph;
		heap = new FibonacciHeap<Integer>();
		targetHeap = new FibonacciHeap<Integer>();

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
				Collections.singletonList(0.0));
	}

	private int calculateShortestPath(List<Integer> targetSet,
			List<Double> destinationCosts) {

		for (int i = 0; i < costs.length; i++) {
			targetDone[i] = false;
			targetCosts[i] = Double.POSITIVE_INFINITY;
			targetPredecessors[i] = -1;
			targetHeapNodes[i] = null;
		}

		targetHeap.clear();

		for (int i = 0; i < targetSet.size(); i++) {
			int target = targetSet.get(i);
			FibonacciHeapNode<Integer> node = new FibonacciHeapNode<Integer>(
					target, destinationCosts.get(i));
			targetHeapNodes[target] = node;
			targetCosts[target] = destinationCosts.get(i);
			targetHeap.insert(node, destinationCosts.get(i));
		}

		boolean isDone = false;
		double bestCost = Double.POSITIVE_INFINITY;
		int bestIndex = -1;

		while (!isDone && (heap.size() > 0 || targetHeap.size() > 0)) {

			// int bestSolution;
			int bestSolution = -1;
			if (heap.size() > 0
					&& (targetHeap.size() == 0 || heap.min().getData() < targetHeap
							.min().getData())) {
				bestSolution = runOneSearchIteration(done, targetDone, heap,
						heapNodes, costs, predecessors, false);
			} else if (targetHeap.size() > 0) {
				bestSolution = runOneSearchIteration(targetDone, done,
						targetHeap, targetHeapNodes, targetCosts,
						targetPredecessors, false);
			}
			double cost = costs[bestSolution] + targetCosts[bestSolution];
			if (cost < bestCost) {
				bestIndex = bestSolution;
				bestCost = cost;
			}
			isDone = done[bestSolution] && targetDone[bestSolution];
		}

		return bestIndex;
	}

	private int runOneSearchIteration(boolean[] thisDone, boolean[] otherDone,
			FibonacciHeap<Integer> heap,
			FibonacciHeapNode<Integer>[] heapNodes, double[] costs,
			int[] predecessors, boolean reverseCost) {
		FibonacciHeapNode<Integer> min = heap.removeMin();

		int index = min.getData();
		double cost = min.getKey();

		thisDone[index] = true;
		
		if (otherDone[index]) {
			return index;
		} /*else {
			
		}
*/
		Node graphNode = graph.getNodes().get(index);
		heapNodes[index] = null;

		// For all neighbours
		for (int i = 0; i < graphNode.getNeighbours().size(); i++) {
			Node neighbour = graphNode.getNeighbours().get(i);
			int neighbourIndex = neighbour.getID();

			// Still in heap or unvisited?
			if (costs[neighbourIndex] == Double.POSITIVE_INFINITY
					|| heapNodes[neighbourIndex] != null) {
				double costToNeighbour;
				if (reverseCost) {
					double edgeCost = 0;
					boolean found = false;
					for (int j = 0; j < neighbour.getNeighbours().size(); j++) {
						if (neighbour.getNeighbours().get(j).getID() == index) {
							edgeCost = neighbour.getCosts().get(j);
							found = true;
							break;
						}
					}
					if (!found) {
						System.out.println("Warning: no neighbour found");
					}
					costToNeighbour = cost + edgeCost;
				} else {
					costToNeighbour = cost + graphNode.getCosts().get(i);
				}

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

		return index;

	}

	public PathSolution getShortestPath(List<Integer> targetSet) {
		List<Double> costs = new ArrayList<Double>(targetSet.size());
		for (int i : targetSet) {
			costs.add(0.0);
		}
		return getShortestPath(targetSet, costs);
	}

	/**
	 * @param targetSet
	 * @return
	 */
	public PathSolution getShortestPath(List<Integer> targetSet,
			List<Double> targetCosts) {
		int bestSolution = -1;
		double bestCost = Double.POSITIVE_INFINITY;
		double bestTargetCost = Double.POSITIVE_INFINITY;

		for (int i = 0; i < targetSet.size(); i++) {
			int target = targetSet.get(i);
			if (costs[target] == Double.POSITIVE_INFINITY
					&& targetCosts.get(i) < bestTargetCost) {
				// Force recompute now, as this could potentially be a better
				// solution
				bestSolution = -1;
				break;
			}
			double cost = costs[target] + targetCosts.get(i);
			if (cost < bestCost) {
				bestCost = cost;
				bestSolution = target;
				bestTargetCost = targetCosts.get(i);
			}
		}

		if (bestSolution == -1) {
			bestSolution = calculateShortestPath(targetSet, targetCosts);
			if (bestSolution == -1) {
				return new PathSolution(Double.POSITIVE_INFINITY, new int[0]);
			} else {
				return buildPath(bestSolution, true);
			}
		} else {
			return buildPath(bestSolution, false);
		}

	}

	/**
	 * @param bestSolution
	 * @return
	 */
	private PathSolution buildPath(int bestSolution, boolean alsoReverse) {
		List<Integer> path = new LinkedList<Integer>();

		// First to source
		int pointer = bestSolution;
		while (pointer != -1) {
			path.add(0, pointer);
			pointer = predecessors[pointer];
		}

		if (alsoReverse) {
			// Now to target
			pointer = targetPredecessors[bestSolution];
			while (pointer != -1) {
				path.add(pointer);
				pointer = targetPredecessors[pointer];
			}
		}

		int[] pathIDs = new int[path.size()];
		int counter = 0;
		for (int id : path) {
			pathIDs[counter] = id;
			counter++;
		}

		double cost = costs[bestSolution];
		if (alsoReverse) {
			cost += targetCosts[bestSolution];
		}

		return new PathSolution(cost, pathIDs);
	}
}
