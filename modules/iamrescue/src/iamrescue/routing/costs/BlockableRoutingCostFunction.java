/**
 * 
 */
package iamrescue.routing.costs;

import iamrescue.agent.ISimulationTimer;
import iamrescue.agent.ITimeStepListener;
import iamrescue.execution.command.IPath;
import iamrescue.util.PositionXY;

import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class BlockableRoutingCostFunction implements IRoutingCostFunction,
		ITimeStepListener {

	private Map<TemporaryBlockKey, BlockInfo> blocks = new FastMap<TemporaryBlockKey, BlockInfo>();
	private Map<Integer, Set<TemporaryBlockKey>> expiryTimes = new FastMap<Integer, Set<TemporaryBlockKey>>();
	private IRoutingCostFunction baseFunction;
	private ISimulationTimer timer;
	private IRoutingCostChangeListener listener;

	public BlockableRoutingCostFunction(IRoutingCostFunction baseFunction,
			ISimulationTimer timer, IRoutingCostChangeListener listener) {
		this.timer = timer;
		this.baseFunction = baseFunction;
		this.listener = listener;
		timer.addTimeStepListener(this);
	}

	public BlockableRoutingCostFunction(IRoutingCostFunction baseFunction,
			ISimulationTimer timer) {
		this(baseFunction, timer, null);
	}

	/**
	 * @param listener
	 *            the listener to set
	 */
	public void setListener(IRoutingCostChangeListener listener) {
		this.listener = listener;
	}

	@Override
	public double getCost(IPath path, StandardWorldModel worldModel) {
		return baseFunction.getCost(path, worldModel);
	}

	@Override
	public double getTravelCost(Area area, PositionXY from, PositionXY to) {
		// Check if there is a block
		TemporaryBlockKey key = new TemporaryBlockKey(area, to);
		BlockInfo info = blocks.get(key);
		if (info != null) {
			boolean remove = false;

			// Check if blocks have changed
			if (info.validUntil < timer.getTime()) {
				remove = true;
			} else {
				if (!area.isBlockadesDefined()) {
					if (info.blockades.size() > 0) {
						remove = true;
					}
				} else if (!area.getBlockades().containsAll(info.blockades)) {
					remove = true;
				}
			}

			if (remove) {
				blocks.remove(key);
				listener.routingCostChanged(area);
			} else {
				return info.multiplier
						* baseFunction.getTravelCost(area, from, to);
			}
		}
		return baseFunction.getTravelCost(area, from, to);
	}

	public void addTemporaryBlock(Area area, PositionXY position,
			int validUntil, double multiplier) {
		if (validUntil >= timer.getTime()) {
			TemporaryBlockKey key = new TemporaryBlockKey(area, position);
			BlockInfo existingInfo = blocks.get(key);
			boolean addThis = true;
			if (existingInfo != null) {
				if (existingInfo.validUntil < validUntil) {
					// Remove old expiry time
					Set<TemporaryBlockKey> set = expiryTimes
							.get(existingInfo.validUntil);
					if (set.size() == 1) {
						expiryTimes.remove(existingInfo.validUntil);
					} else {
						set.remove(key);
					}
				} else {
					// Ignore this
					addThis = false;
				}
			}
			if (addThis) {
				Set<EntityID> blockades = new FastSet<EntityID>();
				if (area.isBlockadesDefined()) {
					blockades.addAll(area.getBlockades());
				}
				blocks.put(key,
						new BlockInfo(validUntil, blockades, multiplier));
				Set<TemporaryBlockKey> set = expiryTimes.get(validUntil);
				if (set == null) {
					set = new FastSet<TemporaryBlockKey>();
					expiryTimes.put(validUntil, set);
				}
				set.add(key);
			}
		}
	}

	@Override
	public void notifyTimeStepStarted(int timeStep) {
		Set<TemporaryBlockKey> keys = expiryTimes.remove(timeStep);
		if (keys != null) {
			for (TemporaryBlockKey temporaryBlockKey : keys) {
				Area area = temporaryBlockKey.area;
				blocks.remove(temporaryBlockKey);
				listener.routingCostChanged(area);
			}
		}
	}

	private static class BlockInfo {
		private int validUntil;
		private Set<EntityID> blockades;
		private double multiplier;

		public BlockInfo(int validUntil, Set<EntityID> blockades,
				double multiplier) {
			super();
			this.validUntil = validUntil;
			this.blockades = blockades;
			this.multiplier = multiplier;
		}

	}

	private static class TemporaryBlockKey {
		private Area area;
		private PositionXY position;

		public TemporaryBlockKey(Area area, PositionXY position) {
			this.area = area;
			this.position = position;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((area == null) ? 0 : area.hashCode());
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
			TemporaryBlockKey other = (TemporaryBlockKey) obj;
			if (area == null) {
				if (other.area != null)
					return false;
			} else if (!area.equals(other.area))
				return false;
			if (position == null) {
				if (other.position != null)
					return false;
			} else if (!position.equals(other.position))
				return false;
			return true;
		}
	}

}
