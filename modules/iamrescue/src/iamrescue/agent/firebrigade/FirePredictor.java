package iamrescue.agent.firebrigade;

import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.spatial.ISpatialIndex;
import iamrescue.belief.spatial.SpatialIndex;
import iamrescue.belief.spatial.SpatialQuery;
import iamrescue.belief.spatial.SpatialQueryFactory;
import iamrescue.util.PositionXY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityListener;
import rescuecore2.worldmodel.Property;

/**
 * This class represents our current model of fire spreading. We build a model
 * that can measure the importance of fires, and clusters them into different
 * fire sites.
 * 
 * @version 0.1
 * @author fmdf08r
 * 
 */
public class FirePredictor implements EntityListener {
	private HeatTransferGraph heatTransferGraph;
	private IAMWorldModel model;

//	private BuildingImportanceModel importanceModel;
	private FastImportanceModel importanceModel;

	// new parameters
	private FastSet<Building> buildingsOnFire;

	private List<FireSite> fireSites;

	/**
	 * Constructor : initialise all the important data structures
	 * 
	 * @param model
	 *            the current world model
	 * 
	 */
	public FirePredictor(IAMWorldModel model) {
		this.model = model;
		heatTransferGraph = new HeatTransferGraph(model);
//		importanceModel = new BuildingImportanceModel(model, heatTransferGraph);
		importanceModel = new FastImportanceModel(model, heatTransferGraph);

		registerBuildingListeners(model);

		// new parameters
		fireSites = new ArrayList<FireSite>();
		buildingsOnFire = new FastSet<Building>();
	}

	// /**
	// *
	// * @return buildings of the world model
	// */
	// public HashSet<Building> getAllBuildings(){
	// return allBuildings;
	// }
	//
	// /**
	// *
	// * @return burn out buildings
	// *
	// */
	// public HashSet<Building> getBurntOutBuildings(){
	// return burntOutBuildings;
	// }

	//	
	// public HashSet<Building> getBuildingsThatMightBeOnFire(IAMWorldModel wm){
	// HashSet<Building> mightBeOnFire = new HashSet<Building>();
	// for (StandardEntity e:
	// wm.getEntitiesOfType(StandardEntityURN.BUILDING)){
	//			
	// Building b = (Building)e;
	// if (!burntOutBuildings.contains(b)){
	// mightBeOnFire.add(b);
	// }
	// }
	// return mightBeOnFire;
	// }

	/**
	 * TODO CHECK IF IT WORKS!!!
	 * 
	 * @param model
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

	public void update(int time) {
		updateFireSites(time);

//		mergeFireSites(time);
	}

	public void merge(int time) {
		mergeFireSites(time);
	}

	private void mergeFireSites(int time) {
		ArrayList<FireSite> newFireSites = new ArrayList<FireSite>();
		ArrayList<FireSite> erasedFireSites = new ArrayList<FireSite>();

		for (FireSite fireSite : fireSites) {
			if (erasedFireSites.contains(fireSite))
				continue;

			for (FireSite otherSite : fireSites) {
				if (erasedFireSites.contains(otherSite))
					continue;

				if (fireSite.equals(otherSite))
					continue;

				if (fireSite.intersects(otherSite)) {
					// merge the two fireSites
					FireSite newSite = merge(time, fireSite, otherSite);

					if (!newFireSites.contains(newSite)) {
						newFireSites.add(newSite);

						if (!erasedFireSites.contains(fireSite))
							erasedFireSites.add(fireSite);

						if (!erasedFireSites.contains(otherSite))
							erasedFireSites.add(otherSite);
					}
				}
			}
		}

		fireSites.removeAll(erasedFireSites);
		fireSites.addAll(newFireSites);
	}

	private FireSite merge(int time, FireSite fireSite, FireSite otherSite) {

		Collection<Building> buildings = fireSite.getBuildingsOnFire();

		buildings.addAll(otherSite.getBuildingsOnFire());

		Building center = computeCenter(buildings);

		FireSite newFireSite = new FireSite(center, heatTransferGraph, model);

		newFireSite.update(time);

		return newFireSite;
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

		FastMap xMap = new FastMap<Integer, FastMap<Integer, Building>>();

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

	private void updateFireSites(int time) {

		// FIRST: we update the FireSites
		for (FireSite fireSite : fireSites)
			fireSite.update(time);

		// SECOND: we look for new FireSites:
		for (Building building : buildingsOnFire) {

			int firecounter = fireSites.size();
			for (FireSite fireSite : fireSites)
				if (!fireSite.containsBuilding(building))
					firecounter--;

			if (firecounter == 0) {

				// This is a NEW FIRE SITE STARTING
				FireSite newSite = new FireSite(building,this.heatTransferGraph, this.model);
				newSite.update(time);

				fireSites.add(newSite);
			}
		}
	}

	public void updateImportanceModel() {
		importanceModel.update();
	}

	/**
	 * 
	 * @param t
	 *            represents the number of time steps in the future
	 * @return
	 */
	public FirePredictor predictFires(int time) {
		FirePredictor firePredictor = this.copy();

		firePredictor.predictFireSites(time);

		firePredictor.mergeFireSites(time);

		return firePredictor;
	}

