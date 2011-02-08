package iamrescue.routing.util;

import iamrescue.agent.ISimulationTimer;
import iamrescue.agent.ITimeStepListener;
import iamrescue.belief.IAMWorldModel;
import iamrescue.execution.command.IPath;
import iamrescue.routing.costs.SimpleDistanceRoutingCostFunction;
import iamrescue.util.HumanMovementUtility;

import java.util.Collection;

import javolution.util.FastList;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.EntityID;

public class SpeedLearningModule implements ITimeStepListener, ISpeedInfo {
	/**
	 * How many observations have been made.
	 */
	// private double learningRate = 0.2;
	private int observations = 0;

	// Magic number from traffic simulator code (is mean max velocity. probably
	// less in practice).
	private double distancePerStep = 35000;

	/**
	 * If previous distance is below this threshold (as percentage of current
	 * average distance), it is not counted.
	 */
	private double noMoveThreshold = 0.2;

	private EntityID myID;

	private ISimulationTimer timer;

	// private ConsistentPropertyChangeNotifier notifier;

	private int lastStepDistance = 0;

	private IAMWorldModel worldModel;

	private SimpleDistanceRoutingCostFunction distanceRouting = null;

	private static final Logger LOGGER = Logger
			.getLogger(SpeedLearningModule.class);

	public SpeedLearningModule(IAMWorldModel worldModel, EntityID myID,
			ISimulationTimer timer) {
		this.worldModel = worldModel;
		this.timer = timer;
		this.myID = myID;

		Collection<StandardPropertyURN> properties = new FastList<StandardPropertyURN>();
		properties.add(StandardPropertyURN.POSITION);
		properties.add(StandardPropertyURN.POSITION_HISTORY);
		properties.add(StandardPropertyURN.X);
		properties.add(StandardPropertyURN.Y);

		// notifier = new ConsistentPropertyChangeNotifier(worldModel
		// .getEntity(myID), properties, this, timer);

		timer.addTimeStepListener(this);
	}

	/*
	 * public void setLearningRate(double learningRate) { this.learningRate =
	 * learningRate; }
	 * 
	 * public double getLearningRate() { return learningRate; }
	 */

	public void setNoMoveThreshold(double noMoveThreshold) {
		this.noMoveThreshold = noMoveThreshold;
	}

	public double getNoMoveThreshold() {
		return noMoveThreshold;
	}

	private void setDistancePerStep(double distancePerStep) {
		this.distancePerStep = distancePerStep;
	}

	public double getDistancePerTimeStep() {
		return distancePerStep;
	}

	private void processUpdate() {
		Human e = (Human) worldModel.getEntity(myID);

		int sum = HumanMovementUtility.getDistanceJustTravelled(e, worldModel);

		if (sum == -1) {
			LOGGER.warn("Could not compute distance travelled of "
					+ e.getFullDescription());
			return;
		}

		// System.out.println("Sum: " + sum);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Moved " + sum + "mm during last time step.");
		}
		double previous = getDistancePerTimeStep();
		if (sum < noMoveThreshold * previous) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("This is too small to change current average of "
						+ previous);
			}
		} else {
			double newAverage = (observations * previous + sum)
					/ (observations + 1);
			observations++;
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("New average: " + newAverage);
			}
			setDistancePerStep(newAverage);
		}
		this.lastStepDistance = sum;
	}

	/**
	 * @return The distance this agent travelled during the last time step
	 */
	public int getLastTimeStepDistance() {
		return lastStepDistance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.agent.ITimeStepListener#notifyTimeStepStarted(int)
	 */
	@Override
	public void notifyTimeStepStarted(int timeStep) {
		if (timeStep > 1) {
			processUpdate();
		}

	}

	@Override
	public double getTimeToTravelDistance(double distance) {
		return distance / distancePerStep;
	}

	@Override
	public double getTimeToTravelPath(IPath path) {
		if (distanceRouting == null) {
			distanceRouting = new SimpleDistanceRoutingCostFunction(worldModel,
					true);
		}
		double distance = distanceRouting.getCost(path, worldModel);
		return getTimeToTravelDistance(distance);
	}

}
