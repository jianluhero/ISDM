package iamrescue.routing.dijkstra;

import java.util.ArrayList;
import java.util.List;

public class SimpleGraph {

	private List<Node> nodes;
	private int id = 0;

	public SimpleGraph() {
		nodes = new ArrayList<Node>();
	}

	public Node addNode() {
		Node node = new Node(id);
		id++;
		nodes.add(node);
		return node;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	/**
	 * Adds edge (in one direction only)
	 * 
	 * @param index1
	 * @param index2
	 * @param cost
	 */
	public void addEdge(int index1, int index2, double cost) {
		nodes.get(index1).addNeighbour(nodes.get(index2), cost);
		// nodes.get(index2).addNeighbour(nodes.get(index1), cost);
	}

	/**
	 * Adds edge (in one direction only)
	 * 
	 * @param index1
	 * @param index2
	 * @param cost
	 */
	public void addEdge(Node node1, Node node2, double cost) {
		node1.addNeighbour(node2, cost);
		// node2.addNeighbour(node1, cost);
	}

	public static class Node {
		private int id;
		private List<Node> neighbours = new ArrayList<Node>();
		private List<Double> costs = new ArrayList<Double>();

		public Node(int id) {
			this.id = id;
		}

		public int getID() {
			return id;
		}

		private void addNeighbour(Node node, double cost) {
			this.neighbours.add(node);
			this.costs.add(cost);
		}

		public List<Node> getNeighbours() {
			return neighbours;
		}

		public List<Double> getCosts() {
			return costs;
		}

		public void setCost(Node node, double newCost) {
			boolean done = false;
			for (int i = 0; i < neighbours.size(); i++) {
				if (neighbours.get(i).equals(node)) {
					costs.set(i, newCost);
					done = true;
					break;
				}
			}
			if (!done) {
				addNeighbour(node, newCost);
			}
		}

		public Double getCostToNeighbour(Node neighbour) {
			for (int i = 0; i < neighbours.size(); i++) {
				if (neighbours.get(i).equals(neighbour)) {
					return costs.get(i);
				}
			}
			return null;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
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
			Node other = (Node) obj;
			if (id != other.id)
				return false;
			return true;
		}

	}
}
