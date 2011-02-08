/**
 * 
 */
package iamrescue.agent.police.goals;

import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.provenance.IProvenanceInformation;
import iamrescue.belief.provenance.ProvenanceLogEntry;
import iamrescue.execution.command.IPath;
import iamrescue.execution.command.IPath.BlockedState;
import iamrescue.routing.IRoutingModule;
import iamrescue.routing.queries.IRoutingLocation;
import iamrescue.routing.queries.RoutingLocation;
import iamrescue.routing.queries.RoutingQuery;

import org.apache.log4j.Logger;

import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

/**
 * @author Sebastian
 */
public class SimpleClearingGoal implements IClearingGoal {
	private EntityID target;
	private double blockedUtility;
	private double unknownUtility;
	private ClearingGoalConfiguration config;
	private double currentUtility;
	private boolean done;

	private static final Logger LOGGER = Logger
			.getLogger(SimpleClearingGoal.class);

	public SimpleClearingGoal(EntityID target, double blockedUtility,
			double unknownUtility, ClearingGoalConfiguration config) {
		this.target = target;
		this.blockedUtility = blockedUtility;
		this.unknownUtility = unknownUtility;
		this.config = config;
	}

	/**
	 * @return the target
	 */
	public EntityID getTarget() {
		return target;
	}

	/**
	 * @return the utility if the target is blocked
	 */
	public double getBlockedUtility() {
		return blockedUtility;
	}

	/**
	 * @return the utility if current status of blockage is unknown
	 */
	public double getUnknownUtility() {
		return unknownUtility;
	}

	@Override
	public double getCurrentUtility() {
		return currentUtility;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	public void evaluateCurrentState() {
		IAMWorldModel worldModel = config.getWorldModel();

		// First check if we know the position. Otherwise, goal is currently
		// useless.
		StandardEntity entity = worldModel.getEntity(target);
		if (entity instanceof Human) {
			Human human = (Human) entity;
			if (!human.isPositionDefined()) {
				// Check latest known position
				currentUtility = 0;
				return;
			}
		}

		IRoutingModule testRouting = config.getTestRouting();
		// Check current status
		IPath testPath = testRouting.findShortestPath(target, config
				.getRefuges());

		LOGGER.debug(config.getWorldModel().getEntity(target)
				.getFullDescription());

		if (!testPath.isValid()
				|| testPath.getBlockedState(config.getWorldModel()).equals(
						BlockedState.BLOCKED)) {
			done = false;
			currentUtility = getBlockedUtility();
		} else if (testPath.getBlockedState(config.getWorldModel()).equals(
				BlockedState.UNBLOCKED)) {
			done = true;
			currentUtility = 0;
		} else {
			done = false;
			currentUtility = getUnknownUtility();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else if (!(obj instanceof SimpleClearingGoal)) {
			return false;
		} else {
			SimpleClearingGoal other = (SimpleClearingGoal) obj;
			return other.target.equals(this.target);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getName() + "->" + getTarget();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return target.hashCode();
	}

	@Override
	public double getCost(PoliceForce agent) {
		if (config.isUseShortestDistanceOnly()) {
			return getDirectLineDistance(agent);
		} else {
			return getRoutingCost(agent);
		}
	}

	private double getRoutingCost(PoliceForce agent) {
		IRoutingLocation start;
		if (agent.isPositionDefined()) {
			start = new RoutingLocation(agent.getID());
		} else {
			IProvenanceInformation provenance = config.getWorldModel()
					.getProvenance(agent.getID(), StandardPropertyURN.POSITION);
			if (provenance == null) {
				return Double.POSITIVE_INFINITY;
			} else {
				ProvenanceLogEntry lastDefined = provenance.getLastDefined();
				if (lastDefined == null
						|| !lastDefined.getProperty().isDefined()) {
					return Double.POSITIVE_INFINITY;
				} else {
					start = new RoutingLocation((EntityID) lastDefined
							.getProperty().getValue());
				}
			}
		}

		if (config.getWorldModel().getEntity(target).getLocation(
				config.getWorldModel()) == null) {
			return Double.POSITIVE_INFINITY;
		}

		IRoutingModule clearingRouting = config.getClearingRouting();
		IPath shortestClearingPath = clearingRouting
				.findShortestPath(new RoutingQuery(start, new RoutingLocation(
						target)));
		if (!shortestClearingPath.isValid()) {
			return Double.POSITIVE_INFINITY;
		} else {
			return clearingRouting.getRoutingCostFunction().getCost(
					shortestClearingPath, config.getWorldModel());
		}
	}

	private double getDirectLineDistance(PoliceForce agent) {
		// Just check distance
		Pair<Integer, Integer> position = agent.getLocation(config
				.getWorldModel());

		if (position == null) {
			position = getBestLocation(agent.getID());
			if (position == null) {
				return Double.POSITIVE_INFINITY;
			}
		}

		// Target
		Pair<Integer, Integer> targetPosition = config.getWorldModel()
				.getEntity(target).getLocation(config.getWorldModel());

		if (targetPosition == null) {
			targetPosition = getBestLocation(target);
			if (targetPosition == null) {

				return Double.POSITIVE_INFINITY;
			}
		}

		int xDiff = position.first() - targetPosition.first();
		int yDiff = position.second() - targetPosition.second();

		return xDiff * xDiff + yDiff * yDiff;
	}

	public Pair<Integer, Integer> getBestLocation(EntityID id) {

		IProvenanceInformation provenanceX = config.getWorldModel()
				.getProvenance(id, StandardPropertyURN.X);
		IProvenanceInformation provenanceY = config.getWorldModel()
				.getProvenance(id, StandardPropertyURN.Y);

		if (provenanceX == null || provenanceY == null) {
			return null;
		}

		// Get last known
		ProvenanceLogEntry lastDefinedX = provenanceX.getLastDefined();
		ProvenanceLogEntry lastDefinedY = provenanceY.getLastDefined();

		if (lastDefinedX == null || lastDefinedY == null) {
			return null;
		}

		Property propertyX = lastDefinedX.getProperty();
		Property propertyY = lastDefinedY.getProperty();

		if (!propertyX.isDefined() || !propertyY.isDefined()) {
			return null;
		}

		return new Pair<Integer, Integer>((Integer) propertyX.getValue(),
				(Integer) propertyY.getValue());
	}
}
