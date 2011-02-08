/**
 * 
 */
package iamrescue.belief;

import iamrescue.agent.ISimulationTimer;
import iamrescue.agent.ITimeStepListener;
import iamrescue.belief.provenance.IProvenanceInformation;
import iamrescue.belief.provenance.ProvenanceLogEntry;
import iamrescue.util.EntityComparator;
import iamrescue.util.PositionXY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.config.Config;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.EntityListener;
import rescuecore2.worldmodel.Property;

/**
 * @author Sebastian
 * 
 */
public class BuildingSearchUtility implements IBuildingSearchUtility,
		EntityListener, ITimeStepListener {

	private static final double THRESHOLD = 0.8;

	private IAMWorldModel worldModel;
	private FastSet<EntityID> safeUnsearchedBuildings;
	private FastSet<EntityID> unknownBuildings;
	private FastSet<EntityID> modulatedSafeUnsearchedBuildings;
	private FastSet<EntityID> modulatedUnknownBuildings;
	private FastSet<EntityID> safeHighBuildings;
	private FastSet<EntityID> unknownHighBuildings;
	private int viewDistance;
	private Set<Human> changed = new FastSet<Human>();
	private long randomSeed = 47298123;
	// private ISimulationTimer timer;

	private static final Logger LOGGER = Logger
			.getLogger(BuildingSearchUtility.class);

	private static final String VIEW_DISTANCE_KEY = "perception.los.max-distance";

	public BuildingSearchUtility(IAMWorldModel worldModel, Config config,
			ISimulationTimer timer) {
		this.worldModel = worldModel;
		timer.addTimeStepListener(this);

		safeUnsearchedBuildings = new FastSet<EntityID>();
		unknownBuildings = new FastSet<EntityID>();
		modulatedSafeUnsearchedBuildings = new FastSet<EntityID>();
		modulatedUnknownBuildings = new FastSet<EntityID>();
		safeHighBuildings = new FastSet<EntityID>();
		unknownHighBuildings = new FastSet<EntityID>();
		viewDistance = config.getIntValue(VIEW_DISTANCE_KEY, 30000);

		Collection<StandardEntity> buildings = worldModel
				.getEntitiesOfType(StandardEntityURN.BUILDING,
						StandardEntityURN.AMBULANCE_CENTRE,
						StandardEntityURN.FIRE_STATION,
						StandardEntityURN.POLICE_OFFICE);

		for (StandardEntity standardEntity : buildings) {
			Building building = (Building) standardEntity;

			if (building.isFierynessDefined()) {
				if (building.getFieryness() == 0) {
					safeUnsearchedBuildings.add(building.getID());
					modulatedSafeUnsearchedBuildings.add(building.getID());
					building.addEntityListener(this);
				}
			} else {
				unknownBuildings.add(building.getID());
				modulatedUnknownBuildings.add(building.getID());
				building.addEntityListener(this);
			}

		}

		Collection<StandardEntity> agents = worldModel.getEntitiesOfType(
				StandardEntityURN.AMBULANCE_TEAM,
				StandardEntityURN.POLICE_FORCE, StandardEntityURN.FIRE_BRIGADE);

		for (StandardEntity agent : agents) {
			Human human = (Human) agent;
			if (human.isPositionDefined()) {
				processPositionUpdate(human);
			}
			human.addEntityListener(this);
		}
	}
	
	public void initialiseSearchList(EntityID myself){
		//set start and end Ids for this agent to prefer to search
		//team size is used to make the set
		Collection<StandardEntity> preferences = worldModel.getEntitiesOfType(StandardEntityURN.BUILDING);
		ArrayList<StandardEntity> prefs = new ArrayList<StandardEntity>();
		for(Iterator<StandardEntity> it = preferences.iterator();it.hasNext();){
			StandardEntity b = it.next();
			prefs.add(b);
		}
		Collections.sort(prefs, new EntityComparator());
		Collections.shuffle(prefs, new Random(randomSeed ));
		Collection<StandardEntity> agentUnsorted = worldModel.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM,StandardEntityURN.POLICE_FORCE);
		ArrayList<StandardEntity> agents = new ArrayList<StandardEntity>();
		for(Iterator<StandardEntity> it = agentUnsorted.iterator();it.hasNext();){
			StandardEntity b = it.next();
			agents.add(b);
		}
		Collections.sort(agents, new EntityComparator());
		//assumption - worldmodel returns agents in the same order? need to ask
		int pos=0;
		int position=0;
		for(Iterator<StandardEntity> it = agents.iterator();it.hasNext();){
			StandardEntity agent = it.next();
			if(agent.getID().getValue()==myself.getValue()){
				position = pos;
			}
			pos++;
		}
		int clustersize = prefs.size()/agents.size();
		ArrayList<EntityID> preference = makePreferenceList(position,clustersize,prefs);
		
		modulatedSafeUnsearchedBuildings = new FastSet<EntityID>();
		modulatedUnknownBuildings = new FastSet<EntityID>();
		
		Collection<StandardEntity> buildings = worldModel
		.getEntitiesOfType(StandardEntityURN.BUILDING,
				StandardEntityURN.AMBULANCE_CENTRE,
				StandardEntityURN.FIRE_STATION,
				StandardEntityURN.POLICE_OFFICE);

		for (StandardEntity standardEntity : buildings) {
			Building building = (Building) standardEntity;

			if (building.isFierynessDefined() && preference.contains(building.getID())) {
				if (building.getFieryness() == 0) {
					modulatedSafeUnsearchedBuildings.add(building.getID());
				}
			} else if(preference.contains(building.getID())) {
				modulatedUnknownBuildings.add(building.getID());
			}

		}
	}
	
	/*
	 * keeps list in prerefnces for my section
	 */
	private ArrayList<EntityID> makePreferenceList(int position, int clustersize, ArrayList<StandardEntity> preferences) {
		ArrayList<EntityID> preference = new ArrayList<EntityID>();
		int start = clustersize*position;
		int it = start;
		//System.out.println("Agent: " + myself.getValue());
		while(it<start+clustersize){
			preference.add(((StandardEntity) preferences.get(it)).getID());
			it++;
			//System.out.println(preferences.get(it).getID().getValue());
		}
		return preference;
	}

	public void processUpdates() {
		for (Human human : changed) {
			processPositionUpdate(human);
		}
		changed.clear();
	}

	/**
	 * @param human
	 */
	private void processPositionUpdate(Human human) {
		// System.out.println(human.getFullDescription());
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Starting position updated for "
					+ human.getFullDescription());
		}
		if (human.isPositionDefined()) {
			EntityID positionID = human.getPosition();

			StandardEntity se = worldModel.getEntity(positionID);
			// System.out.println(se.getFullDescription());
			if (se instanceof Building) {

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(human + " is on building "
							+ se.getFullDescription());
				}

				boolean XYdefined = false;
				if (human.isXDefined() && human.isYDefined()) {
					PositionXY buildingLocation = new PositionXY(se
							.getLocation(worldModel));
					PositionXY humanLocation = new PositionXY(human
							.getLocation(worldModel));

					double distance = buildingLocation
							.distanceTo(humanLocation);

					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace(human + " is " + distance
								+ " from building " + se);
					}

					XYdefined = distance <= THRESHOLD * viewDistance;
					// System.out.println("view dist: "
					// + buildingLocation.distanceTo(humanLocation)
					// + XYdefined + viewDistance);
				}

				if (XYdefined) {
					safeUnsearchedBuildings.remove(positionID);
					modulatedSafeUnsearchedBuildings.remove(positionID);
					safeHighBuildings.remove(positionID);
					
					// Don't need the following, but in case there is
					// inconsistent
					// information, do it anyway:
					unknownBuildings.remove(positionID);
					modulatedUnknownBuildings.remove(positionID);
					unknownHighBuildings.remove(positionID);

					Building b = (Building) se;
					b.removeEntityListener(this);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * rescuecore2.worldmodel.EntityListener#propertyChanged(rescuecore2.worldmodel
	 * .Entity, rescuecore2.worldmodel.Property, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void propertyChanged(Entity e, Property p, Object oldValue,
			Object newValue) {
		if (e instanceof Building && !(e instanceof Refuge)) {
			if (p.getURN().equals(StandardPropertyURN.FIERYNESS.toString())
					&& p.isDefined()) {
				unknownBuildings.remove(e.getID());
				boolean removed2 =unknownHighBuildings.remove(e.getID());
				boolean removed = modulatedUnknownBuildings.remove(e.getID());

				if (((Integer) newValue).intValue() > 0) {
					safeUnsearchedBuildings.remove(e.getID());
					modulatedSafeUnsearchedBuildings.remove(e.getID());
					safeHighBuildings.remove(e.getID());
					e.removeEntityListener(this);
				} else {
					safeUnsearchedBuildings.add(e.getID());
					if(removed2){
						safeHighBuildings.add(e.getID());
					}
					if(removed){
						modulatedSafeUnsearchedBuildings.add(e.getID());
					}
				}
			}
		} else if (e instanceof Human) {
			if (p.getURN().equals(StandardPropertyURN.X.toString())
					|| p.getURN().equals(StandardPropertyURN.Y.toString())) {
				enqueueUpdated((Human) e);
			}
		}
		/*
		 * || p.getURN().equals( StandardPropertyURN.POSITION.toString())) {
		 * 
		 * LOGGER.trace(p);
		 * 
		 * IProvenanceInformation infoX = worldModel.getProvenance(e .getID(),
		 * StandardPropertyURN.X.toString());
		 * 
		 * LOGGER.trace("X info " + infoX);
		 * 
		 * if (infoX == null) { return; }
		 * 
		 * int xTime = infoX.getLatest().getTimeStep();
		 * 
		 * LOGGER.trace("X time " + xTime);
		 * 
		 * IProvenanceInformation infoY = worldModel.getProvenance(e .getID(),
		 * StandardPropertyURN.Y.toString());
		 * 
		 * LOGGER.trace("Y info " + infoY);
		 * 
		 * if (infoY == null) { return; }
		 * 
		 * int yTime = infoY.getLatest().getTimeStep();
		 * 
		 * LOGGER.trace("Y time " + yTime);
		 * 
		 * IProvenanceInformation infoPos = worldModel.getProvenance(e .getID(),
		 * StandardPropertyURN.POSITION.toString());
		 * 
		 * LOGGER.trace("Pos info " + infoPos);
		 * 
		 * if (infoPos == null) { return; }
		 * 
		 * int posTime = infoPos.getLatest().getTimeStep();
		 * 
		 * LOGGER.trace("Pos time " + posTime);
		 */

	}

	/**
	 * @param e
	 */
	private void enqueueUpdated(Human human) {
		changed.add(human);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.belief.IBuildingSearchUtility#getSafeUnsearchedBuildings()
	 */
	@Override
	public Collection<EntityID> getSafeUnsearchedBuildings() {
		return Collections.unmodifiableCollection(safeUnsearchedBuildings);
	}
	
	@Override
	public Collection<EntityID> getModulatedSafeUnsearchedBuildings() {
		return Collections.unmodifiableCollection(modulatedSafeUnsearchedBuildings);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.belief.IBuildingSearchUtility#getUnknownBuildings()
	 */
	@Override
	public Collection<EntityID> getUnknownBuildings() {
		return Collections.unmodifiableCollection(unknownBuildings);
	}
	
	@Override
	public Collection<EntityID> getModulatedUnknownBuildings() {
		return Collections.unmodifiableCollection(modulatedUnknownBuildings);
	}
	
	@Override
	public Collection<EntityID> getSafeHigh() {
		//return null;
		return Collections.unmodifiableCollection(safeHighBuildings);
	}
	
	@Override
	public Collection<EntityID> getUnknownHigh() {
		//return null;
		return Collections.unmodifiableCollection(unknownHighBuildings);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see iamrescue.agent.ITimeStepListener#notifyTimeStepStarted(int)
	 */
	@Override
	public void notifyTimeStepStarted(int timeStep) {
		processUpdates();
	}

	/*
	 * adds these buildings to the high priority list
	 */
	public void addHighPriorityBuildings(Collection<StandardEntity> buildings) {
		for(Iterator<StandardEntity> it = buildings.iterator(); it.hasNext();){
			StandardEntity s = it.next();
			if(s instanceof Building){
				if(((Building) s).isFierynessDefined()){
					if(((Building)s).getFieryness()==0){
						//building isnt on fire
						// is it searched already?
						if(safeUnsearchedBuildings.contains(s.getID()) ){
							safeHighBuildings.add(s.getID());
						} else if(unknownBuildings.contains(s.getID()) ){
							unknownHighBuildings.add(s.getID());
						}
					}
				}
			}
		}
		
	}

}
