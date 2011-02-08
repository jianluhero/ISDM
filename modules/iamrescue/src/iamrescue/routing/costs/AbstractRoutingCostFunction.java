/**
 * 
 */
package iamrescue.routing.costs;

import iamrescue.execution.command.IPath;
import iamrescue.util.PositionXY;

import java.util.List;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public abstract class AbstractRoutingCostFunction implements
		IRoutingCostFunction {

	public abstract double getTravelCost(Area area, PositionXY from,
			PositionXY to);

	private static final Logger LOGGER = Logger
			.getLogger(AbstractRoutingCostFunction.class);

	@Override
	public double getCost(IPath path, StandardWorldModel worldModel) {

		if (!path.isValid()) {
			LOGGER.error("This path is not valid.");
			return Double.POSITIVE_INFINITY;
		}

		// assert path.getLocations().get(0).equals(sourceID) :
		// "Path does not start with source";

		if (path.getLocations().size() == 1) {
			// No movement
			return 0;
		}

		List<EntityID> entities = path.getLocations();
		List<PositionXY> positions = path.getXYPath();

		double cost = 0;

		for (int i = 0; i < entities.size(); i++) {
			PositionXY lastPosition = positions.get(i);
			PositionXY nextPosition = positions.get(i + 1);
			Area entity = (Area) retrieveEntity(entities.get(i), worldModel);
			if (lastPosition.equals(nextPosition)) {
				continue;
			} else {
				cost += getTravelCost(entity, lastPosition, nextPosition);
			}
		}

		return cost;
	}

	protected StandardEntity retrieveEntity(EntityID id,
			StandardWorldModel worldModel) {
		StandardEntity entity = worldModel.getEntity(id);
		if (entity == null) {
			throw new IllegalArgumentException(
					"Could not find entity in world model: " + id);
		}
		return entity;
	}
}
