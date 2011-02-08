/**
 * 
 */
package iamrescue.routing.dijkstra;

import org.jgrapht.util.FibonacciHeapNode;

/**
 * @author Sebastian
 * 
 */
public class ExperimentalDijkstraSearchNode {

	public static final double DEFAULT_COST = Double.POSITIVE_INFINITY;
	public static final int DEFAULT_PREDECESSOR = -1;
	public static final FibonacciHeapNode<Integer> DEFAULT_HEAP_NODE = null;
	public static final boolean DEFAULT_TERMINAL_NODE = false;

	private NodeValidator validator;

	private double cost = DEFAULT_COST;
	private int predecessor = DEFAULT_PREDECESSOR;
	private FibonacciHeapNode<Integer> heapNode = DEFAULT_HEAP_NODE;
	private int valueValidity;

	private boolean terminalNode = DEFAULT_TERMINAL_NODE;
	private int terminalValidity;

	public ExperimentalDijkstraSearchNode(NodeValidator validator) {
		this.validator = validator;
		invalidate();
	}

	public void setValues(double cost, int predecessor,
			FibonacciHeapNode<Integer> heapNode) {
		this.cost = cost;
		this.predecessor = predecessor;
		this.heapNode = heapNode;
		valueValidity = validator.getCurrentIteration();
	}

	public void setTerminal(boolean terminal) {
		this.terminalNode = terminal;
		terminalValidity = validator.getCurrentIteration();
	}

	public void invalidate() {
		valueValidity = Integer.MIN_VALUE;
		terminalValidity = Integer.MIN_VALUE;
	}

	/**
	 * @return the cost
	 */
	public double getCost() {
		if (valueValidity < validator.getCurrentIteration()) {
			return DEFAULT_COST;
		} else {
			return cost;
		}
	}

	/**
	 * @return the heapNode
	 */
	public FibonacciHeapNode<Integer> getHeapNode() {
		if (valueValidity < validator.getCurrentIteration()) {
			return DEFAULT_HEAP_NODE;
		} else {
			return heapNode;
		}
	}

	/**
	 * @return the predecessor
	 */
	public int getPredecessor() {
		if (valueValidity < validator.getCurrentIteration()) {
			return DEFAULT_PREDECESSOR;
		} else {
			return predecessor;
		}
	}

	/**
	 * @return the terminalNode
	 */
	public boolean isTerminalNode() {
		if (terminalValidity < validator.getCurrentIteration()) {
			return DEFAULT_TERMINAL_NODE;
		} else {
			return terminalNode;
		}
	}

	public void markVisited() {
		this.heapNode = null;
	}
}