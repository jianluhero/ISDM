package iamrescue.agent.firebrigade;

import org.apache.commons.lang.builder.ToStringBuilder;

import rescuecore2.standard.entities.Building;

public class HeatTransferRelation {

	private int rays;
	private double heatTransferRate;
	private Building source;
	private Building destination;

	public HeatTransferRelation(Building source, Building destination) {
		this.source = source;
		this.destination = destination;
	}

	public void incrementRaysHit() {
		rays++;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public void normalise(int totalRays) {
		heatTransferRate = (double) rays / totalRays;
	}

	public double getHeatTransferRate() {
		return heatTransferRate;
	}

	public Building getSource() {
		return source;
	}

	public Building getDestination() {
		return destination;
	}

	public int getRays() {
		return rays;
	}
}
