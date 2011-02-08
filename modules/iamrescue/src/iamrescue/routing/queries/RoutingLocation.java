/**
 * 
 */
package iamrescue.routing.queries;

import iamrescue.util.PositionXY;
import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class RoutingLocation implements IRoutingLocation {

	private EntityID id;
	private PositionXY position;

	public RoutingLocation(EntityID id, PositionXY position) {
		this.id = id;
		this.position = position;
	}

	public RoutingLocation(EntityID id, int x, int y) {
		this(id, new PositionXY(x, y));
	}

	public RoutingLocation(EntityID id, Pair<Integer, Integer> position) {
		this(id, new PositionXY(position));
	}

	public RoutingLocation(EntityID id) {
		this.id = id;
		this.position = null;
	}


	public EntityID getID() {
		return id;
	}

	public PositionXY getPositionXY() {

		if (!hasPositionDefined()) {
			throw new IllegalStateException("Position is undefined.");
		}

		return position;
	}

	public boolean hasPositionDefined() {
		return position != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ID:" + id + ",pos:" + position;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((position == null) ? 0 : position.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RoutingLocation other = (RoutingLocation) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (position == null) {
			if (other.position != null)
				return false;
		} else if (!position.equals(other.position))
			return false;
		return true;
	}

}
