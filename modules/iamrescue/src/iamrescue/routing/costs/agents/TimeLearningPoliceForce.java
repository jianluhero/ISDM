package iamrescue.routing.costs.agents;

import iamrescue.agent.AbstractIAMAgent;
import iamrescue.execution.command.IPath;
import iamrescue.execution.command.MoveCommand;
import iamrescue.execution.command.RestCommand;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

public class TimeLearningPoliceForce extends AbstractIAMAgent<PoliceForce> {

	private EntityID target = null;
	private int startTime = 0;
	private double initialPrediction = -1;
	private StandardEntity[] entities;
	// private BidirectionalDijkstrasRoutingModule routing;
	private int sameLocation = 0;
	private EntityID lastLocation = null;

	private static final Logger LOGGER = Logger
			.getLogger(TimeLearningPoliceForce.class);

	private EntityID lastPosition = new EntityID(0);
	private int samePositionCount = 0;

	@Override
	protected List<StandardEntityURN> getAgentTypes() {
		return Collections.singletonList(StandardEntityURN.POLICE_FORCE);
	}

	@Override
	protected void postConnect() {
		super.postConnect();
		this.entities = getWorldModel().getEntitiesOfType(
				StandardEntityURN.BUILDING).toArray(new StandardEntity[0]);
		// this.routing = new BlockDetectingRoutingModule(getWorldModel(),
		// new SimpleTimeLearningRoutingCostFunction(getWorldModel(),
		// false, getID(), getTimer()), getTimer(),
		// getExecutionService(), getID(), getSpatialIndex());
		// showRoutingViewer();

		showWorldModelViewer();
		// System.out.println(config.getFloatValue("misc.injury.collapse.concrete.all.critical"));
	}

	@Override
	protected void think(int time, ChangeSet changed) {
		if (time < 3) {
			getExecutionService().execute(new RestCommand());
			return;
		}

		if (me().getPosition().equals(lastLocation)) {
			samePositionCount++;
		} else {
			lastLocation = me().getPosition();
			samePositionCount = 0;
		}

		if (target != null && target.equals(me().getPosition())) {
			// Reached!
			LOGGER.info("Reached destination after "
					+ (getTimer().getTime() - startTime)
					+ " time steps. Predicted: " + initialPrediction);
			target = null;
			initialPrediction = -1;
		}

		// Reset if we haven't moved for 500 time steps
		if (samePositionCount > 500) {
			target = null;
		}

		IPath currentPath = null;
		do {
			// Select random building
			if (target == null
					|| (currentPath != null && !currentPath.isValid())) {
				target = entities[(int) (Math.random() * entities.length)]
						.getID();
				startTime = getTimer().getTime();
			}
			currentPath = getRoutingModule().findShortestPath(getID(), target);
			// if (currentPath.isValid()) {
			// LOGGER.info("Selected path " + currentPath);
			// }
		} while (!currentPath.isValid());

		double cost = getRoutingModule().getRoutingCostFunction().getCost(
				currentPath, getWorldModel());

		if (initialPrediction == -1) {
			initialPrediction = cost;
		}

		LOGGER.info("Current estimated cost for path: " + cost);
		/*
		 * LOGGER.info("Current average distance per step: " +
		 * ((SimpleTimeLearningRoutingCostFunction) routing
		 * .getRoutingCostFunction()).getDistancePerStep());
		 */
		getExecutionService().execute(new MoveCommand(currentPath));
	}

	@Override
	protected void fallback(int time, ChangeSet changed) {
		// TODO Auto-generated method stub

	}
}
