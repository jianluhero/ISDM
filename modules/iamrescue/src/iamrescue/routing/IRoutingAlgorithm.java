package iamrescue.routing;

import iamrescue.routing.dijkstra.PathSolution;

import java.util.List;



public interface IRoutingAlgorithm {

	/**
	 * Finds the shortest path to the closest element in the target set.
	 * 
	 * @param targetSet
	 *            The target set.
	 * @param targetCosts
	 *            Costs of each target.
	 * @return The path solution to the closest target.
	 */
	public PathSolution getShortestPath(List<Integer> targetSet,
			List<Double> targetCosts);

	/**
	 * Finds the shortest path to the target.
	 * 
	 * @param target
	 *            The target.
	 * @return Path solution.
	 */
	public PathSolution getShortestPath(int target);
}
