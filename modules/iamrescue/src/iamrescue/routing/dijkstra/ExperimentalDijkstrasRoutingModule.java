package iamrescue.routing.dijkstra;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.routing.AbstractRoutingModule;
import iamrescue.routing.IRoutingAlgorithm;
import iamrescue.routing.costs.IRoutingCostFunction;
import iamrescue.routing.queries.IRoutingLocation;

import java.util.List;

import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityListener;

public class ExperimentalDijkstrasRoutingModule extends AbstractRoutingModule
		implements EntityListener {

	private IRoutingLocation lastLocation = null;
	private IRoutingAlgorithm lastAlgorithm = null;
	private NodeValidator validator;
	private ExperimentalDijkstraSearchNode[] nodes;

	public ExperimentalDijkstrasRoutingModule(IAMWorldModel worldModel,
			IRoutingCostFunction routingCostFunction, ISimulationTimer timer) {
		super(worldModel, routingCostFunction, timer);

		validator = new NodeValidator();
		nodes = new ExperimentalDijkstraSearchNode[graph.getNodes().size()];
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = new ExperimentalDijkstraSearchNode(validator);
		}
	}

	/**
	 * @param from
	 */
	/*
	 * protected IRoutingAlgorithm obtainSolver(StandardEntity from, int
	 * positionExtra) {
	 * 
	 * IRoutingAlgorithm shortestPath;
	 * 
	 * if (usingCache) { CacheKey key = new CacheKey(from, positionExtra);
	 * 
	 * // Check if we need to build graph again. shortestPath =
	 * pathCache.get(key); if (shortestPath == null) { shortestPath =
	 * createSolver(from, positionExtra); pathCache.put(key, shortestPath); } }
	 * else { shortestPath = createSolver(from, positionExtra); }
	 * 
	 * return shortestPath; }
	 */

	protected IRoutingAlgorithm createSolver(IRoutingLocation from) {
		boolean needReset = validator.increaseIteration();
		if (needReset) {
			for (ExperimentalDijkstraSearchNode node : nodes) {
				node.invalidate();
			}
		}

		lastLocation = from;

		Pair<List<Integer>, List<Double>> initial = computeSearchNodes(from);
		return new ExperimentalDijkstrasShortestPath(graph, initial.first(),
				initial.second(), nodes);
	}

	@Override
	protected void graphChanged() {
		lastLocation = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see newrouting.AbstractRoutingModule#obtainSolver(util.PositionXY)
	 */
	@Override
	protected IRoutingAlgorithm obtainSolver(IRoutingLocation from) {
		IRoutingAlgorithm shortestPath;

		if (lastLocation != null && lastLocation.equals(from)) {
			shortestPath = lastAlgorithm;
		} else {
			shortestPath = createSolver(from);
			lastAlgorithm = shortestPath;
		}

		return shortestPath;
	}
}
