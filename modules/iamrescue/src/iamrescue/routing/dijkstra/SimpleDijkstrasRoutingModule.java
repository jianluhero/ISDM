package iamrescue.routing.dijkstra;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.routing.AbstractRoutingModule;
import iamrescue.routing.IRoutingAlgorithm;
import iamrescue.routing.costs.IRoutingCostFunction;
import iamrescue.routing.queries.IRoutingLocation;
import iamrescue.routing.queries.RoutingLocation;
import iamrescue.routing.util.LeastRecentlyUsedMap;
import iamrescue.util.PositionXY;

import java.util.List;

import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityListener;

public class SimpleDijkstrasRoutingModule extends AbstractRoutingModule
		implements EntityListener {

	private static final int DEFAULT_HISTORY = 10;
	private boolean usingCache = false;

	private LeastRecentlyUsedMap<IRoutingLocation, IRoutingAlgorithm> pathCache = new LeastRecentlyUsedMap<IRoutingLocation, IRoutingAlgorithm>(
			DEFAULT_HISTORY);

	private IRoutingLocation lastLocation = null;
	private IRoutingAlgorithm lastAlgorithm = null;

	public SimpleDijkstrasRoutingModule(IAMWorldModel worldModel,
			IRoutingCostFunction routingCostFunction, ISimulationTimer timer) {
		super(worldModel, routingCostFunction, timer);
	}

	/**
	 * @return True iff this module keeps a history of past searches.
	 */
	public boolean isUsingCache() {
		return usingCache;
	}

	/**
	 * @param usingCache
	 *            Whether the routing module should cache previous results
	 *            (speeds up repeated searches from same origin, but slows down
	 *            new searches).
	 */
	public void setUsingCache(boolean usingCache) {
		this.usingCache = usingCache;
		this.lastLocation = null;
	}

	/**
	 * @return Max size of cache
	 */
	public int getCacheMaxCapacity() {
		return pathCache.getMaxCapacity();
	}

	/**
	 * @param maxCapacity
	 *            How many searches should be cached.
	 */
	public void setCacheMaxCapacity(int maxCapacity) {
		pathCache.setMaximumCapacity(maxCapacity);
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
		if (!usingCache) {
			lastLocation = from;
		}
		Pair<List<Integer>, List<Double>> initial = computeSearchNodes(from);
		return new DijkstrasShortestPath(graph, initial.first(), initial
				.second());
	}

	@Override
	protected void graphChanged() {
		pathCache.clear();
		lastLocation = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see newrouting.AbstractRoutingModule#obtainSolver(util.PositionXY)
	 */
	@Override
	protected IRoutingAlgorithm obtainSolver(IRoutingLocation from) {

		// This is needed in case the position of an agent is updated.
		from = convertToAbsolute(from);

		IRoutingAlgorithm shortestPath;

		if (usingCache) {
			// Check if we need to build graph again.
			shortestPath = pathCache.get(from);
			if (shortestPath == null) {
				shortestPath = createSolver(from);
				pathCache.put(from, shortestPath);
			}
		} else {
			if (lastLocation != null && lastLocation.equals(from)) {
				shortestPath = lastAlgorithm;
			} else {
				shortestPath = createSolver(from);
				lastAlgorithm = shortestPath;
			}
		}

		return shortestPath;
	}

	/**
	 * @param from
	 * @return
	 */
	private IRoutingLocation convertToAbsolute(IRoutingLocation from) {
		StandardEntity entity = worldModel.getEntity(from.getID());
		PositionXY position = new PositionXY(entity.getLocation(worldModel));
		while (entity instanceof Human) {
			entity = worldModel.getEntity(((Human) entity).getPosition());
		}
		return new RoutingLocation(entity.getID(), position);
	}
}
