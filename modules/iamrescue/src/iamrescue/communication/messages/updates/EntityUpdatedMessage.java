package iamrescue.communication.messages.updates;

import iamrescue.communication.messages.Message;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

public abstract class EntityUpdatedMessage extends Message {

	private short timestamp;

	private EntityID id;

	private Map<String, Property> updatedProperties = new HashMap<String, Property>();

	private Entity object;

	protected final static Log log = LogFactory
			.getLog(EntityUpdatedMessage.class);

	/**
	 * This creates a new update message.
	 * 
	 * @param timestep
	 *            the timestep at which this message was created
	 */
	public EntityUpdatedMessage(short timestamp) {
		this.timestamp = timestamp;
	}

	public String toShortString() {
		return getMessageName() + "[s:" + getSenderAgentID() + ",t:"
				+ timestamp + ",id:" + id + "]";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.Message#getMessageContentsAsString()
	 */
	@Override
	public String getMessageContentsAsString() {
		StringBuffer sb = new StringBuffer();
		sb.append("ID:");
		sb.append(id);
		sb.append(",timestamp=:");
		sb.append(timestamp);
		sb.append(",properties:<");
		if (updatedProperties == null) {
			sb.append("null");
		} else {
			for (Entry<String, Property> entry : updatedProperties.entrySet()) {
				sb.append('(');
				sb.append(entry.getValue());
				sb.append(')');
			}
		}
		return sb.toString();
	}

	/**
	 * Returns the ID of the object this update is about.
	 * 
	 * @return The ID of the object.
	 */
	public EntityID getObjectID() {
		return id;
	}

	public Entity getObject() {
		return object;
	}

	/**
	 * Should return a list of URNs of Properties that are relevant for this
	 * message. Only relevant properties will be communicated, all other
	 * properties are ignored
	 * 
	 * @return
	 */
	public abstract List<String> getRelevantProperties();

	/**
	 * Gets the time at which the message was sent.
	 * 
	 * @return the time at which the message was sent
	 */
	public final short getTimestamp() {
		return timestamp;
	}

	public final void addUpdatedProperty(Property property) {
		String propertyURN = property.getURN();

		if (!getRelevantProperties().contains(propertyURN))
			log.trace("Property " + property + " is not relevant for message "
					+ getClass());
		else {
			if (updatedProperties.containsKey(propertyURN))
				throw new IllegalArgumentException("Property " + property
						+ " has already been set on this message. NewValue "
						+ property.getValue() + " OldValue "
						+ updatedProperties.get(property));

			updatedProperties.put(propertyURN, property);
		}
	}

	public final void removeUpdatedProperty(String propertyURN) {

		if (!getRelevantProperties().contains(propertyURN)) {
			log.trace("Property " + propertyURN
					+ " is not relevant for message " + getClass());
		} else {
			Property removed = updatedProperties.remove(propertyURN);
			if (removed == null) {
				log.warn("Attempted to remove non-existant property "
						+ propertyURN + " from message " + this);
			}
		}

	}

	// public boolean providesOwnCodec(String propertyURN) {
	// / return false;
	// }

	// public PropertyCodec getOwnCodec(String propertyURN) {
	// throw new IllegalArgumentException("No own codec for: " + propertyURN);
	// }

	public final Set<String> getChangedProperties() {
		return updatedProperties.keySet();
	}

	public final boolean isPropertyChanged(String property) {
		return updatedProperties.containsKey(property);
	}

	public Property getProperty(String property) {
		return updatedProperties.get(property);
	}

	public Collection<Property> getProperties() {
		return updatedProperties.values();
	}

	// @Override
	// public final boolean equals(Object obj) {
	// if (obj instanceof UpdateMessage) {
	// UpdateMessage message = (UpdateMessage) obj;
	//
	// if (message.getMessagePrefix() == getMessagePrefix()) {
	// if (message.updatedProperties.keySet().equals(
	// updatedProperties.keySet())) {
	// for (Property property : message.updatedProperties.keySet()) {
	// Object object1 = message.updatedProperties
	// .get(property);
	// Object object2 = updatedProperties.get(property);
	//
	// if (object1 instanceof Number) {
	// if (object2 instanceof Number) {
	// if (!iamrescue.util.NumberUtils.almostEquals(
	// (Number) object1, (Number) object2,
	// 0.05, 30))
	// return false;
	// } else {
	// return false;
	// }
	// }
	// }
	// return true;
	// }
	// }
	// return false;
	// }
	//
	// return false;
	// }

	protected final byte getMessagePrefix() {
		return getCodec().getMessagePrefix();
	}

	public void setObject(Entity object) {
		if (object != null) {
			if (!isCorrectObjectClass(object)) {
				throw new IllegalArgumentException(
						"Object is not of correct type " + object
								+ ". Message class " + getClass());
			}

			this.object = object;
			this.id = object.getID();
		}
	}

	protected abstract boolean isCorrectObjectClass(Entity object);

	private void copyProperties(EntityUpdatedMessage message) {
		message.id = id;
		message.updatedProperties = updatedProperties;
		message.timestamp = timestamp;
		message.object = object;

		super.copyProperties(message);
	}

	@Override
	public final Message copy() {
		EntityUpdatedMessage message = createMessage(getTimestamp());
		copyProperties(message);
		return message;
	}

	protected abstract EntityUpdatedMessage createMessage(short timestamp);
}
