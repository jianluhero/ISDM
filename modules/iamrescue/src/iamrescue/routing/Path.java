/**
 * 
 */
package iamrescue.routing;

import iamrescue.belief.IAMWorldModel;
import iamrescue.execution.command.IPath;
import iamrescue.routing.costs.IRoutingCostFunction;
import iamrescue.routing.costs.PassableRoutingCostFunction;
import iamrescue.util.PositionXY;

import java.util.ArrayList;
import java.util.List;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 *         Change to iamrescue2010 branch
 * 
 */
public class Path implements IPath {

	public static final Path INVALID_PATH = new Path();

	private List<EntityID> locations;
	private List<PositionXY> positionXYs;

	private double cost;
	private boolean costIsSet = false;

	private boolean valid = true;

	/*
	 * private PositionXY startPosition = null; private PositionXY endPosition =
	 * null;
	 */
	/*
	 * public Path(List<EntityID> locations) { this.locations = locations; }
	 */

	public Path(List<EntityID> locations, List<PositionXY> positionXYs) {
		this.locations = locations;
		this.positionXYs = positionXYs;
		if (locations.size() != 0 || positionXYs.size() != 0) {
			if (locations.size() != positionXYs.size() - 1) {
				throw new IllegalArgumentException(
						"Positions list must be one element longer than locations.");
			}
		}
	}

	public void add(int index, EntityID location) {
		locations.add(index, location);
	}

	/**
	 * @param cost
	 *            the cost to set
	 */
	public void setCost(double cost) {
		this.cost = cost;
		costIsSet = true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((locations == null) ? 0 : locations.hashCode());
		result = prime * result
				+ ((positionXYs == null) ? 0 : positionXYs.hashCode());
		result = prime * result + (valid ? 1231 : 1237);
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
		Path other = (Path) obj;
		if (locations == null) {
			if (other.locations != null)
				return false;
		} else if (!locations.equals(other.locations))
			return false;
		if (positionXYs == null) {
			if (other.positionXYs != null)
				return false;
		} else if (!positionXYs.equals(other.positionXYs))
			return false;
		if (valid != other.valid)
			return false;
		return true;
	}

	public void remove(int index) {
		locations.remove(index);
	}

	private Path() {
		this.valid = false;
	}

	@Override
	public List<EntityID> getLocations() {
		if (!isValid()) {
			throw new IllegalStateException("This path is not valid.");
		}
		return locations;
	}

	public List<EntityID> getLocationsWithoutStart() {
		if (!isValid()) {
			throw new IllegalStateException("This path is not valid.");
		}
		return locations;
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public String toString() {
		if (!isValid()) {
			return "Invalid_Path";
		} else {
			StringBuffer str = new StringBuffer("Path:");
			for (EntityID id : locations) {
				str.append('_');
				str.append(id);
			}
			return str.toString();
		}
	}

	public String toVerboseString() {
		if (!isValid()) {
			return "Invalid_Path";
		} else {
			StringBuffer str = new StringBuffer("Path:");
			for (int i = 0; i < locations.size(); i++) {
				str.append('_');
				str.append(positionXYs.get(i));
				str.append('_');
				str.append(locations.get(i));
			}
			str.append('_');
			str.append(positionXYs.get(positionXYs.size() - 1));
			return str.toString();
		}
	}

	public String toString(StandardWorldModel worldModel) {
		if (!isValid()) {
			return "Invalid_Path";
		} else {
			StringBuffer str = new StringBuffer("Path:");
			for (EntityID id : locations) {
				str.append('_');
				str.append(worldModel.getEntity(id));
			}
			return str.toString();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * execution.command.IPath#getBlockedState(rescuecore2.standard.entities
	 * .StandardWorldModel)
	 */
	@Override
	public BlockedState getBlockedState(IAMWorldModel worldModel) {

		PassableRoutingCostFunction roadCostFunction = new PassableRoutingCostFunction(
				0, 1, Double.POSITIVE_INFINITY, worldModel);

		double cost = roadCostFunction.getCost(this, worldModel);

		if (cost == 0) {
			return BlockedState.UNBLOCKED;
		} else if (cost == Double.POSITIVE_INFINITY) {
			return BlockedState.BLOCKED;
		} else {
			return BlockedState.UNKNOWN;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see execution.command.IPath#getEndPosition()
	 */
	@Override
	public PositionXY getEndPosition() {
		return positionXYs.get(positionXYs.size() - 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see execution.command.IPath#getStartingPosition()
	 */
	@Override
	public PositionXY getStartingPosition() {
		return positionXYs.get(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see execution.command.IPath#getXYPath()
	 */
	@Override
	public List<PositionXY> getXYPath() {
		return positionXYs;
	}

	@Override
	public EntityID getDestination() {
		return locations.get(locations.size() - 1);
	}

	@Override
	public EntityID getStart() {
		return locations.get(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.execution.command.IPath#calculateCost(iamrescue.routing.costs
	 * .IRoutingCostFunction)
	 */
	@Override
	public double calculateCost(IRoutingCostFunction roadCostFunction,
			IAMWorldModel worldModel) {
		cost = roadCostFunction.getCost(this, worldModel);
		costIsSet = true;
		return cost;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.execution.command.IPath#getCost()
	 */
	@Override
	public double getCost() {
		if (!isValid()) {
			return Double.POSITIVE_INFINITY;
		}
		if (!costIsSet) {
			throw new IllegalStateException(
					"The cost of this path has not been set. "
							+ "Call calculateCost() method first");
		}
		return cost;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.execution.command.IPath#containsBurningBuilding(rescuecore2
	 * .standard.entities.StandardWorldModel)
	 */
	@Override
	public boolean containsBurningBuilding(StandardWorldModel model) {
		for (EntityID id : getLocations()) {
			StandardEntity entity = model.getEntity(id);
			if (entity instanceof Building) {
				Building b = (Building) entity;
				if (b.isFierynessDefined()) {
					if (b.isOnFire()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public IPath removeLastNode() {
		if (!valid || locations.size() <= 1) {
			return INVALID_PATH;
		}
		List<EntityID> newList = new ArrayList<EntityID>(locations.size() - 1);
		List<PositionXY> newPositions = new ArrayList<PositionXY>(positionXYs
				.size() - 1);
		for (int i = 0; i < locations.size() - 1; i++) {
			newList.add(locations.get(i));
		}
		for (int i = 0; i < positionXYs.size() - 1; i++) {
			newPositions.add(positionXYs.get(i));
		}
		return new Path(newList, newPositions);
	}
}
