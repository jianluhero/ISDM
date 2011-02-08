/**
 * 
 */
package iamrescue.routing.costs;

import iamrescue.agent.ISimulationTimer;
import iamrescue.execution.command.IPath.BlockedState;
import iamrescue.util.PositionXY;

import java.util.Map;

import javolution.util.FastMap;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class BlockCache {

	private int currentTimeStep = -1;

	// private final BlockIndex LOOK_UP_INDEX = new BlockIndex();

	private Map<BlockIndex, BlockedState> blockedMap = new FastMap<BlockIndex, BlockedState>();
	private Map<EntityID, Boolean> useCommunicatedMap = new FastMap<EntityID, Boolean>();
	private ISimulationTimer timer;

	private BlockIndex lastQuery;

	private static final Logger LOGGER = Logger.getLogger(BlockCache.class);

	/**
	 * 
	 */
	public BlockCache(ISimulationTimer timer) {
		this.timer = timer;
	}

	private boolean resetIfNecessary() {
		if (timer.getTime() != currentTimeStep) {
			if (blockedMap.size() > 0) {
				blockedMap.clear();
			}
			if (useCommunicatedMap.size() > 0) {
				useCommunicatedMap.clear();
			}
			currentTimeStep = timer.getTime();
			return true;
		} else {
			return false;
		}
	}

	public BlockedState getBlockedState(Area area, PositionXY from,
			PositionXY to) {
		boolean reset = resetIfNecessary();
		lastQuery = new BlockIndex(area.getID(), from, to);
		// String out ="Checking : " + area.getID() + " " + from + " -> " + to
		// +":";

		if (reset) {
			// out += "null";
			// LOGGER.warn(out);
			return null;
		} else {
			BlockedState blockedState = blockedMap.get(lastQuery);
			// out +=blockedState;
			// LOGGER.warn(out);
			return blockedState;
		}
	}

	public Boolean getUseCommunicatedInfo(Blockade blockade) {
		boolean reset = resetIfNecessary();
		if (reset) {
			return null;
		} else {
			return useCommunicatedMap.get(blockade.getID());
		}
	}

	public void setBlockedStateForLastQuery(BlockedState state) {
		blockedMap.put(lastQuery, state);
	}

	public void setUseCommunicatedInfo(Blockade blockade,
			Boolean useCommunicatedInfo) {
		resetIfNecessary();
		useCommunicatedMap.put(blockade.getID(), useCommunicatedInfo);
	}

	// public void

	private static class BlockIndex {
		private PositionXY oneEnd;
		private PositionXY otherEnd;
		private EntityID areaID;

		public BlockIndex(EntityID areaID, PositionXY oneEnd,
				PositionXY otherEnd) {
			setValues(areaID, oneEnd, otherEnd);
		}

		private BlockIndex() {

		}

		private void setValues(EntityID areaID, PositionXY oneEnd,
				PositionXY otherEnd) {
			this.areaID = areaID;
			if (oneEnd.getX() < otherEnd.getX()) {
				this.oneEnd = oneEnd;
				this.otherEnd = otherEnd;
			} else if (oneEnd.getX() == otherEnd.getX()) {
				if (oneEnd.getY() < otherEnd.getY()) {
					this.oneEnd = oneEnd;
					this.otherEnd = otherEnd;
				} else {
					this.oneEnd = otherEnd;
					this.otherEnd = oneEnd;
				}
			} else {
				this.oneEnd = otherEnd;
				this.otherEnd = oneEnd;
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((areaID == null) ? 0 : areaID.hashCode());
			result = prime * result
					+ ((oneEnd == null) ? 0 : oneEnd.hashCode());
			result = prime * result
					+ ((otherEnd == null) ? 0 : otherEnd.hashCode());
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
			BlockIndex other = (BlockIndex) obj;
			if (areaID == null) {
				if (other.areaID != null)
					return false;
			} else if (!areaID.equals(other.areaID))
				return false;
			if (oneEnd == null) {
				if (other.oneEnd != null)
					return false;
			} else if (!oneEnd.equals(other.oneEnd))
				return false;
			if (otherEnd == null) {
				if (other.otherEnd != null)
					return false;
			} else if (!otherEnd.equals(other.otherEnd))
				return false;
			return true;
		}

	}
}
