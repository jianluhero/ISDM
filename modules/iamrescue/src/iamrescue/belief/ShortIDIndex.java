/**
 * 
 */
package iamrescue.belief;

import iamrescue.util.EntityComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.standard.entities.AmbulanceCentre;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.FireStation;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.PoliceOffice;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.entities.World;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 */
public class ShortIDIndex {
	private static final Set<Class<? extends Entity>> indexedClasses = new FastSet<Class<? extends Entity>>();

	private EntityID[] shortToLong;
	private Map<EntityID, Short> longToShort;
	private Set<Class<? extends Entity>> allIndexedClasses = new FastSet<Class<? extends Entity>>();

	/**
	 * Constructs a short id index of this worldmodel (all entities except
	 * civilians and blockades). The world model should already contain all
	 * objects to be indexed.
	 * 
	 * @param worldModel
	 *            The world model.
	 */
	public ShortIDIndex(StandardWorldModel worldModel) {
		List<StandardEntity> fixedObjects = new ArrayList<StandardEntity>();
		Collection<StandardEntity> allObjects = worldModel.getAllEntities();
		for (StandardEntity se : allObjects) {
			for (Class<? extends Entity> standardEntity : indexedClasses) {
				if (standardEntity.isAssignableFrom(se.getClass())) {
					fixedObjects.add(se);
					allIndexedClasses.add(se.getClass());
					break;
				}
			}
		}
		allIndexedClasses.addAll(indexedClasses);

		// Now sort by ID
		Collections.sort(fixedObjects, new EntityComparator());

		longToShort = new FastMap<EntityID, Short>(fixedObjects.size());
		shortToLong = new EntityID[fixedObjects.size()];

		short nextShort = Short.MIN_VALUE;

		for (int i = 0; i < shortToLong.length; i++) {
			StandardEntity entity = fixedObjects.get(i);
			EntityID id = entity.getID();
			longToShort.put(id, nextShort);
			shortToLong[nextShort - Short.MIN_VALUE] = id;
			nextShort++;
		}
	}

	public boolean isIndexed(Class<? extends Entity> clazz) {
		return allIndexedClasses.contains(clazz);
		/*
		 * for (Class<? extends Entity> indexedClass : indexedClasses) { if
		 * (indexedClass.isAssignableFrom(clazz)) { return true; } } return
		 * false;
		 */
	}

	public Set<Class<? extends Entity>> getIndexClasses() {
		return Collections.unmodifiableSet(indexedClasses);
	}

	public short getMinID() {
		return Short.MIN_VALUE;
	}

	public short getMaxID() {
		return (short) (Short.MIN_VALUE + shortToLong.length - 1);
	}

	/**
	 * Returns true if this is associated with a short id.
	 * 
	 * @param id
	 *            The EntityID.
	 * @return True iff it can be converted to a short ID.
	 */
	public boolean knowsAboutLongID(EntityID id) {
		return longToShort.containsKey(id);
	}

	/**
	 * Returns true if this is a valid short id.
	 * 
	 * @param id
	 *            The short id.
	 * @return True iff valid.
	 */
	public boolean knowsAboutShortID(short id) {
		int index = id - Short.MIN_VALUE;
		if (index < 0 || index >= shortToLong.length) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Converts short id to EntityID
	 * 
	 * @param id
	 *            The short id.
	 * @return The EntityID associated with this id.
	 */
	public EntityID getEntityID(short id) {
		int index = id - Short.MIN_VALUE;
		return shortToLong[index];
	}

	/**
	 * Returns the short id of this entityID.
	 * 
	 * @param id
	 *            ID of the entity.
	 * @return short id.
	 */
	public short getShortID(EntityID id) {
		return longToShort.get(id);
	}

	static {
		indexedClasses.add(Area.class);
		indexedClasses.add(Road.class);
		indexedClasses.add(Building.class);
		indexedClasses.add(PoliceForce.class);
		indexedClasses.add(PoliceOffice.class);
		indexedClasses.add(AmbulanceTeam.class);
		indexedClasses.add(AmbulanceCentre.class);
		indexedClasses.add(FireBrigade.class);
		indexedClasses.add(FireStation.class);
		indexedClasses.add(World.class);
	}

}
