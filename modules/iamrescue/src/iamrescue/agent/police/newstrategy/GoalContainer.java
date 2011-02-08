/**
 * 
 */
package iamrescue.agent.police.newstrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javolution.util.FastList;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;

/**
 * @author Sebastian
 * 
 */
public class GoalContainer {
	private List<PoliceForce> blockedPolice;
	private List<FireBrigade> blockedFireBrigade;
	private List<AmbulanceTeam> blockedAmbulance;
	private List<Civilian> blockedCivilians;
	private List<Building> blockedUnsearchedBuildings;
	private List<Building> blockedBurningBuildings;

	public GoalContainer() {
		blockedPolice = new FastList<PoliceForce>();
		blockedFireBrigade = new FastList<FireBrigade>();
		blockedAmbulance = new FastList<AmbulanceTeam>();
		blockedCivilians = new FastList<Civilian>();
		blockedUnsearchedBuildings = new FastList<Building>();
		blockedBurningBuildings = new FastList<Building>();
	}

	/*
	 * public void addAgent(Human human) { blockedAgents.add(human); }
	 */

	public void addCivilian(Civilian civilian) {
		blockedCivilians.add(civilian);
	}

	public void addBurningBuilding(Building building) {
		blockedBurningBuildings.add(building);
	}

	public void addUnsearchedBuilding(Building building) {
		blockedUnsearchedBuildings.add(building);
	}

	public void clear() {
		blockedAmbulance.clear();
		blockedPolice.clear();
		blockedFireBrigade.clear();
		blockedBurningBuildings.clear();
		blockedCivilians.clear();
		blockedUnsearchedBuildings.clear();
	}

	public List<AmbulanceTeam> getBlockedAmbulance() {
		return blockedAmbulance;
	}

	public List<FireBrigade> getBlockedFireBrigade() {
		return blockedFireBrigade;
	}

	public List<PoliceForce> getBlockedPolice() {
		return blockedPolice;
	}

	/**
	 * @return the blockedBurningBuildings
	 */
	public List<Building> getBlockedBurningBuildings() {
		return Collections.unmodifiableList(blockedBurningBuildings);
	}

	/**
	 * @return the blockedCivilians
	 */
	public List<Civilian> getBlockedCivilians() {
		return Collections.unmodifiableList(blockedCivilians);
	}

	/**
	 * @return the blockedUnsearchedBuildings
	 */
	public List<Building> getBlockedUnsearchedBuildings() {
		return Collections.unmodifiableList(blockedUnsearchedBuildings);
	}

	public void addPoliceForce(PoliceForce agent) {
		blockedPolice.add(agent);
	}

	public void addAmbulanceTeam(AmbulanceTeam agent) {
		blockedAmbulance.add(agent);
	}

	public void addFireBrigade(FireBrigade agent) {
		blockedFireBrigade.add(agent);
	}

	public void addAgent(Human human) {
		if (human instanceof FireBrigade) {
			addFireBrigade((FireBrigade) human);
		} else if (human instanceof AmbulanceTeam) {
			addAmbulanceTeam((AmbulanceTeam) human);
		} else {
			addPoliceForce((PoliceForce) human);
		}
	}

	public List<Human> getBlockedAgents() {
		List<Human> list = new ArrayList<Human>();
		list.addAll(blockedAmbulance);
		list.addAll(blockedPolice);
		list.addAll(blockedFireBrigade);
		return list;
	}

}
