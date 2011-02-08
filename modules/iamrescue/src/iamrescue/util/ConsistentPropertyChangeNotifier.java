/**
 * 
 */
package iamrescue.util;

import iamrescue.agent.ISimulationTimer;
import iamrescue.agent.ITimeStepListener;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityListener;
import rescuecore2.worldmodel.Property;

/**
 * @author Sebastian
 * 
 */
public class ConsistentPropertyChangeNotifier implements EntityListener,
		ITimeStepListener {
	private ISimulationTimer timer;
	private int currentTime = -1;
	private Entity entity;
	private boolean waiting = false;
	private Map<String, PropertyUpdate> updates = new FastMap<String, PropertyUpdate>();

	private Set<String> relevantProperties;

	private Set<IConsistentPropertyListener> listeners = new FastSet<IConsistentPropertyListener>();

	public ConsistentPropertyChangeNotifier(Entity entity,
			Collection<StandardPropertyURN> relevantProperties,
			IConsistentPropertyListener listener, ISimulationTimer timer) {
		this.entity = entity;
		entity.addEntityListener(this);
		this.relevantProperties = new FastSet<String>();
		this.timer = timer;
		for (StandardPropertyURN standardPropertyURN : relevantProperties) {
			this.relevantProperties.add(standardPropertyURN.toString());
		}
		listeners.add(listener);
	}

	public void removeListener(IConsistentPropertyListener listener) {
		listeners.remove(listener);
		if (listeners.size() == 0) {
			entity.removeEntityListener(this);
		}
	}

	public void addListener(IConsistentPropertyListener listener) {
		if (listeners.size() == 0) {
			throw new IllegalArgumentException(
					"This listener is no longer being notified, "
							+ "as all listeners had previously been removed.");
		}
		listeners.add(listener);
	}

	@Override
	public void propertyChanged(Entity e, Property p, Object oldValue,
			Object newValue) {

		if (relevantProperties.contains(p.getURN())) {

			int time = timer.getTime();
			if (time != currentTime) {
				updates.clear();
				currentTime = time;
			}

			entity = e;
			String urn = p.getURN();
			updates.put(urn, new PropertyUpdate(p, oldValue, newValue));

			if (updates.size() == relevantProperties.size() && !waiting) {
				// Done - wait for end of time step now
				timer.addTimeStepListener(this);
				waiting = true;
			}
		}
	}

	public static class PropertyUpdate {
		private Property property;
		private Object oldValue;
		private Object newValue;

		public PropertyUpdate(Property property, Object oldValue,
				Object newValue) {
			super();
			this.property = property;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		public Property getProperty() {
			return property;
		}

		public Object getOldValue() {
			return oldValue;
		}

		public Object getNewValue() {
			return newValue;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.agent.ITimeStepListener#notifyTimeStepStarted(int)
	 */
	@Override
	public void notifyTimeStepStarted(int timeStep) {
		waiting = false;
		timer.removeTimeStepListener(this);
		if (timeStep == currentTime
				&& relevantProperties.size() == updates.size()) {
			fireUpdate();
			updates.clear();
		}
	}

	/**
	 * 
	 */
	private void fireUpdate() {
		List<IConsistentPropertyListener> copy = new FastList<IConsistentPropertyListener>(
				listeners);
		for (IConsistentPropertyListener listener : copy) {
			PropertyUpdate[] updates = this.updates.values().toArray(
					new PropertyUpdate[this.updates.size()]);
			listener.allPropertiesChanged(entity, updates);
		}
	}
}
