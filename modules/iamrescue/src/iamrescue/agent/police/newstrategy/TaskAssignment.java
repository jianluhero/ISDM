package iamrescue.agent.police.newstrategy;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.routing.util.ISpeedInfo;
import iamrescue.util.comparators.IDComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rescuecore2.config.Config;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public class TaskAssignment {

	// Police forces to indices in list
	private Map<EntityID, Integer> policeToIndices;

	// Targets to sets of indices
	private Map<EntityID, Set<Integer>> targetToIndices;

	// List of police forces
	private List<EntityID> policeForces;

	// List of target of each police force
	private List<List<EntityID>> targets;

	// Predicted target completion times
	private Map<EntityID, Integer> completionTimes;

	private IAMWorldModel worldModel;

	private FutureClearingRoutingModule routing;

	public TaskAssignment(IAMWorldModel worldModel, Config config,
			ISimulationTimer timer, ISpeedInfo speedInfo) {

		this.worldModel = worldModel;

		// Get all police agents
		Collection<StandardEntity> policeCollection = worldModel
				.getEntitiesOfType(StandardEntityURN.POLICE_FORCE);
		policeForces = new ArrayList<EntityID>();
		for (StandardEntity police : policeCollection) {
			policeForces.add(police.getID());
		}

		// Sort by ID
		Collections.sort(policeForces, new IDComparator());

		routing = new FutureClearingRoutingModule(config, timer, worldModel,
				speedInfo);
	}

	public void calculateAssignment() {

	}

}
