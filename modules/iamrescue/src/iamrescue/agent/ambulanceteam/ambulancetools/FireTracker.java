package iamrescue.agent.ambulanceteam.ambulancetools;

import iamrescue.agent.ISimulationTimer;
import iamrescue.agent.ambulanceteam.IAMAmbulanceTeam;
import iamrescue.agent.firebrigade.FastFireSite;
import iamrescue.agent.firebrigade.FastImportanceModel;
import iamrescue.agent.firebrigade.HeatTransferGraph;
import iamrescue.agent.firebrigade.IAMStrategyFireBrigade;
import iamrescue.belief.IAMWorldModel;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;
import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.EntityListener;
import rescuecore2.worldmodel.Property;

public class FireTracker implements EntityListener {
	private HeatTransferGraph heatTransferGraph;
	private IAMWorldModel model;

	private static final Logger LOGGER = Logger
			.getLogger(IAMAmbulanceTeam.class);


	private List<FastFireSite> fireSites;
	private ISimulationTimer timer;
	private boolean weMerge = true;

	/**
	 * Constructor : initialise all the important data structures
	 * 
	 * @param model
	 *            the current world model
	 * 
	 */
	public FireTracker(ISimulationTimer timer, IAMWorldModel model) {
		this.model = model;
		this.timer = timer;

		heatTransferGraph = new HeatTransferGraph(model);

		registerBuildingListeners(model);

		// new parameters
		fireSites = new ArrayList<FastFireSite>();
	}

	/**
	 * register listeners to buildings
	 * 
	 * @param model
	 * 
	 */
	private void registerBuildingListeners(IAMWorldModel model) {
		// TODO only select buildings that can catch fire
		Collection<StandardEntity> entitiesOfType = model
				.getEntitiesOfType(StandardEntityURN.BUILDING,
						StandardEntityURN.REFUGE,
						StandardEntityURN.AMBULANCE_CENTRE,
						StandardEntityURN.POLICE_OFFICE,
						StandardEntityURN.FIRE_STATION);

		for (StandardEntity standardEntity : entitiesOfType) {
			standardEntity.addEntityListener(this);
		}
	}

