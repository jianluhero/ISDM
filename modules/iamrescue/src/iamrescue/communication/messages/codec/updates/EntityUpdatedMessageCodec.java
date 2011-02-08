package iamrescue.communication.messages.codec.updates;

import iamrescue.communication.messages.codec.AbstractMessageCodec;
import iamrescue.communication.messages.codec.BitStreamDecoder;
import iamrescue.communication.messages.codec.BitStreamEncoder;
import iamrescue.communication.messages.updates.EntityUpdatedMessage;
import iamrescue.util.BaseConverter;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rescuecore2.registry.Registry;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

public abstract class EntityUpdatedMessageCodec extends
		AbstractMessageCodec<EntityUpdatedMessage> {

	private static final int PROPERTY_UNCHANGED = 0;
	private static final int PROPERTY_UNDEFINED = 1;
	private static final int PROPERTY_CHANGED = 2;

	private static final int NUMBER_OF_PROPERTY_STATES = 3;

	private List<String> relevantProperties;

	// How many bits do we need to encode the property status
	private int bitsRequired;

	private static Log log = LogFactory.getLog(EntityUpdatedMessageCodec.class);

	public EntityUpdatedMessageCodec(List<String> relevantProperties) {
		this.relevantProperties = relevantProperties;
		bitsRequired = (int) Math.ceil(Math.log(Math.pow(
				NUMBER_OF_PROPERTY_STATES, relevantProperties.size()))
				/ Math.log(2));

	}

	@Override
	protected EntityUpdatedMessage decodeMessage(BitStreamDecoder decoder) {
		short timeStamp = (short) (decoder.readNumber() - Byte.MIN_VALUE);
		EntityUpdatedMessage message = createMessage(timeStamp);
		Entity object = decoder.readEntityByID();

		if (object == null) {
			// this will happen if a message has been received about an entity
			// that is not known to this agent yet. Therefore, a new entity will
			// be created with the specified ID
			object = createObject(new EntityID(decoder.readNumber()
					- Short.MIN_VALUE));
		}

		message.setObject(object);

		boolean[] propertyStatusBits = new boolean[bitsRequired];

		for (int i = 0; i < bitsRequired; i++) {
			propertyStatusBits[i] = decoder.readBoolean();
		}

		int[] propertyStatus = BaseConverter.convertFromBinary(
				propertyStatusBits, NUMBER_OF_PROPERTY_STATES);

		// Pad if required
		propertyStatus = BaseConverter.padToLength(propertyStatus,
				relevantProperties.size());

		for (int i = 0; i < propertyStatus.length; i++) {
			if (propertyStatus[i] != PROPERTY_UNCHANGED) {
				// relevant property with index i has changed
				try {
					String propertyKey = relevantProperties.get(i);
					Property value;
					if (propertyStatus[i] == PROPERTY_CHANGED) {
						// if (message.providesOwnCodec(propertyKey)) {
						// PropertyCodec ownCodec = message
						// .getOwnCodec(propertyKey);
						// value = ownCodec.decode(object, decoder);
						// } else {
						value = decoder.readProperty(object, propertyKey);
						// }
					} else {
						value = Registry.getCurrentRegistry().createProperty(
								propertyKey.toString());
						value.undefine();
					}
					message.addUpdatedProperty(value);
					// object.getProperty(value.getURN()).takeValue(value);
				} catch (IndexOutOfBoundsException e) {
					log.error("Property " + i + " does not exist.");
					throw e;
				}
			}
		}

		return message;
	}

	@Override
	protected void encodeMessage(EntityUpdatedMessage message,
			BitStreamEncoder encoder) {
		encoder.appendNumber(message.getTimestamp() + Byte.MIN_VALUE);

		// encode id
		Entity object = message.getObject();

		encoder.appendEntityID(object);

		// encode which properties have changed
		int[] propertyStatus = new int[relevantProperties.size()];

		for (int i = 0; i < relevantProperties.size(); i++) {
			String property = relevantProperties.get(i);

			if (!message.getChangedProperties().contains(property)) {
				// Not included
				propertyStatus[i] = PROPERTY_UNCHANGED;
			} else {
				if (!message.getProperty(property).isDefined()) {
					// Undefined
					// System.out.println("Property " +
					// message.getObject().getProperty(property.toString()) +
					// " undefined.");
					propertyStatus[i] = PROPERTY_UNDEFINED;
				} else {
					// Defined
					// System.out.println("Property " +
					// message.getObject().getProperty(property.toString()) +
					// " defined.");
					propertyStatus[i] = PROPERTY_CHANGED;
				}
			}
		}

		boolean[] propertyAsBits = BaseConverter.convertToBinary(
				propertyStatus, NUMBER_OF_PROPERTY_STATES);

		// Pad if necessary
		propertyAsBits = BaseConverter
				.padToLength(propertyAsBits, bitsRequired);

		encoder.getBitStream().append(propertyAsBits);

		// System.out.println(Arrays.toString(propertyStatus));
		for (int i = 0; i < relevantProperties.size(); i++) {
			String property = relevantProperties.get(i);
			if (propertyStatus[i] == PROPERTY_CHANGED) {
				try {
					// if (message.providesOwnCodec(property)) {
					// PropertyCodec ownCodec = message.getOwnCodec(property);
					// ownCodec.encode(object, message.getProperty(property),
					// encoder);
					// } else {
					encoder.appendProperty(object, message
							.getProperty(property));
					// }
				} catch (Exception e) {
					log.error(getClass() + ": Something went wrong "
							+ "during encoding of property " + property
							+ " for message " + message, e);
				}
			}
		}
	}

	/**
	 * The class of entity this message is about
	 * 
	 * @return
	 */
	protected abstract Class<? extends Entity> getObjectClass();

	/**
	 * Returns a new (empty) instance of a message
	 * 
	 * @param timeStamp
	 * @return
	 */
	protected abstract EntityUpdatedMessage createMessage(short timeStamp);

	/**
	 * Instantiates a new Entity with the correct entity id, or throws a
	 * IllegalArgumentException if the creation of a new entity is not
	 * supported. For example, instantiating a new instance of a Building
	 * doesn't make sense, since all buildings are known at the start of the
	 * simulation. However, it should be supported for messages about Civilians,
	 * since new civilians might be detected after the start of the simulation,
	 * and messages about (previously unknown) civilians are exchanged between
	 * agents.
	 * 
	 * @param id
	 * @return
	 */
	protected abstract Entity createObject(EntityID id);

}
