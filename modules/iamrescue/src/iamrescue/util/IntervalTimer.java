package iamrescue.util;

public class IntervalTimer {
	private long startTime;
	private long interval;

	public IntervalTimer() {

	}

	public void start() {
		startTime = System.nanoTime();
		interval = startTime;
	}

	public long getTime() {
		return System.nanoTime() - startTime;
	}

	public long lap() {
		long now = System.nanoTime();
		long result = now - interval;
		interval = now;
		return result;
	}
}
