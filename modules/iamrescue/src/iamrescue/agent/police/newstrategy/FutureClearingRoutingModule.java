package iamrescue.agent.police.newstrategy;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.execution.command.IPath;
import iamrescue.routing.AbstractRoutingModule;
import iamrescue.routing.IRoutingModule;
import iamrescue.routing.WorldModelConverter;
import iamrescue.routing.costs.ClearingAndMovingRoutingFunction;
import iamrescue.routing.costs.IRoutingCostFunction;
import iamrescue.routing.dijkstra.SimpleDijkstrasRoutingModule;
import iamrescue.routing.dijkstra.SimpleGraph;
import iamrescue.routing.queries.IRoutingQuery;
import iamrescue.routing.util.ISpeedInfo;
import iamrescue.util.PositionXY;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;
import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.config.Config;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

public class FutureClearingRoutingModule implements IRoutingModule {

	private static final Logger LOGGER = Logger
			.getLogger(FutureClearingRoutingModule.class);

	private AbstractRoutingModule routingModule;
	private ClearingAndMovingRoutingFunction clearingCostFunction;
	private IAMWorldModel worldModel;

	// Blocks -> num. police agents clearing them
	private Map<EntityID, Integer> ignoreMap;
	private Map<EntityID, Integer> alreadyMap;

	private Set<EntityID> dirty = new FastSet<EntityID>();

	public FutureClearingRoutingModule(Config config, ISimulationTimer timer,
			IAMWorldModel worldModel, ISpeedInfo speedInfo) {
		clearingCostFunction = new ClearingAndMovingRoutingFunction(worldModel,
				config, speedInfo);
		routingModule = new SimpleDijkstrasRoutingModule(worldModel,
				clearingCostFunction, timer);
		this.worldModel = worldModel;
		this.ignoreMap = new FastMap<EntityID, Integer>();
		this.alreadyMap = new FastMap<EntityID, Integer>();

	}

	@Override
	public boolean areConnected(EntityID from, EntityID to) {
		return routingModule.areConnected(from, to);
	}

	public void forceRecompute(Area area) {
		routingModule.forceRecompute(area);
	}

	@Override
	public IPath findShortestPath(IRoutingQuery query) {
		checkRecompute();
		return routingModule.findShortestPath(query);
	}

	private void checkRecompute() {
		if (dirty.size() > 0) {
			for (EntityID id : dirty) {
				recompute(id);
			}
			dirty.clear();
		}
	}

	@Override
	public List<IPath> findShortestPath(List<IRoutingQuery> queries) {
		checkRecompute();
		return routingModule.findShortestPath(queries);
	}

	@Override
	public IPath findShortestPath(EntityID from,
			Collection<EntityID> possibleDestinations) {
		checkRecompute();
		return routingModule.findShortestPath(from, possibleDestinations);
	}

	@Override
	public IPath findShortestPath(EntityID from, PositionXY exactPosition,
			Collection<EntityID> possibleDestinations) {
		checkRecompute();
		return routingModule.findShortestPath(from, exactPosition,
				possibleDestinations);
	}

	@Override
	public IPath findShortestPath(EntityID from, EntityID destination) {
		checkRecompute();
		return routingModule.findShortestPath(from, destination);
	}

	@Override
	public IPath findShortestPath(EntityID from, PositionXY fromPosition,
			EntityID destination, PositionXY destinationPosition) {
		checkRecompute();
		return routingModule.findShortestPath(from, fromPosition, destination,
				destinationPosition);
	}

	@Override
	public IRoutingCostFunction getRoutingCostFunction() {
		return clearingCostFunction;
	}

	@Override
	public SimpleGraph getRoutingGraph() {
		checkRecompute();
		return routingModule.getRoutingGraph();
	}

	public void addIgnored(List<EntityID> blockadesOrRoadsCleared) {
		for (EntityID id : blockadesOrRoadsCleared) {
			Integer already = ignoreMap.get(id);
			if (already == null) {
				ignoreMap.put(id, 1);
				clearingCostFunction.addIgnored(id);
				dirty.add(id);
			} else {
				ignoreMap.put(id, already + 1);
			}
		}
	}

	private void recompute(EntityID id) {
		StandardEntity entity = worldModel.getEntity(id);
		if (entity instanceof Area) {
			routingModule.forceRecompute((Area) entity);
		} else if (entity instanceof Blockade) {
			Blockade b = (Blockade) entity;
			if (b.isPositionDefined()) {
				StandardEntity area = worldModel.getEntity(id);
				routingModule.forceRecompute((Area) area);
			}
		}
	}

	public void removeIgnored(List<EntityID> blockadesOrRoadsCleared) {
		for (EntityID id : blockadesOrRoadsCleared) {
			Integer already = ignoreMap.get(id);
			if (already == null) {
				LOGGER.warn("Trying to remove non-existing blockade");
			} else {
				if (already > 1) {
					ignoreMap.put(id, already - 1);
				} else {
					ignoreMap.remove(id);
					clearingCostFunction.removeIgnored(id);
					dirty.add(id);
				}
			}
		}
	}

	public void addAlreadyWorking(List<EntityID> blockadesOrRoads) {
		for (EntityID id : blockadesOrRoads) {
			Integer already = alreadyMap.get(id);
			if (already == null) {
				alreadyMap.put(id, 1);
			} else {
				alreadyMap.put(id, already + 1);
			}
			clearingCostFunction.addAlreadyClearing(id);
			dirty.add(id);
		}
	}

	public void removeAlreadyWorking(List<EntityID> blockadesOrRoads) {
		for (EntityID id : blockadesOrRoads) {
			Integer already = alreadyMap.get(id);
			if (already == null || already == 1) {
				clearingCostFunction.setAlreadyClearing(id, 0);
				alreadyMap.remove(id);
			} else {
				alreadyMap.put(id, already - 1);
				clearingCostFunction.setAlreadyClearing(id, already - 1);
			}
			clearingCostFunction.addAlreadyClearing(id);
			dirty.add(id);
		}
	}

	public void recomputeAll() {
		Collection<StandardEntity> areas = worldModel.getEntitiesOfType(
				StandardEntityURN.BUILDING, StandardEntityURN.ROAD,
				StandardEntityURN.FIRE_STATION,
				StandardEntityURN.AMBULANCE_CENTRE,
				StandardEntityURN.POLICE_OFFICE, StandardEntityURN.REFUGE);
		for (StandardEntity standardEntity : areas) {
			routingModule.forceRecompute((Area) standardEntity);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.routing.IRoutingModule#getConverter()
	 */
	@Override
	public WorldModelConverter getConverter() {
		return routingModule.getConverter();
	}

	@Override
	public IPath findShortestPath(Entity from, Entity to) {
		checkRecompute();
		return routingModule.findShortestPath(from, to);
	}

	@Override
	public IPath findShortestPath(Entity from, Collection<? extends Entity> to) {
		checkRecompute();
		return routingModule.findShortestPath(from, to);
	}
}