/**
 * 
 */
package iamrescue.communication.messages.codec;

import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.ShortIDIndex;
import iamrescue.communication.messages.codec.property.PropertyEncoderStore;
import iamrescue.routing.WorldModelConverter;
import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class CommunicationBeliefBaseAdapter implements
		ICommunicationBeliefBaseAdapter {

	private IAMWorldModel worldModel;
	private WorldModelConverter converter;
	private boolean minXSet = false;
	private int minX;
	private int minY;
	private boolean minYSet = false;
	private Config config;
	private PropertyEncoderStore encoders;

	public CommunicationBeliefBaseAdapter(IAMWorldModel worldModel,
			Config config) {
		this(worldModel, config, null);
	}

	public CommunicationBeliefBaseAdapter(IAMWorldModel worldModel,
			Config config, WorldModelConverter converter) {
		this.worldModel = worldModel;
		this.converter = converter;
		this.config = config;
		this.encoders = new PropertyEncoderStore(config);
	}

	@Override
	public WorldModelConverter getConverter() {
		return converter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.codec.ICommunicationBeliefBaseAdapter
	 * #getObjectByID(int)
	 */
	@Override
	public Entity getObjectByID(int id) {
		return worldModel.getEntity(new EntityID(id));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.codec.ICommunicationBeliefBaseAdapter
	 * #getObjectByShortID(short, java.lang.Class)
	 */
	@Override
	public Entity getObjectByShortID(short shortID) {
		ShortIDIndex shortIndex = worldModel.getShortIndex();
		EntityID entityID = shortIndex.getEntityID(shortID);
		return worldModel.getEntity(entityID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.codec.ICommunicationBeliefBaseAdapter
	 * #getShortID(rescuecore2.worldmodel.Entity)
	 */
	@Override
	public short getShortID(Entity object) {
		ShortIDIndex shortIndex = worldModel.getShortIndex();
		return shortIndex.getShortID(object.getID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.codec.ICommunicationBeliefBaseAdapter
	 * #isShortIDAvailable(java.lang.Class)
	 */
	@Override
	public boolean isShortIDAvailable(Class<? extends Entity> objectClass) {
		ShortIDIndex shortIndex = worldModel.getShortIndex();
		return shortIndex.isIndexed(objectClass);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.codec.ICommunicationBeliefBaseAdapter
	 * #isRescueEntity(rescuecore2.worldmodel.EntityID)
	 */
	@Override
	public boolean isRescueEntity(EntityID id) {
		return worldModel.isRescueEntity(id);
	}

	public int getMinX() {
		if (!minXSet) {
			Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> worldBounds = worldModel
					.getWorldBounds();
			if (worldBounds.first().first() < worldBounds.second().first()) {
				minX = worldBounds.first().first();
			} else {
				minX = worldBounds.second().first();
			}
			minXSet = true;
		}
		return minX;
	}

	public int getMinY() {
		if (!minYSet) {
			Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> worldBounds = worldModel
					.getWorldBounds();
			if (worldBounds.first().second() < worldBounds.second().second()) {
				minY = worldBounds.first().second();
			} else {
				minY = worldBounds.second().second();
			}
			minYSet = true;
		}
		return minY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.codec.ICommunicationBeliefBaseAdapter
	 * #getShortIndex()
	 */
	@Override
	public ShortIDIndex getShortIndex() {
		return worldModel.getShortIndex();
	}

	@Override
	public Config getConfig() {
		return config;
	}

	@Override
	public PropertyEncoderStore getEncoders() {
		return encoders;
	}

}
