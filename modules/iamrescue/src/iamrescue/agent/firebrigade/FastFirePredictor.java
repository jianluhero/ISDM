package iamrescue.agent.firebrigade;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.provenance.IProvenanceInformation;
import iamrescue.belief.provenance.ProvenanceLogEntry;

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
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.EntityListener;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.WorldModel;
import rescuecore2.worldmodel.WorldModelListener;

public class FastFirePredictor implements EntityListener,
		WorldModelListener<StandardEntity> {
	private HeatTransferGraph heatTransferGraph;
	private IAMWorldModel model;

	private static final int TIME_STEP_DISCOUNT = 500;
	private static final double DISCOUNT_FACTOR = 0.95;

	private int totalCivilians = 0;

	private static final Logger LOGGER = Logger
			.getLogger(IAMStrategyFireBrigade.class);

	private FastImportanceModel importanceModel;

	// new parameters
	private FastSet<Building> buildingsOnFire;

	private List<FastFireSite> fireSites;
	private ISimulationTimer timer;
	private boolean weMerge = false;
	private EntityID myID;
	private double[] discountPowers;

	/**
	 * Constructor : initialise all the important data structures
	 * 
	 * @param model
	 *            the current world model
	 * 
	 */
	public FastFirePredictor(ISimulationTimer timer, IAMWorldModel model,
			EntityID myID) {
		this.model = model;
		this.timer = timer;
		this.myID = myID;

		heatTransferGraph = new HeatTransferGraph(model);

		importanceModel = new FastImportanceModel(model, heatTransferGraph);

		registerBuildingListeners(model);

		// new parameters
		fireSites = new ArrayList<FastFireSite>();
		buildingsOnFire = new FastSet<Building>();

		discountPowers = new double[TIME_STEP_DISCOUNT];

		discountPowers[0] = 1;
		discountPowers[1] = DISCOUNT_FACTOR;

		for (int i = 2; i < discountPowers.length; i++) {
			if (discountPowers[i - 1] == 0) {
				discountPowers[i] = 0;
			} else {
				discountPowers[i] = discountPowers[i - 1] * DISCOUNT_FACTOR;
			}
		}

		totalCivilians = model.getEntitiesOfType(StandardEntityURN.CIVILIAN)
				.size();
		model.addWorldModelListener(this);
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
									break;
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

	public void updateImportanceModel() {
		importanceModel.update();
	}

	public List<FastFireSite> getFireSites() {
		return fireSites;
	}

	public Double getTotalImportance(List<Building> buildings) {
		Map<Double, Set<Building>> importances = this.getImportances(buildings);

		Double total = 0.0;
		Set<Double> imps = importances.keySet();
		for (Double i : imps) {
			Set<Building> bs = importances.get(i);
			total += i * bs.size();
		}
		return total;
	}

	private int getTimeLastObserved(Building b) {
		IProvenanceInformation provenance = model.getProvenance(b.getID(),
				StandardPropertyURN.FIERYNESS);
		if (provenance == null) {
			return -100;
		}

		ProvenanceLogEntry lastDefined = provenance.getLastDefined();

		if (lastDefined == null) {
			return -100;
		}

		return lastDefined.getTimeStep();
	}

	protected Map<Double, Set<Building>> getImportances(
			Collection<Building> fireBuildings) {

		Map<Double, Set<Building>> pairs = new FastMap<Double, Set<Building>>();

		for (Building b : fireBuildings) {

			double importance = calculateImportance(b);

			Set<Building> buildings;
			if (pairs.containsKey(importance)) {
				buildings = pairs.get(importance);
			} else {
				buildings = new HashSet<Building>();
			}
			buildings.add(b);
			pairs.put(importance, buildings);
		}
		return pairs;
	}

	public double calculateContextDependentImportance(Building b) {
		int civilianNumber = importanceModel.getCiviliansAroundBuilding(b);
		double civilianImportance = 100 * ((0.1 + civilianNumber) / (0.1 + totalCivilians));
		return civilianImportance;
	}
	
	public double calculateImportance(Building b) {
		Rectangle2D bounds = model.getBounds();

		double centerX = (bounds.getMinX() + bounds.getMaxX()) / 2;
		double centerY = (bounds.getMinY() + bounds.getMaxY()) / 2;

		double dx = b.getX() - centerX;
		double dy = b.getY() - centerY;
		double distanceFromCenter = Math.sqrt(dx * dx + dy * dy);
		if (distanceFromCenter < 1) {
			distanceFromCenter = 1;
		}

		/*
		 * int numberOfHeatNeighbours =
		 * heatTransferGraph.getNeighbouringBuildings(b).size();
		 * if(numberOfHeatNeighbours == 0){ numberOfHeatNeighbours = 1; }
		 */

		Pair<Integer, Integer> myLocation = model.getEntity(myID).getLocation(
				model);

		dx = myLocation.first() - b.getX();
		dy = myLocation.second() - b.getY();

		double distanceFromMe = Math.sqrt(dx * dx + dy * dy);
		double distanceSum = 10 * distanceFromCenter + distanceFromMe;

		

		double intrinsicImportance = importanceModel.getImportance(b);
		double civilianImportance= calculateContextDependentImportance(b);

		double importance = intrinsicImportance * civilianImportance
				/ distanceSum;

		/*
		 * double importance = importanceModel.getContextImportance(b) /
		 * distanceSum ;//* numberOfHeatNeighbours);
		 */
		int timeAgoLastObserved = timer.getTime() - getTimeLastObserved(b);

		double discountFactor;

		if (timeAgoLastObserved == 0) {
			discountFactor = 1;
		} else {
			if (timeAgoLastObserved < TIME_STEP_DISCOUNT) {
				discountFactor = discountPowers[timeAgoLastObserved];
			} else {
				if (discountPowers[TIME_STEP_DISCOUNT - 1] == 0) {
					discountFactor = 0;
				} else {
					discountFactor = Math.pow(DISCOUNT_FACTOR,
							timeAgoLastObserved);
				}
			}
		}

		importance *= discountFactor;

		return importance;
	}

	public List<Building> getOrderOfImportantBuildingsToExtinguish(
			List<Building> targetBuildings) {

		Map<Double, Set<Building>> importances = this
				.getImportances(targetBuildings);
		ArrayList<Building> orderedBuildings = new ArrayList<Building>();
		ArrayList<Double> sortedImportances = new ArrayList<Double>(importances
				.keySet());

		Collections.sort(sortedImportances);
		for (Double importance : sortedImportances) {
			Set<Building> buildings = importances.get(importance);
			for (Building b : buildings) {
				orderedBuildings.add(b);
			}
		}
		return orderedBuildings;
	}

	// public List<Building> getBuildingsToExtinguish(int noRequired, int
	// numberOfStepsAhead) {
	//
	// FirePredictor predictedFires = predictFires(numberOfStepsAhead); //
	// update
	//
	// Collection<Building> buildingsOnFire = predictedFires
	// .getAllPredictedBuildingsOnFire();
	// Map<Double, Set<Building>> importances = getImportances(buildingsOnFire);
	// List<Building> buildingsToExtinguish =
	// getNumberOfMostImportantBuildingsToExtinguish(
	// importances, noRequired);
	//
	// return buildingsToExtinguish;
	// }

	// private FireSite merge(int time, FireSite fireSite, FireSite otherSite) {
	//
	// Collection<Building> buildings = fireSite.getBuildingsOnFire();
	//
	// buildings.addAll(otherSite.getBuildingsOnFire());
	//
	// Building center = computeCenter(buildings);
	//
	// FireSite newFireSite = new FireSite(center, heatTransferGraph, model);
	//
	// newFireSite.update(time);
	//
	// return newFireSite;
	// }
	//
	//
	// private void updateFireSites(int time) {
	//
	// // FIRST: we update the FireSites
	// for (FireSite fireSite : fireSites)
	// fireSite.update(time);
	//
	// // SECOND: we look for new FireSites:
	// for (Building building : buildingsOnFire) {
	//
	// int firecounter = fireSites.size();
	// for (FireSite fireSite : fireSites)
	// if (!fireSite.containsBuilding(building))
	// firecounter--;
	//
	// if (firecounter == 0) {
	//
	// // This is a NEW FIRE SITE STARTING
	// FireSite newSite = new FireSite(building,this.heatTransferGraph,
	// this.model);
	// newSite.update(time);
	//
	// fireSites.add(newSite);
	// }
	// }
	// }
	//
	//
	// /**
	// *
	// * @param t
	// * represents the number of time steps in the future
	// * @return
	// */
	// public FirePredictor predictFires(int time) {
	// FirePredictor firePredictor = this.copy();
	//
	// firePredictor.predictFireSites(time);
	//
	// firePredictor.mergeFireSites(time);
	//
	// return firePredictor;
	// }
	//
	// private void predictFireSites(int time) {
	// for (FireSite fSite : fireSites) {
	// fSite.predictFireArrays(time);
	// }
	// }
	//
	// public FirePredictor copy() {
	// FirePredictor firePredictorCopy = new FirePredictor(model);
	//
	// // no need for deep copy
	// firePredictorCopy.heatTransferGraph = heatTransferGraph;
	// firePredictorCopy.importanceModel = importanceModel;
	// firePredictorCopy.buildingsOnFire = buildingsOnFire;
	//
	// firePredictorCopy.fireSites = cloneFireSites();
	//
	// return firePredictorCopy;
	// }
	//
	// private List<FireSite> cloneFireSites() {
	// List<FireSite> result = new ArrayList<FireSite>();
	//
	// for (FireSite fireSite : fireSites) {
	// try {
	// result.add((FireSite) fireSite.clone());
	// } catch (CloneNotSupportedException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	//
	// return result;
	// }
	//
	//
	// protected List<Building> getNumberOfMostImportantBuildingsToExtinguish(
	// Map<Double, Set<Building>> importances, int numberOf) {
	// ArrayList<Building> topBuildings = new ArrayList<Building>();
	// ArrayList<Double> sortedImportances = new ArrayList<Double>(importances
	// .keySet());
	//
	// Collections.sort(sortedImportances);
	// for (Double importance : sortedImportances) {
	// Set<Building> buildings = importances.get(importance);
	// for (Building b : buildings) {
	// if (topBuildings.size() < numberOf) {
	// topBuildings.add(b);
	// }
	// }
	// if (topBuildings.size() == numberOf) {
	// break;
	// }
	// }
	// return topBuildings;
	// }
	//
	//
	//
	//
	// private Collection<Building> getAllPredictedBuildingsOnFire() {
	// ArrayList<Building> predictedBuildingsOnFire = new ArrayList<Building>();
	//
	// for (FireSite fireSite : fireSites) {
	// predictedBuildingsOnFire.addAll(fireSite.getBuildingsOnFire());
	// }
	//
	// return predictedBuildingsOnFire;
	// }
	//
	//
	// public IAMWorldModel getWorldModel() {
	// return model;
	// }
	//
	//
	// // public BuildingImportanceModel getImportanceModel() {
	// // return importanceModel;
	// // }
	public FastImportanceModel getImportanceModel() {
		return importanceModel;
	}

	//
	//	
	// public void addBuildingsOnFire(FastSet<Building> buildingsOnFire2) {
	// this.buildingsOnFire.addAll(buildingsOnFire2);
	// }
	//
	//
	// public void setFireSites(List<FastFireSite> fireSites) {
	// this.fireSites = fireSites;
	// }
	//
	// public FastSet<Building> getBuildingsOnFire() {
	// return buildingsOnFire;
	// }
	//
	// public FastSet<Building> getBurnOutBuildings() {
	// return buildingsOnFire;
	// }

	@Override
	public void entityAdded(WorldModel<? extends StandardEntity> model,
			StandardEntity e) {
		if (e instanceof Civilian) {
			totalCivilians++;
		}
	}

	@Override
	public void entityRemoved(WorldModel<? extends StandardEntity> model,
			StandardEntity e) {
		if (e instanceof Civilian) {
			totalCivilians--;
		}
	}
}
