/**
 * 
 */
package iamrescue.routing.dijkstra;

/**
 * @author Sebastian
 * 
 */
public class NodeValidator {
	private int currentIteration = Integer.MIN_VALUE + 1;

	public boolean increaseIteration() {
		if (currentIteration == Integer.MAX_VALUE) {
			currentIteration = Integer.MIN_VALUE + 1;
			return true;
		} else {
			currentIteration++;
			return false;
		}
	}

	public int getCurrentIteration() {
		return currentIteration;
	}
}
