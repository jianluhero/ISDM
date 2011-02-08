package iamrescue.agent;

import java.util.List;

import javolution.util.FastList;

public class SimulationTimer implements ISimulationTimer {

	private int time;
	private List<ITimeStepListener> listeners = new FastList<ITimeStepListener>();

	public void setTime(int time) {
		this.time = time;
	}

	public int getTime() {
		return time;
	}

	/**
	 * Registers a new listener with this.
	 * 
	 * @param listener
	 *            The new listener
	 */
	public void addTimeStepListener(ITimeStepListener listener) {
		listeners.add(listener);
	}

	/**
	 * Removes a listener
	 * 
	 * @param listener
	 *            The listener to remove
	 */
	public void removeTimeStepListener(ITimeStepListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Fires the listeners.
	 */
	public void fireTimeStepStarted() {
		List<ITimeStepListener> copy = new FastList<ITimeStepListener>(
				listeners);
		for (ITimeStepListener listener : copy) {
			listener.notifyTimeStepStarted(time);
		}
	}
}