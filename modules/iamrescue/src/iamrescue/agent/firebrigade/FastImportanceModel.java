package iamrescue.agent.firebrigade;

import iamrescue.agent.AbstractIAMAgent;
import iamrescue.belief.IAMWorldModel;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;
import javolution.util.FastSet;

import org.apache.commons.collections15.BidiMap;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityListener;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.WorldModel;
import rescuecore2.worldmodel.WorldModelListener;
import rescuecore2.worldmodel.properties.EntityRefProperty;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import edu.uci.ics.jung.algorithms.matrix.GraphMatrixOperations;
import edu.uci.ics.jung.algorithms.util.Indexer;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class FastImportanceModel {
	// 2^T = look ahead
	private final int T = 3;
	private IAMWorldModel model;

	private static final int CIVILIAN_DISTANCE = 150000; // 150m

	// private SparseDoubleMatrix2D importanceCoefficientMatrix;
	private double[] importanceCoefficientArray;
	private double[] contextDependentBuildingImportance;
	private double[] civilianDependentBuildingImportance;

	private FastMap<Building, Integer> totalCiviliansAroundBuildingMap;
	private FastMap<Civilian, FastSet<Building>> totalBuildingsAroundCivilianMap;

	private BidiMap<Building, Integer> indexer;
	private SparseDoubleMatrix1D importanceLoss;

	private boolean doPowerCalc = true;
	private int totalBuildingsNumber;

	public FastImportanceModel(IAMWorldModel model, HeatTransferGraph graph) {
		long start = System.currentTimeMillis();

		this.model = model;
		DirectedSparseGraph<Building, HeatTransferRelation> heatTransferGraph = graph
				.getHeatTransferGraph();

		Map<HeatTransferRelation, Number> edgeWeight = new FastMap<HeatTransferRelation, Number>();

		for (HeatTransferRelation relation : heatTransferGraph.getEdges()) {
			edgeWeight.put(relation, relation.getHeatTransferRate());
		}

		Collection<Building> buildings = heatTransferGraph.getVertices();

		totalBuildingsNumber = buildings.size();

		indexer = Indexer.create(buildings);

		/**
		 * initialise the new maps
		 */
		totalCiviliansAroundBuildingMap = new FastMap<Building, Integer>();
		totalBuildingsAroundCivilianMap = new FastMap<Civilian, FastSet<Building>>();

		computeImportanceCoefficientMatrix(heatTransferGraph, edgeWeight);

		for (Building building : buildings) {
			/**
			 * update buildings map
			 */
			totalCiviliansAroundBuildingMap.put(building, new Integer(0));
		}

		for (StandardEntity entity : model
				.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
			//entity.addEntityListener(new CivilianEntityListener());

			/**
			 * update civilians map
			 */
			totalBuildingsAroundCivilianMap.put((Civilian) entity,
					new FastSet<Building>());
		}

		//model.addWorldModelListener(new CivilianAddedListener());

		update();

		long finish = System.currentTimeMillis();
		System.out.println("Building Building Importance Model Took "
				+ (finish - start));
	}

	/**
	 * 
	 * 
	 * @param edgeWeight
	 * @param heatTransferGraph
	 */
	private void computeImportanceCoefficientMatrix(
			DirectedSparseGraph<Building, HeatTransferRelation> heatTransferGraph,
			Map<HeatTransferRelation, Number> edgeWeight) {
		importanceLoss = new SparseDoubleMatrix1D(heatTransferGraph
				.getVertexCount());

		SparseDoubleMatrix2D importanceCoefficientMatrix = GraphMatrixOperations
				.graphToSparseMatrix(heatTransferGraph, edgeWeight);

		// Ensure that the columns sum to 1. I.e. "importance" is never lost
		// during value iteration. Otherwise, the importance transfer
		// coefficients will all be 0
		for (int i = 0; i < importanceCoefficientMatrix.columns(); i++) {
			double colSum = importanceCoefficientMatrix.viewColumn(i).zSum();
			importanceLoss.set(i, colSum);

			for (int j = 0; j < importanceCoefficientMatrix.rows(); j++) {
				importanceCoefficientMatrix.set(j, i,
						importanceCoefficientMatrix.get(j, i) / colSum);
			}
		}

		long start = System.currentTimeMillis();

		/**
		 * NO MORE POWER CALCULATION!!!
		 */
		if (doPowerCalc) {
			importanceCoefficientMatrix = (SparseDoubleMatrix2D) new Algebra()
					.pow(importanceCoefficientMatrix, T);
		}

		// scale the matrix to reintroduce importance loss
		for (int i = 0; i < importanceCoefficientMatrix.rows(); i++) {
			for (int j = 0; j < importanceCoefficientMatrix.columns(); j++) {
				importanceCoefficientMatrix.set(i, j,
						importanceCoefficientMatrix.get(i, j)
								* importanceLoss.get(j));
			}
		}

		importanceCoefficientArray = new double[totalBuildingsNumber];

		for (int i = 0; i < importanceCoefficientMatrix.rows(); i++)
			for (int j = 0; j < importanceCoefficientMatrix.columns(); j++)
				importanceCoefficientArray[i] += importanceCoefficientMatrix
						.get(i, j);

		long finish = System.currentTimeMillis();

		System.out.println(finish - start);
	}

	/**
	 * This method integrates the knowledge about the current position of the
	 * civilians with the importance Matrix
	 * 
	 * IT SHOULD BE CALLED AT EVERY TIME-STEP!!!
	 * 
	 */
	public void update() {
		contextDependentBuildingImportance = new double[importanceCoefficientArray.length];

		AbstractIAMAgent.stopIfInterrupted();

		computeCivilianImportanceArray();

		for (int i = 0; i < contextDependentBuildingImportance.length; i++) {
			contextDependentBuildingImportance[i] = importanceCoefficientArray[i]
					* civilianDependentBuildingImportance[i];
		}

		AbstractIAMAgent.stopIfInterrupted();

	}

	private void computeCivilianImportanceArray() {
		civilianDependentBuildingImportance = new double[importanceCoefficientArray.length];

		int totalCivilians = model
				.getEntitiesOfType(StandardEntityURN.CIVILIAN).size();

		for (Building building : totalCiviliansAroundBuildingMap.keySet()) {
			int index = indexer.get(building);

			double civilianNumber = totalCiviliansAroundBuildingMap
					.get(building);

			// civilianDependentBuildingImportance[index] =
			// building.getTotalArea()*((0.1 + civilianNumber )/ (0.1 +
			// totalCivilians));
			civilianDependentBuildingImportance[index] = 100 * ((0.1 + civilianNumber) / (0.1 + totalCivilians));
		}

	}
	
	public int getCiviliansAroundBuilding(Building b) {
		Integer civilians= totalCiviliansAroundBuildingMap.get(b);
		if (civilians == null) {
			return 0;
		} else {
			return civilians;
		}
	}
	

	private class CivilianAddedListener implements
			WorldModelListener<StandardEntity> {

		@Override
		public void entityAdded(WorldModel<? extends StandardEntity> model,
				StandardEntity e) {
			if (e instanceof Civilian) {
				totalBuildingsAroundCivilianMap.put((Civilian) e,
						new FastSet<Building>());
				CivilianEntityListener listener = new CivilianEntityListener();
				e.addEntityListener(listener);
				EntityRefProperty positionProperty = ((Civilian) e)
						.getPositionProperty();
				if (positionProperty.isDefined()) {
					listener.propertyChanged(e, positionProperty, null,
							positionProperty.getValue());
				}
			}
		}

		@Override
		public void entityRemoved(WorldModel<? extends StandardEntity> model,
				StandardEntity e) {

		}

	}

	private class CivilianEntityListener implements EntityListener {

		private Object lastValue = null;

		@Override
		public void propertyChanged(Entity e, Property p, Object oldValue,
				Object newValue) {

			if (lastValue == newValue) {
				return;
			} else {
				lastValue = null;
			}

			Civilian civilian = (Civilian) e;

			if (p.getURN().equals(StandardPropertyURN.HP.toString())) {
				if (civilian.isHPDefined())
					if (civilian.getHP() == 0) {
						// civilian has moved
						// StandardEntity newPosition =
						// model.getEntity(civilian.getPosition());

						// first thing we decrease the value of the buildings
						// that previously contained the building
						FastSet<Building> previousBuildings = totalBuildingsAroundCivilianMap
								.get(civilian);
						if (previousBuildings != null) {
							for (Building building : previousBuildings) {
								int newvalue = totalCiviliansAroundBuildingMap
										.get(building) - 1;

								totalCiviliansAroundBuildingMap.put(building,
										new Integer(newvalue));
							}
							totalBuildingsAroundCivilianMap.remove(civilian);
						}
					}
			}

			if (p.getURN().equals(StandardPropertyURN.POSITION.toString())) {
				// civilian has moved
				boolean needToIncreaseImportance = false;

				if (p.isDefined()) {
					StandardEntity newPosition = model.getEntity(civilian
							.getPosition());
					needToIncreaseImportance = (newPosition instanceof Building && !(newPosition instanceof Refuge));
				}

				// first thing we decrease the value of the buildings that
				// previously contained the building
				FastSet<Building> previousBuildings = totalBuildingsAroundCivilianMap
						.get(civilian);
				if (previousBuildings != null) {
					for (Building building : previousBuildings) {
						int newvalue = totalCiviliansAroundBuildingMap
								.get(building) - 1;

						totalCiviliansAroundBuildingMap.put(building,
								new Integer(newvalue));
					}
				}

				if (needToIncreaseImportance) {
					// we calculate the new set of buildings within range
					Collection<StandardEntity> BuildingsInRange = model
							.getObjectsInRange(civilian, CIVILIAN_DISTANCE);

					FastSet<Building> newSet = new FastSet<Building>();
					for (StandardEntity standardEntity : BuildingsInRange) {
						if (standardEntity instanceof Building) {
							Building building = (Building) standardEntity;
							// we add the element to the new set
							/*
							 * if (building.isFierynessDefined()) if
							 * (building.getFieryness() <= 3 &&
							 * building.getFieryness() >= 1) continue;
							 */

							newSet.add(building);

							// we increment the map of civilians per building
							int newvalue = totalCiviliansAroundBuildingMap
									.get(building) + 1;

							totalCiviliansAroundBuildingMap.put(building,
									new Integer(newvalue));
						}
					}
					// we can finally update the map
					totalBuildingsAroundCivilianMap.put(civilian, newSet);
				} else {
					totalBuildingsAroundCivilianMap.remove(civilian);
				}
			}
		}
	}

	public Set<Building> getBuildings() {
		return indexer.keySet();
	}

	public double getContextImportance(Building building) {
		int buildingIndex = indexer.get(building);
		return contextDependentBuildingImportance[buildingIndex];
	}

	public double getImportance(Building building) {
		int buildingIndex = indexer.get(building);
		return importanceCoefficientArray[buildingIndex];
	}
}
