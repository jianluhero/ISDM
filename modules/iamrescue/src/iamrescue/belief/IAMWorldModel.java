package iamrescue.belief;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.provenance.IProvenanceInformation;
import iamrescue.belief.provenance.IProvenanceStore;
import iamrescue.belief.provenance.ProvenanceLogEntry;
import iamrescue.belief.provenance.ProvenanceStore;
import iamrescue.belief.provenance.SensedOrigin;
import iamrescue.communication.messages.codec.property.PropertyEncoderStore;
import iamrescue.routing.WorldModelConverter;
import iamrescue.routing.costs.BlockCache;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.config.Config;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

public class IAMWorldModel extends StandardWorldModel implements
		IBuildingSearchUtility {

	// Set this to -1 if no time limit
	// private static final int PROVENANCE_MAX_ENTRIES = 10;

	private Map<StandardEntityURN, Collection<StandardEntity>> agentsByType;

	private IProvenanceStore provenance;

	private static final Logger LOGGER = Logger.getLogger(IAMWorldModel.class);

	// private Map<IDPair, EntityID> connectingRoadMap = new FastMap<IDPair,
	// EntityID>();

	private ShortIDIndex shortIndex = null;
	private ISimulationTimer timer;
	private ChangeSet lastMergedChanges;
	private WorldModelConverter converter = null;
	private BlockCache blockCache;

	private BuildingSearchUtility buildingUtility;

	private Config config;

	private FastSet<EntityID> rescueEntities;

	private boolean initialisedSearch = false;
	private StuckMemory stuckMemory;

	public IAMWorldModel(ISimulationTimer timer, Config config) {
		super();
		this.timer = timer;
		this.config = config;
		this.provenance = new ProvenanceStore();
		blockCache = new BlockCache(timer);
		stuckMemory = new StuckMemory();
	}

	public IAMWorldModel(ISimulationTimer timer) {
		this(timer, new Config());
		LOGGER.warn("No configuration specified. Using empty.");
	}

	/**
	 * @return the stuckMemory
	 */
	public StuckMemory getStuckMemory() {
		return stuckMemory;
	}

	public void setDefaultConverter(WorldModelConverter converter) {
		this.converter = converter;
	}

	/**
	 * @return the blockCache
	 */
	public BlockCache getBlockCache() {
		return blockCache;
	}

	/**
	 * @return the converter
	 */
	public WorldModelConverter getDefaultConverter() {
		return converter;
	}

	/**
	 * This signals that initial information about the world has been merged.
	 * Can add logic here to set up further information about static beliefs.
	 */
	public void postConnect() {
		buildingUtility = new BuildingSearchUtility(this, config, timer);
		rescueEntities = new FastSet<EntityID>();
		Collection<StandardEntity> entities = getEntitiesOfType(
				StandardEntityURN.AMBULANCE_CENTRE,
				StandardEntityURN.AMBULANCE_TEAM,
				StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.FIRE_STATION,
				StandardEntityURN.POLICE_FORCE, StandardEntityURN.POLICE_OFFICE);
		for (StandardEntity standardEntity : entities) {
			rescueEntities.add(standardEntity.getID());
		}
		stuckMemory = new StuckMemory();
	}

	/**
	 * Checks if ID belongs to one of the rescue agents or centres.
	 * 
	 * 
	 * @param id
	 *            ID to check
	 * @return True iff agent or centre.
	 */
	public boolean isRescueEntity(EntityID id) {
		return rescueEntities.contains(id);
	}

	/**
	 * Returns an index converter between EntityIDs and shorts.
	 * 
	 * @return the shortIndex
	 */
	public ShortIDIndex getShortIndex() {
		if (shortIndex == null) {
			buildShortIndex();
		}
		return shortIndex;
	}

	public IProvenanceInformation getProvenance(EntityID id,
			StandardPropertyURN property) {
		return provenance.getProvenance(id, property.toString());
	}

	/**
	 * Causes the short index to be built (or rebuilt). This should be called
	 * after all initial objects have been received.
	 */
	public void buildShortIndex() {
		shortIndex = new ShortIDIndex(this);
	}

	/*
	 * private static class IDPair { private int id1; private int id2; public
	 * IDPair(int id1, int id2) { this.id1 = id1; this.id2 = id2; }
	 * 
	 * @Override public int hashCode() { final int prime = 31; int result = 1;
	 * result = prime * result + id1; result = prime * result + id2; return
	 * result; }
	 * 
	 * @Override public boolean equals(Object obj) { if (this == obj) return
	 * true; if (obj == null) return false; if (getClass() != obj.getClass())
	 * return false; IDPair other = (IDPair) obj; if (id1 != other.id1) return
	 * false; if (id2 != other.id2) return false; return true; } public IDPair
	 * getReverse() { return new IDPair(id2, id1); } }
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * rescuecore2.worldmodel.AbstractWorldModel#merge(rescuecore2.worldmodel
	 * .ChangeSet)
	 */
	@Override
	public void merge(ChangeSet changeSet) {
		lastMergedChanges = getChanges(changeSet);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Receiving changeSet: " + changeSet
					+ ". New updates: " + lastMergedChanges);
		}

		removeOldBlockades(lastMergedChanges, changeSet);

		super.merge(changeSet);

		// add provenance information for these updates

		// Careful: we assume here this method is only ever called by the
		// AbstractAgent implementation using the
		// sense updates.
		Set<EntityID> changedEntities = changeSet.getChangedEntities();
		for (EntityID entityID : changedEntities) {
			for (Property p : changeSet.getChangedProperties(entityID)) {
				ProvenanceLogEntry entry = new ProvenanceLogEntry(timer
						.getTime(), SensedOrigin.INSTANCE, p.copy());
				provenance.storeProvenance(entityID, entry);
			}
		}
	}

	/**
	 * Removes blockades that are no longer associated with areas that have just
	 * been seen
	 * 
	 * @param lastMergedChanges
	 * @param changeSet
	 */
	private void removeOldBlockades(ChangeSet lastMergedChanges, ChangeSet seen) {
		Set<EntityID> changedEntities = seen.getChangedEntities();
		for (EntityID entityID : changedEntities) {
			Property changedProperty = seen.getChangedProperty(entityID,
					StandardPropertyURN.BLOCKADES.toString());
			if (changedProperty != null && changedProperty.isDefined()) {
				Set<EntityID> seenBlockades = new FastSet<EntityID>();
				seenBlockades.addAll((List<EntityID>) changedProperty
						.getValue());

				List<EntityID> previousBlockades = ((Area) getEntity(entityID))
						.getBlockades();

				if (previousBlockades != null) {

					for (EntityID previousBlock : previousBlockades) {
						if (!seenBlockades.contains(previousBlock)) {
							// Remove this block
							LOGGER.debug("Removing block " + previousBlock);
							removeEntity(previousBlock);
							lastMergedChanges.entityDeleted(previousBlock);
						}
					}
				}
			}
		}
	}

	public Map<StandardEntityURN, Collection<StandardEntity>> getRescueAgents() {
		if (agentsByType == null) {
			agentsByType = new HashMap<StandardEntityURN, Collection<StandardEntity>>();
			agentsByType.put(StandardEntityURN.AMBULANCE_CENTRE,
					getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE));
			agentsByType.put(StandardEntityURN.AMBULANCE_TEAM,
					getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM));
			agentsByType.put(StandardEntityURN.FIRE_BRIGADE,
					getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE));
			agentsByType.put(StandardEntityURN.FIRE_STATION,
					getEntitiesOfType(StandardEntityURN.FIRE_STATION));
			agentsByType.put(StandardEntityURN.POLICE_FORCE,
					getEntitiesOfType(StandardEntityURN.POLICE_FORCE));
			agentsByType.put(StandardEntityURN.POLICE_OFFICE,
					getEntitiesOfType(StandardEntityURN.POLICE_OFFICE));
		}

		return agentsByType;
	}

	/**
	 * @param objectID
	 * @param entry
	 */
	public void storeProvenance(EntityID id, ProvenanceLogEntry entry) {
		provenance.storeProvenance(id, entry);
	}

	/**
	 * Takes a sensed changeSet and returns the true changes compared to the
	 * current state of the world model.
	 * 
	 * @param sensed
	 *            The sensed changes
	 * @return True change set
	 */
	public ChangeSet getChanges(ChangeSet sensed) {
		ChangeSet changes = new ChangeSet();
		Set<EntityID> changedEntities = sensed.getChangedEntities();
		for (EntityID entityID : changedEntities) {
			Entity entity = getEntity(entityID);
			Set<Property> changedProperties = sensed
					.getChangedProperties(entityID);
			String urn = sensed.getEntityURN(entityID);
			if (entity == null) {
				// Entity did not exist
				for (Property property : changedProperties) {
					changes.addChange(entityID, urn, property);
				}
			} else {
				// Entity existed
				for (Property property : changedProperties) {
					// Compare to world model
					Property knownProperty = entity.getProperty(property
							.getURN());

					if (knownProperty.getValue() == null) {
						if (property.getValue() != null) {
							changes.addChange(entityID, urn, property);
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Property is not null anymore: "
										+ property);
							}
						}
					} else {
						if (property.getValue() == null) {
							changes.addChange(entityID, urn, property);
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Property is now null: "
										+ property);
							}
						} else {
							if (!checkIfPropertiesEqual(knownProperty, property)) {
								if (LOGGER.isTraceEnabled()) {
									LOGGER.trace(knownProperty
											+ " is not equal to " + property);
								}
								// For position, check with some margin for
								// error
								if (property.getURN().equals(
										StandardPropertyURN.X.toString())
										|| property.getURN().equals(
												StandardPropertyURN.Y
														.toString())) {
									int diff = (Integer) property.getValue()
											- (Integer) knownProperty
													.getValue();
									if (diff < -PropertyEncoderStore.COORD_ACCURACY
											|| diff > PropertyEncoderStore.COORD_ACCURACY) {
										changes.addChange(entityID, urn,
												property);
									}
								} else {
									changes.addChange(entityID, urn, property);
								}
							}
						}
					}
				}
			}
		}
		return changes;
	}

	/**
	 * @param knownProperty
	 * @param newProperty
	 * @return
	 */
	public static boolean checkIfPropertiesEqual(Property knownProperty,
			Property newProperty) {
		if (knownProperty.isDefined()) {
			if (!newProperty.isDefined()) {
				return false;
			} else {
				Object knownValue = knownProperty.getValue();
				Object newValue = newProperty.getValue();
				return checkIfPropertyValuesEqual(knownValue, newValue);
			}
		} else {
			if (!newProperty.isDefined()) {
				return true;
			} else {
				return false;
			}
		}
	}

	public static boolean checkIfPropertyValuesEqual(Object knownValue,
			Object newValue) {
		if (knownValue == null) {
			return newValue == null;
		} else {
			if (newValue == null) {
				return false;
			} else {
				if (!knownValue.equals(newValue)) {
					if (knownValue instanceof int[]
							&& newValue instanceof int[]) {
						int[] knownValueList = (int[]) knownValue;
						int[] newValueList = (int[]) newValue;
						boolean equal = Arrays.equals(knownValueList,
								newValueList);
						return equal;
					}
					return false;
				} else {
					return true;
				}
			}
		}
	}

	/**
	 * @return the lastSensedChanges
	 */
	public ChangeSet getLastSensedChanges() {
		return lastMergedChanges;
	}

	/**
	 * @param objectID
	 * @param urn
	 * @return
	 */
	public IProvenanceInformation getProvenance(EntityID objectID, String urn) {
		return provenance.getProvenance(objectID, urn);
	}

	/**
	 * Returns the properties about which this model has provenance information.
	 * 
	 * @param objectID
	 *            The ID in question.
	 * @return Properties that have associated provenance information (empty if
	 *         none known or object has been unknown).
	 */
	public Collection<String> getProvenanceProperties(EntityID objectID) {
		return provenance.getKnownProperties(objectID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.belief.IBuildingSearchUtility#getSafeUnsearchedBuildings()
	 */
	@Override
	public Collection<EntityID> getSafeUnsearchedBuildings() {
		return buildingUtility.getSafeUnsearchedBuildings();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.belief.IBuildingSearchUtility#getUnknownBuildings()
	 */
	@Override
	public Collection<EntityID> getUnknownBuildings() {
		return buildingUtility.getUnknownBuildings();
	}

	@Override
	public Collection<EntityID> getModulatedSafeUnsearchedBuildings() {
		// TODO Auto-generated method stub
		return buildingUtility.getModulatedSafeUnsearchedBuildings();
	}

	@Override
	public Collection<EntityID> getModulatedUnknownBuildings() {
		// TODO Auto-generated method stub
		return buildingUtility.getModulatedUnknownBuildings();
	}

	public boolean initialisedSearch() {
		// TODO Auto-generated method stub
		return initialisedSearch;
	}

	public void initialiseSearch(EntityID myself) {
		// TODO Auto-generated method stub
		buildingUtility.initialiseSearchList(myself);
		initialisedSearch = true;
	}

	public void updateHighPriorityBuildings(Collection<StandardEntity> buildings) {
		buildingUtility.addHighPriorityBuildings(buildings);
	}

	@Override
	public Collection<EntityID> getSafeHigh() {
		// TODO Auto-generated method stub
		return buildingUtility.getSafeHigh();
	}

	@Override
	public Collection<EntityID> getUnknownHigh() {
		// TODO Auto-generated method stub
		return buildingUtility.getUnknownHigh();
	}

}
