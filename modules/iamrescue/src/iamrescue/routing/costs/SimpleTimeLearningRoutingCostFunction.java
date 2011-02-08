package iamrescue.routing.costs;

import iamrescue.belief.IAMWorldModel;
import iamrescue.routing.util.ISpeedInfo;
import iamrescue.util.PositionXY;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.Area;

public class SimpleTimeLearningRoutingCostFunction extends
		SimpleDistanceRoutingCostFunction {

	private static final Logger LOGGER = Logger
			.getLogger(SimpleTimeLearningRoutingCostFunction.class);
	private ISpeedInfo speedInfo;

	public SimpleTimeLearningRoutingCostFunction(IAMWorldModel worldModel,
			ISpeedInfo speedInfo, boolean ignoreBlocks) {
		super(worldModel, ignoreBlocks);
		this.speedInfo = speedInfo;
	}

	@Override
	public void setBurningPenalty(double burningPenalty) {
		super.setBurningPenalty(burningPenalty);
	}

	@Override
	public double getBurningPenalty() {
		return super.getBurningPenalty();
	}

	@Override
	public double getTravelCost(Area area, PositionXY from, PositionXY to) {
		double distance = super.getTravelCost(area, from, to);
		return speedInfo.getTimeToTravelDistance(distance);
	}

}