package iamrescue.routing.dijkstra;

public class PathSolution {
	private double cost;
	private int[] pathIDs;

	public PathSolution(double cost, int[] pathIDs) {
		this.cost = cost;
		this.pathIDs = pathIDs;
	}

	public double getCost() {
		return cost;
	}

	public int[] getPathIDs() {
		return pathIDs;
	}
}