	/**
	 * Update temperature (and thus energy) based on new measured (or
	 * communicated value)
	 */
	@Override
	public void propertyChanged(Entity e, Property p, Object oldValue,
			Object newValue) {

		/**
		 * add buildings on fire
		 */
		if (p.getURN().equals(StandardPropertyURN.FIERYNESS.toString())) {
			if (e instanceof Building) {
				Building building = (Building) e;

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(building.getFullDescription());
				}

				if (building.isFierynessDefined()) {
					// new building burning, add it to fireSites
					if (building.getFieryness() >= 1
							&& building.getFieryness() <= 3) {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("spotted a building on fire at time "
									+ timer.getTime() + " for: ");
						}

						if (belongsToAFireSite(building)) {
							// the building has already being considered
							if (LOGGER.isDebugEnabled()) {
								LOGGER
										.debug("building on fire belongs to a fire site");
							}
							return;
						}

						if (LOGGER.isDebugEnabled()) {
							LOGGER
									.debug("building does not belong to any fire site, we check its neighbours");
						}

						Collection<Building> neighbours = heatTransferGraph
								.getNeighbouringBuildings(building);

						List<FastFireSite> containedFireSites = atLeastOneBelongsToAFireSite(neighbours);
						if (containedFireSites != null) {
							if (containedFireSites.size() == 1) {
								// new building to be added to a fireSite
								if (LOGGER.isDebugEnabled()) {
									LOGGER
											.debug("building belongs only to a fire site we add it");
								}

								containedFireSites.get(0).addBuildingOnFire(
										building);
								return;
							}

							// neighbours contained in more than one fireSite
							// they need to be merged
							if (weMerge) {
								if (LOGGER.isDebugEnabled()) {
									LOGGER
											.debug("building belongs to many fire sites we need to merge them");
								}
								merge(containedFireSites, building);
								return;
							} else {
								// we decide not to merge fireBuildings
								for (FastFireSite fastFireSite : containedFireSites) {
									fastFireSite.addBuildingOnFire(building);
								}
								return;
							}

						} else {
							// it is a NEW ISOLATED FIRESITE
							if (LOGGER.isDebugEnabled()) {
								LOGGER
										.debug("building on fire belongs to a NEW fire site");
							}

							fireSites.add(new FastFireSite(timer, building,
									model, heatTransferGraph));
						}
					} else {
						if (building.getFieryness() == 8) {
							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug("building has burnt out");
							}
							removeFromFireSites(building);
						}
					}
				}
			}
		}
	}

	private void merge(List<FastFireSite> containedFireSites, Building building) {
		List<FastFireSite> newFastFireSites = new ArrayList<FastFireSite>();
		
		// first we remove all the fireSites that need to be merged
		fireSites.removeAll(containedFireSites);
		// we add the remaining fireSites to the new lits
		
		newFastFireSites.addAll(fireSites);

		// now we merge the fireSites
		// 1 - we create a new set of buildings
		Map<EntityID, Building> allBuildings = new FastMap<EntityID, Building>();
		// 2 - we add the building that is contained in all the previous
		// fireSites
		allBuildings.put(building.getID(), building);
		// 3 - we add to this map all the building on fire
		for (FastFireSite fastFireSite : containedFireSites) {
			allBuildings.putAll(fastFireSite.getBuildingsOnFire());
		}
		// 4 - we compute the center of this set
		Building center = computeCenter(allBuildings.values());
		// 5 - we create the new fireSite
		FastFireSite mergedFireSite = new FastFireSite(timer, center, model,
				heatTransferGraph);
		// 6 - we add to it the new Buildings
		mergedFireSite.setBuildingsOnFire(allBuildings);
		// 7 - we update the fireSites
		mergedFireSite.updateArraysSpeed(containedFireSites);

		newFastFireSites.add(mergedFireSite);
		// 6 - we update the fireSites list
		fireSites = newFastFireSites;
	}

	private void removeFromFireSites(Building building) {
		for (FastFireSite fireSite : fireSites) {
			fireSite.removeBuilding(building);
		}
	}

	private List<FastFireSite> atLeastOneBelongsToAFireSite(
			Collection<Building> neighbours) {
		List<FastFireSite> containedFireSites = new ArrayList<FastFireSite>();

		for (Building building : neighbours) {
			for (FastFireSite fireSite : fireSites) {

				if (fireSite.containsBuilding(building))
					if (!containedFireSites.contains(fireSite))
						containedFireSites.add(fireSite);
			}
		}

		if (containedFireSites.size() == 0)
			return null;

		return containedFireSites;
	}

	private boolean belongsToAFireSite(Building building) {
		for (FastFireSite fireSite : fireSites)
			if (fireSite.containsBuilding(building))
				return true;

		return false;
	}

	/**
	 * this method finds the center of a Set of Buildings by computing the the
	 * median of all the buildings to be considered
	 * 
	 * @param buildings
	 *            the buildings for which we need the centre
	 * 
	 * @return centre of the set of buildings
	 * 
	 */
	public Building computeCenter(Collection<Building> buildings) {
		Building center = null;

		FastMap<Integer, FastMap<Integer, Building>> xMap = new FastMap<Integer, FastMap<Integer, Building>>();

		for (Building building : buildings) {
			FastMap<Integer, Building> yMap = (FastMap<Integer, Building>) xMap
					.get(new Integer(building.getX()));

			if (yMap == null) {
				yMap = new FastMap<Integer, Building>();
				yMap.put(new Integer(building.getY()), building);

				xMap.put(new Integer(building.getX()), yMap);
			} else
				yMap.put(new Integer(building.getY()), building);
		}

		// we can now sort everything
		ArrayList<Integer> xs = new ArrayList<Integer>(xMap.keySet());

		Collections.sort(xs);

		int xMapIndex = xs.size() / 2;

		// int xMapIndex = (int) Math.floor(xIndex);

		if (xs.size() % 2 == 0) {
			xMapIndex--;
		}

		FastMap<Integer, Building> medianSet = (FastMap<Integer, Building>) xMap
				.get(xs.get(xMapIndex));

		ArrayList<Integer> ys = new ArrayList<Integer>(medianSet.keySet());

		Collections.sort(ys);

		int yMapIndex = ys.size() / 2;

		if (ys.size() % 2 == 0) {
			yMapIndex--;
		}

		center = medianSet.get(ys.get(yMapIndex));

		return center;
	}

	public HeatTransferGraph getHeatTransferGraph() {
		return heatTransferGraph;
	}

	public List<FastFireSite> getFireSites() {
		return fireSites;
	}
}
