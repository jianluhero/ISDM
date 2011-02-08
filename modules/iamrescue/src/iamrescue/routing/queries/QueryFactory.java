/**
 * 
 */
package iamrescue.routing.queries;

import iamrescue.util.PositionXY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class QueryFactory {

	// private StandardWorldModel worldModel;

	/*
	 * public QueryFactory(StandardWorldModel worldModel) { this.worldModel =
	 * worldModel; }
	 */

	private QueryFactory() {
	}

	public static IRoutingQuery createQuery(EntityID from, EntityID to) {
		return createQuery(from, Collections.singleton(to));
	}

	public static IRoutingQuery createQuery(EntityID from,
			Collection<EntityID> to) {
		IRoutingLocation fromLocation = new RoutingLocation(from);
		List<IRoutingLocation> destinationList = new ArrayList<IRoutingLocation>(
				to.size());
		for (EntityID id : to) {
			destinationList.add(new RoutingLocation(id));
		}
		return new RoutingQuery(fromLocation, destinationList);
	}

	public static IRoutingQuery createQuery(EntityID from,
			PositionXY fromPosition, Collection<EntityID> to) {
		IRoutingLocation fromLocation = new RoutingLocation(from, fromPosition);
		List<IRoutingLocation> destinationList = new ArrayList<IRoutingLocation>(
				to.size());
		for (EntityID id : to) {
			destinationList.add(new RoutingLocation(id));
		}
		return new RoutingQuery(fromLocation, destinationList);
	}

	public static IRoutingQuery createQuery(Entity from,
			Collection<? extends Entity> to) {
		IRoutingLocation fromLocation = new RoutingLocation(from.getID());
		List<IRoutingLocation> destinationList = new ArrayList<IRoutingLocation>(
				to.size());
		for (Entity entity : to) {
			destinationList.add(new RoutingLocation(entity.getID()));
		}
		return new RoutingQuery(fromLocation, destinationList);
	}

	public static IRoutingQuery createQuery(Entity from, Entity to) {
		return createQuery(from.getID(), to.getID());
	}
}
