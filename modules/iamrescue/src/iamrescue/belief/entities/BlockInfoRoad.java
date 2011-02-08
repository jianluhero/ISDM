package iamrescue.belief.entities;

import static rescuecore2.standard.Constants.PROPERTY_URN_PREFIX;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.BooleanProperty;
import rescuecore2.worldmodel.properties.IntProperty;

public class BlockInfoRoad extends Road {

	public static final String HAS_BEEN_PASSED_URN = PROPERTY_URN_PREFIX
			+ "hasbeenpassed";

	public static final String IMPORTANCE_URN = PROPERTY_URN_PREFIX
			+ "importance";

	private BooleanProperty hasBeenPassed;

	private IntProperty importance;

	/**
	 * Construct a BlockInfoRoad object with entirely undefined property values.
	 * 
	 * @param id
	 *            The ID of this entity.
	 */
	public BlockInfoRoad(EntityID id) {
		super(id);
		hasBeenPassed = new BooleanProperty(HAS_BEEN_PASSED_URN);
		importance = new IntProperty(IMPORTANCE_URN);
		registerProperties(hasBeenPassed, importance);
	}

	/**
	 * BlockInfoRoad copy constructor.
	 * 
	 * @param other
	 *            The BlockInfoRoad to copy.
	 */
	public BlockInfoRoad(BlockInfoRoad other) {
		super(other);
		hasBeenPassed = new BooleanProperty(other.hasBeenPassed);
		importance = new IntProperty(other.importance);
		registerProperties(hasBeenPassed, importance);
	}

	@Override
	protected Entity copyImpl() {
		return new BlockInfoRoad(getID());
	}

	public boolean hasBeenPassed() {
		return hasBeenPassed.getValue();
	}

	public boolean isHasBeenPassedDefined() {
		return hasBeenPassed.isDefined();
	}

	public void setHasBeenPassed(boolean knownToBePassable) {
		this.hasBeenPassed.setValue(knownToBePassable);
	}

	public BooleanProperty getHasBeenPassedProperty() {
		return hasBeenPassed;
	}

	public void undefineHasBeenPassed() {
		hasBeenPassed.undefine();
	}

	public int getImportance() {
		return importance.getValue();
	}

	public boolean isImportanceDefined() {
		return importance.isDefined();
	}

	public void setImportance(int importance) {
		this.importance.setValue(importance);
	}

	public IntProperty getImportanceProperty() {
		return importance;
	}

	public void undefineImportance() {
		importance.undefine();
	}

	@Override
	public Property getProperty(String urn) {
		if (urn.equals(HAS_BEEN_PASSED_URN)) {
			return hasBeenPassed;
		} else if (urn.equals(IMPORTANCE_URN)) {
			return importance;
		} else {
			return super.getProperty(urn);
		}
	}
}