	private void predictFireSites(int time) {
		for (FireSite fSite : fireSites) {
			fSite.predictFireArrays(time);
		}
	}

	public FirePredictor copy() {
		FirePredictor firePredictorCopy = new FirePredictor(model);

		// no need for deep copy
		firePredictorCopy.heatTransferGraph = heatTransferGraph;
		firePredictorCopy.importanceModel = importanceModel;
		firePredictorCopy.buildingsOnFire = buildingsOnFire;

		firePredictorCopy.fireSites = cloneFireSites();

		return firePredictorCopy;
	}

	private List<FireSite> cloneFireSites() {
		List<FireSite> result = new ArrayList<FireSite>();

		for (FireSite fireSite : fireSites) {
			try {
				result.add((FireSite) fireSite.clone());
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return result;
	}

	protected Map<Double, Set<Building>> getImportances(
			Collection<Building> fireBuildings) {

		Map<Double, Set<Building>> pairs = new FastMap<Double, Set<Building>>();

		for (Building b : fireBuildings) {
			double importance = importanceModel.getContextImportance(b);
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

	protected List<Building> getNumberOfMostImportantBuildingsToExtinguish(
			Map<Double, Set<Building>> importances, int numberOf) {
		ArrayList<Building> topBuildings = new ArrayList<Building>();
		ArrayList<Double> sortedImportances = new ArrayList<Double>(importances
				.keySet());

		Collections.sort(sortedImportances);
		for (Double importance : sortedImportances) {
			Set<Building> buildings = importances.get(importance);
			for (Building b : buildings) {
				if (topBuildings.size() < numberOf) {
					topBuildings.add(b);
				}
			}
			if (topBuildings.size() == numberOf) {
				break;
			}
		}
		return topBuildings;
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

	// public static boolean isValidTarget(Building b){
	//		
	// if(b == null){
	// System.out.println("FIRE MODEL: isValidTarget failed b = null");
	// return false;
	// }
	//		
	// if (b.getFierynessProperty().getValue() != null && b.getFieryness() ==
	// 8){
	// System.out.println("FIRE MODEL: isValidTarget failed fieryness = 8");
	// return false;
	// }
	//		
	// if (! b.isOnFire()){
	// System.out.println("FIRE MODEL: isValidTarget failed b is not on fire");
	// return false;
	// }
	//		
	// if (b.isOnFire() && b.getIgnitionProperty().getValue() != null &&
	// b.getFieryness() == 8){
	// System.out.println("FIRE MODEL: isValidTarget failed b is not on fire");
	// return false;
	// }
	//		
	// // if getignition is not undefined and is set to false
	// if (b.getIgnitionProperty().getValue() != null && ! b.getIgnition()){
	// System.out.println("FIRE MODEL: isValidTarget failed not ignited");
	// return false;
	// }
	//		
	// System.out.println("FIRE MODEL: isValidTarget passed");
	// return true;
	// }

	public List<Building> getBuildingsToExtinguish(int noRequired,
			int numberOfStepsAhead) {

		/**
		 * How about we make it MYOPIC??
		 * 
		 * Do not do any prediction...
		 */
		FirePredictor predictedFires = predictFires(numberOfStepsAhead); // update
		// model
		// to
		// the
		// future

		Collection<Building> buildingsOnFire = predictedFires
				.getAllPredictedBuildingsOnFire();
		Map<Double, Set<Building>> importances = getImportances(buildingsOnFire);
		List<Building> buildingsToExtinguish = getNumberOfMostImportantBuildingsToExtinguish(
				importances, noRequired);

		return buildingsToExtinguish;
	}

	private Collection<Building> getAllPredictedBuildingsOnFire() {
		ArrayList<Building> predictedBuildingsOnFire = new ArrayList<Building>();

		for (FireSite fireSite : fireSites) {
			predictedBuildingsOnFire.addAll(fireSite.getBuildingsOnFire());
		}

		return predictedBuildingsOnFire;
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
			Building building = (Building) e;

			if (building.isFierynessDefined()) {
				if (building.getFieryness() >= 1) {
					if (building.getFieryness() <= 3) {
						buildingsOnFire.add(building);
					}
				} else {
					buildingsOnFire.remove(building);
				}
			}
		}

	}

	public IAMWorldModel getWorldModel() {
		return model;
	}

	public HeatTransferGraph getHeatTransferGraph() {
		return heatTransferGraph;
	}

//	public BuildingImportanceModel getImportanceModel() {
//		return importanceModel;
//	}
	public FastImportanceModel getImportanceModel() {
		return importanceModel;
	}

	
	public void addBuildingsOnFire(FastSet<Building> buildingsOnFire2) {
		this.buildingsOnFire.addAll(buildingsOnFire2);
	}

	public List<FireSite> getFireSites() {
		return fireSites;
	}

	public void setFireSites(List<FireSite> fireSites) {
		this.fireSites = fireSites;
	}

	public FastSet<Building> getBuildingsOnFire() {
		return buildingsOnFire;
	}

	public FastSet<Building> getBurnOutBuildings() {
		return buildingsOnFire;
	}
}
