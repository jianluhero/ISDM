package iamrescue.routing;

import iamrescue.execution.command.IPath;
import iamrescue.routing.costs.IRoutingCostFunction;
import iamrescue.routing.dijkstra.SimpleGraph;
import iamrescue.routing.queries.IRoutingQuery;
import iamrescue.util.PositionXY;

import java.util.Collection;
import java.util.List;

import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

public interface IRoutingModule {

	/**
	 * Finds the shortest path between the locations given in the routing query.
	 * 
	 * 
	 * @param query
	 *            The query to run.
	 * @return The shortest path (invalid path if none was found).
	 */
	public IPath findShortestPath(IRoutingQuery query);

	/**
	 * This runs a number of routing queries. When some of them share the same
	 * *source* locations, this can be more efficient than running single
	 * queries.
	 * 
	 * @param queries
	 *            The queries to run.
	 * @return List of shortest paths (same order as in the original list).
	 */
	public List<IPath> findShortestPath(List<IRoutingQuery> queries);

	/**
	 * Finds shortest path between given entity and the closest of the given
	 * destinations.
	 * 
	 * @param entity
	 *            The entity to route from (uses exact position if an agent,
	 *            otherwise assumes best exit of area).
	 * @param possibleDestinations
	 *            The possible destinations.
	 * @return The shortest path.
	 */
	public IPath findShortestPath(EntityID from,
			Collection<EntityID> possibleDestinations);

	/**
	 * Routes from an exact position to one of the targets.
	 * 
	 * @param from
	 *            The starting entity
	 * @param exactPosition
	 *            The exact position to route from.
	 * @param possibleDestinations
	 *            The possible destinations.
	 * @return The shortest path to the closest destination.
	 */
	public IPath findShortestPath(EntityID from, PositionXY exactPosition,
			Collection<EntityID> possibleDestinations);

	/**
	 * Finds shortest path from origin to destination.
	 * 
	 * @param from
	 *            Source
	 * @param destination
	 *            Destination
	 * @return Shortest path between these.
	 */
	public IPath findShortestPath(EntityID from, EntityID destination);

	/**
	 * Finds shortest path from origin to destination using exact x,y
	 * coordinates.
	 * 
	 * @param from
	 *            Source
	 * @param fromPosition
	 *            Exact location on source.
	 * @param destination
	 *            Destination
	 * @param destinationPosition
	 *            Exact location at destination.
	 * @return Shortest path between these.
	 */
	public IPath findShortestPath(EntityID from, PositionXY fromPosition,
			EntityID destination, PositionXY destinationPosition);

	/**
	 * Checks if the two entities are connected at all (regardless of
	 * weights/blockades).
	 * 
	 * 
	 * @param from
	 *            One entity
	 * @param to
	 *            other entity
	 * @return true iff connected.
	 */
	public boolean areConnected(EntityID from, EntityID to);

	public IPath findShortestPath(Entity from, Entity to);

	public IPath findShortestPath(Entity from, Collection<? extends Entity> to);

	/**
	 * @return The road cost function that this routing module uses.
	 */
	public IRoutingCostFunction getRoutingCostFunction();

	/**
	 * Returns the underlying routing graph.
	 * 
	 * @return
	 */
	public SimpleGraph getRoutingGraph();

	/**
	 * 
	 * @return The underlying world model converter.
	 */
	public WorldModelConverter getConverter();

}
