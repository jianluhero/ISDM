/**
 * 
 */
package iamrescue.routing.dijkstra;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.routing.IRoutingAlgorithm;
import iamrescue.routing.costs.IRoutingCostFunction;
import iamrescue.routing.queries.IRoutingLocation;

import java.util.List;

import rescuecore2.misc.Pair;

/**
 * @author Sebastian
 * 
 */
public class BidirectionalDijkstrasRoutingModule extends
		SimpleDijkstrasRoutingModule {

	public BidirectionalDijkstrasRoutingModule(IAMWorldModel worldModel,
			IRoutingCostFunction routingCostFunction, ISimulationTimer timer) {
		super(worldModel, routingCostFunction, timer);
	}

	@Override
	protected IRoutingAlgorithm createSolver(IRoutingLocation from) {
		Pair<List<Integer>, List<Double>> initial = computeSearchNodes(from);
		return new MultiDirectionalDijkstrasShortestPath(graph,
				initial.first(), initial.second());
	}

}
