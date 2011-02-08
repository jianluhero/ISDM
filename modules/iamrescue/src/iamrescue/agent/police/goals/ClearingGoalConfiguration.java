package iamrescue.agent.police.goals;

import iamrescue.belief.IAMWorldModel;
import iamrescue.routing.IRoutingModule;

import java.util.Collection;

import javolution.util.FastList;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public class ClearingGoalConfiguration {
	private boolean useShortestDistanceOnly;
	private IRoutingModule testRouting;
	private IRoutingModule clearingRouting;
	private Collection<EntityID> refuges;
	private IAMWorldModel worldModel;

	public ClearingGoalConfiguration(IRoutingModule testRouting,
			IRoutingModule clearingRouting, IAMWorldModel worldModel,
			boolean useShortestDistanceOnly) {
		this.useShortestDistanceOnly = useShortestDistanceOnly;
		this.testRouting = testRouting;
		this.clearingRouting = clearingRouting;
		Collection<StandardEntity> refugeEntities = worldModel
				.getEntitiesOfType(StandardEntityURN.REFUGE);
		refuges = new FastList<EntityID>();
		for (StandardEntity refuge : refugeEntities) {
			refuges.add(refuge.getID());
		}
		this.worldModel = worldModel;
	}

	/**
	 * @return the useShortestDistanceOnly
	 */
	public boolean isUseShortestDistanceOnly() {
		return useShortestDistanceOnly;
	}

	/**
	 * @param useShortestDistanceOnly
	 *            the useShortestDistanceOnly to set
	 */
	public void setUseShortestDistanceOnly(boolean useShortestDistanceOnly) {
		this.useShortestDistanceOnly = useShortestDistanceOnly;
	}

	/**
	 * @return the testRouting
	 */
	public IRoutingModule getTestRouting() {
		return testRouting;
	}

	/**
	 * @return the clearingRouting
	 */
	public IRoutingModule getClearingRouting() {
		return clearingRouting;
	}

	/**
	 * @return the refuges
	 */
	public Collection<EntityID> getRefuges() {
		return refuges;
	}

	/**
	 * @return the worldModel
	 */
	public IAMWorldModel getWorldModel() {
		return worldModel;
	}

}
