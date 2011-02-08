package iamrescue.communication.messages.codec;

import iamrescue.belief.ShortIDIndex;
import iamrescue.communication.messages.codec.property.PropertyEncoderStore;
import iamrescue.routing.WorldModelConverter;
import rescuecore2.config.Config;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

public interface ICommunicationBeliefBaseAdapter {

	boolean isShortIDAvailable(Class<? extends Entity> objectClass);

	Entity getObjectByShortID(short shortID);

	/**
	 * Returns the object with the specified ID or null if object is not
	 * available
	 * 
	 * @param id
	 * @return
	 */
	Entity getObjectByID(int id);

	short getShortID(Entity object);

	WorldModelConverter getConverter();

	boolean isRescueEntity(EntityID id);

	// World getWorldBounds();
	int getMinX();

	int getMinY();

	ShortIDIndex getShortIndex();

	Config getConfig();

	PropertyEncoderStore getEncoders();

}
