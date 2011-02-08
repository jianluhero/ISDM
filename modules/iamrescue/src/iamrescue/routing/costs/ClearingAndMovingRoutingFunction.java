package iamrescue.routing.costs;

import iamrescue.belief.IAMWorldModel;
import iamrescue.routing.util.ISpeedInfo;
import iamrescue.util.PositionXY;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.Area;
import rescuecore2.worldmodel.EntityID;

public class ClearingAndMovingRoutingFunction extends
		AbstractRoutingCostFunction {

	public static final int DEFAULT_MM_PER_TIME_STEP = 60000;

	private SimpleDistanceRoutingCostFunction moveCostFunction;
	private ClearingCostFunction repairCostFunction;
	private int mmPerTimeStep = DEFAULT_MM_PER_TIME_STEP;

	public ClearingAndMovingRoutingFunction(IAMWorldModel worldModel,
			Config config, ISpeedInfo speedInfo) {
		this.moveCostFunction = new SimpleTimeLearningRoutingCostFunction(
				worldModel, speedInfo, true);
		// this.move
		this.repairCostFunction = new ClearingCostFunction(worldModel, config);
		this.moveCostFunction.setBurningPenalty(5000000);
		mmPerTimeStep = 1;
	}

	public ClearingAndMovingRoutingFunction(IAMWorldModel worldModel,
			Config config) {
		this.moveCostFunction = new SimpleDistanceRoutingCostFunction(
				worldModel);
		// this.move
		this.repairCostFunction = new ClearingCostFunction(worldModel, config);
		this.moveCostFunction.setBurningPenalty(5000000);
	}

	@Override
	public double getTravelCost(Area area, PositionXY from, PositionXY to) {
		double moveCost = moveCostFunction.getTravelCost(area, from, to)
				/ mmPerTimeStep;
		double repairCost = repairCostFunction.getTravelCost(area, from, to);
		return moveCost + repairCost;
	}

	public void addIgnored(EntityID id) {
		repairCostFunction.addIgnored(id);

	}

	public void removeIgnored(EntityID id) {
		repairCostFunction.removeIgnored(id);
	}

	public void addAlreadyClearing(EntityID id) {
		repairCostFunction.addAlreadyClearing(id);
	}

	public void setAlreadyClearing(EntityID id, int already) {
		repairCostFunction.setAlreadyClearing(id, already);
	}

	public int getAlreadyClearing(EntityID id) {
		return repairCostFunction.getAlreadyClearing(id);
	}

	public void clearIgnored() {
		repairCostFunction.clearIgnored();
	}
}