/**
 * 
 */
package iamrescue.util;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.provenance.IProvenanceInformation;
import iamrescue.belief.provenance.ProvenanceLogEntry;
import iamrescue.belief.provenance.SensedOrigin;

import java.util.Iterator;
import java.util.NavigableSet;

import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.standard.view.PositionHistoryLayer;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class HumanMovementUtility {
	/**
	 * Returns the distance travelled by this agent in the last time step. This
	 * only works if we have the x and y coordinates of the agent and its
	 * position history.
	 * 
	 * @param human
	 *            The human to check
	 * @param model
	 *            The world model
	 * @return The distance, or -1 if not enough information is known.
	 */
	public static int getDistanceJustTravelled(Human human, IAMWorldModel model) {
		Pair<Integer, Integer> location = human.getLocation(model);
		if (location == null) {
			return -1;
		} else {
			if (!human.isPositionHistoryDefined()) {
				return 0;
			}
			int[] positionHistory = human.getPositionHistory();
			if (positionHistory.length == 0 || positionHistory.length == 1
					|| positionHistory.length == 2) {
				return 0;
			}

			double sum = 0;

			PositionXY lastPosition = null;

			for (int i = 0; i < positionHistory.length - 2; i = i + 2) {
				PositionXY thisPosition = new PositionXY(positionHistory[i],
						positionHistory[i + 1]);
				if (lastPosition != null) {
					sum += lastPosition.distanceTo(thisPosition);
				}
				lastPosition = thisPosition;
			}
			PositionXY current = new PositionXY(location);
			sum += lastPosition.distanceTo(current);
			return (int) Math.round(sum);
		}
	}

	/**
	 * Returns the position of the agent during the last time step.
	 * 
	 * @return The position of the agent, or null if it was unknown / undefined
	 *         / too early
	 */
	public static PositionXY findLastKnownPosition(EntityID id,
			IAMWorldModel worldModel, ISimulationTimer timer) {
		int currentTime = timer.getTime();

		// 2 arrays for x and y, respectively
		IProvenanceInformation[] provenances = new IProvenanceInformation[2];
		provenances[0] = worldModel.getProvenance(id, StandardPropertyURN.X);
		provenances[1] = worldModel.getProvenance(id, StandardPropertyURN.Y);

		int[] lastPositions = new int[2];

		boolean found = false;

		// cycle through entries to find latest
		for (int i = 0; i < 2; i++) {
			found = false;
			Iterator<ProvenanceLogEntry> iterator = provenances[i]
					.getAllLatestFirst();
			// iterator = all.descendingIterator();
			while (iterator.hasNext()) {
				ProvenanceLogEntry entry = iterator.next();
				if (entry.getTimeStep() == currentTime - 1) {
					if (entry.getOrigin().equals(SensedOrigin.INSTANCE)) {
						if (entry.getProperty().isDefined()) {
							lastPositions[i] = (Integer) entry.getProperty()
									.getValue();
							found = true;
							break;
						} else {
							// Undefined
							break;
						}
					}
				} else if (entry.getTimeStep() < currentTime - 1) {
					// Too late
					break;
				}
			}
			if (!found) {
				// No point to continue for second value
				break;
			}
		}

		if (!found) {
			return null;
		} else {
			return new PositionXY(lastPositions[0], lastPositions[1]);
		}
	}

	/**
	 * @param e
	 * @param worldModel
	 * @param timer
	 * @return
	 */
	public static int getAbsoluteDistanceJustTravelled(Human e,
			IAMWorldModel worldModel, ISimulationTimer timer) {
		if (!(e.isXDefined() && e.isYDefined() && e.isPositionDefined())) {
			return -1;
		}
		PositionXY currentPosition = new PositionXY(e.getLocation(worldModel));
		PositionXY findLastKnownPosition = findLastKnownPosition(e.getID(),
				worldModel, timer);
		if (findLastKnownPosition == null) {
			return -1;
		}

		return (int) Math.round(currentPosition
				.distanceTo(findLastKnownPosition));
	}
}
