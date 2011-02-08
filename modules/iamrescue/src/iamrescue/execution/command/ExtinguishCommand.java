package iamrescue.execution.command;

import iamrescue.execution.IExecutionService;

import org.apache.commons.lang.Validate;

import rescuecore2.standard.entities.Building;

public class ExtinguishCommand extends FireBrigadeCommand {
	private Building buildingToExtinguish;

	private double percentageOfFullPower;

	public Building getBuildingToExtinguish() {
		return buildingToExtinguish;
	}

	public void setBuildingToExtinguish(Building buildingToExtinguish) {
		this.buildingToExtinguish = buildingToExtinguish;
	}

	public double getPercentageOfFullPower() {
		return percentageOfFullPower;
	}

	public void setPercentageOfFullPower(double percentageOfFullPower) {
		this.percentageOfFullPower = percentageOfFullPower;
	}

	public void execute(IExecutionService service) {
		service.execute(this);
	}

	public void checkValidity() {
		Validate.notNull(buildingToExtinguish);
		Validate.isTrue(percentageOfFullPower > 0.0
				&& percentageOfFullPower <= 1.0);
	}
}
