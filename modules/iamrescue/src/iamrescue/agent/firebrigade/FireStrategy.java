package iamrescue.agent.firebrigade;

import iamrescue.execution.command.IPath;
import iamrescue.routing.IRoutingModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import javolution.util.FastMap;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.worldmodel.EntityID;

public class FireStrategy {

	private List<FireBrigade> fireBrigadeAgents;
	private IRoutingModule pathPlanner;
	private static final Logger LOGGER = Logger.getLogger(FireStrategy.class);
	protected EntityID me;
	protected IAMStrategyFireBrigade strategyBrigade;
	
	public FireStrategy(List<FireBrigade> fireBrigadeAgents, 
			IRoutingModule pathPlanner, EntityID me, IAMStrategyFireBrigade strategyBrigade){
		this.fireBrigadeAgents = fireBrigadeAgents;
		this.pathPlanner = pathPlanner;
		this.me = me;
		this.strategyBrigade = strategyBrigade;
	}
	
	public Building oneBuildingAtATime(FireBrigade me, 
//			ArrayList<ArrayList<Building>> fireSite, ArrayList<ArrayList<Building>> centreBuildings, FirePredictor fireModel){
			ArrayList<ArrayList<Building>> fireSite, ArrayList<ArrayList<Building>> centreBuildings, FastFirePredictor fireModel){
		
		if (LOGGER.isDebugEnabled()) { LOGGER.debug("oneBuildingAtATime, fireSite is: "+fireSite.toString());}
		if (LOGGER.isDebugEnabled()) { LOGGER.debug("oneBuildingAtATime, centreBuildings is: "+centreBuildings.toString());}
		Building b = null;
		if(fireSite.isEmpty() || fireSite.get(0).isEmpty()){
			if (LOGGER.isDebugEnabled()) { LOGGER.debug("oneBuildingAtATime, fireSite empty, so sending centre.");}
			b = this.oneBuildingAtATime(me, centreBuildings, fireModel);
		}else{
			if (LOGGER.isDebugEnabled()) { LOGGER.debug("oneBuildingAtATime, fireSite not empty, so sending fireSite.");}
			b = this.oneBuildingAtATime(me, fireSite, fireModel);
		}
		return b;
	}
	
	//fireSite is the predicted buildings
	private Building oneBuildingAtATime(FireBrigade me, 
//			ArrayList<ArrayList<Building>> fireSite, FirePredictor fireModel){
		ArrayList<ArrayList<Building>> fireSite, FastFirePredictor fireModel){
		
		if (LOGGER.isDebugEnabled()) { LOGGER.debug("oneBuildingAtATime, fireSite is: "+fireSite.toString());}
		
		ArrayList<ArrayList<Building>> fireSites = 
			this.orderFireSites(fireSite, me, fireModel);

		if (LOGGER.isDebugEnabled()) { LOGGER.debug("oneBuildingAtATime, orderedFireSites is: "+fireSites.toString());}
		for(ArrayList<Building> buildings : fireSites){
			for(Building b: buildings){
				if (LOGGER.isDebugEnabled()) { LOGGER.debug("Checking FireStrategy to see if agent "+me.getID()+" can real building "+b.getID());}
				if(this.canIReachTarget(b, me.getID())){
					return b;
				}
			}
		}
		if (LOGGER.isDebugEnabled()) { LOGGER.debug("Agent "+me.getID()+" could not reach any buildings.");}
		return null;	
	}

	private String lastAllocationHash = null;
	private FastMap<FireBrigade, Building> oneAllocatedBuildings = null;
	
	public Building oneBuildingEach(FireBrigade me, 
//			ArrayList<ArrayList<Building>> targetBuildings, ArrayList<ArrayList<Building>> centreBuildings, FirePredictor fireModel){
		ArrayList<ArrayList<Building>> targetBuildings, ArrayList<ArrayList<Building>> centreBuildings, FastFirePredictor fireModel){

		Building b = null;
		if(targetBuildings.isEmpty() || targetBuildings.get(0).isEmpty()){
			b = this.oneBuildingEach(me, centreBuildings, fireModel);
		}else{
			b = this.oneBuildingEach(me, targetBuildings, fireModel);
		}
		return b;
	}
	
	private Building oneBuildingEach(FireBrigade me, 
//			ArrayList<ArrayList<Building>> targetBuildings, FirePredictor fireModel){
		ArrayList<ArrayList<Building>> targetBuildings, FastFirePredictor fireModel){
		
		if (LOGGER.isDebugEnabled()) { LOGGER.debug("oneBuildingEach with firesites: "+targetBuildings.toString());}
		
		if (targetBuildings.isEmpty()){
			return null;
		}
		
		// if the firesites have changed, reallocate
		if (lastAllocationHash != null){
			String thisAllocationHash = targetBuildings.toString();
			if (!thisAllocationHash.equals(lastAllocationHash)){
				oneAllocatedBuildings = null;
			}
			lastAllocationHash = thisAllocationHash;
		}
		
		// always reallocate, since agents might have moved!
		oneAllocatedBuildings = null;

		
		if(oneAllocatedBuildings == null){
			ArrayList<ArrayList<Building>> fireSites = 
				this.orderFireSites(targetBuildings, me, fireModel);
			
			oneAllocatedBuildings = new FastMap<FireBrigade, Building>();

			ArrayList<FireBrigade> unallocatedFireBrigades = new ArrayList<FireBrigade>();
			for (FireBrigade fb: fireBrigadeAgents){
				unallocatedFireBrigades.add(fb);
			}

			FireBrigade nextfb = unallocatedFireBrigades.remove(0);
			boolean avoidinf = false;
			int tried = 0; // how many buildings we have tried to allocate this building to.
			
			ArrayList<Building> fireSite = fireSites.remove(0); // pull the first fire site out
			UNALLOCLOOP: while(!unallocatedFireBrigades.isEmpty()){
				
				for (Building b: fireSite){
					if (this.canIReachTarget(b, nextfb.getID())){
						oneAllocatedBuildings.put(nextfb, b);
						avoidinf = false; // reset the infinite loop avoider if we match
						
						if (unallocatedFireBrigades.isEmpty()){
							break UNALLOCLOOP;
						} else {
							nextfb = unallocatedFireBrigades.remove(0);
							tried = 0;
						}
						continue UNALLOCLOOP;
					} else {
						
						tried++;
						if (tried >= fireSite.size()){
							// we can try the other fire sites.
							for (ArrayList<Building> altFireSite: fireSites){
								
								for (Building altB: altFireSite){
									if (this.canIReachTarget(altB, nextfb.getID())){
										oneAllocatedBuildings.put(nextfb, b);
										
										if (unallocatedFireBrigades.isEmpty()){
											break UNALLOCLOOP;
										} else {
											nextfb = unallocatedFireBrigades.remove(0);
											tried = 0;
											avoidinf = false; // reset the infinite loop avoider if we match
											continue UNALLOCLOOP; // go back to the other fire site
										}
									}
								}
							}
							// couldn't allocate :(
							nextfb = unallocatedFireBrigades.remove(0);
							avoidinf = false;
							tried = 0;
							continue UNALLOCLOOP; // go back to the other fire site
						}
					}
				}
				
				if (avoidinf){
					if (LOGGER.isDebugEnabled()) { LOGGER.debug("hit infinite lopp avoider");}
					if (unallocatedFireBrigades.isEmpty()){
						break UNALLOCLOOP;
					} else {
						nextfb = unallocatedFireBrigades.remove(0);
						tried = 0;
						avoidinf = false;
					}
				} else {
					avoidinf = true; // only allow one loop through for each agent, to avoid infinite loop
				}
			}
		}
		
		if (LOGGER.isDebugEnabled()) { LOGGER.debug("oneBuildingEach allocations are:"+oneAllocatedBuildings.toString()+", where we are: "+me.toString());}
		Building b = oneAllocatedBuildings.get(me);
		return b;
	}
	
	
	public Building teamDivide(FireBrigade me, 
			ArrayList<ArrayList<Building>> targetBuildings, 
			ArrayList<ArrayList<Building>> centreBuildings, 
			List<FireBrigade> fireBrigades, 
//			FirePredictor fireModel){
		FastFirePredictor fireModel){

		Building b = null;
		
		// if we assigned both teams to the same target, check if the targets are the same, and then return the previous target building.
		if (this.singleTarget){
			String thisTarget = targetBuildings.toString() + centreBuildings.toString();
			if (thisTarget.equals(this.singleTargetPrevious)){
				return previousSingleTarget;
			}
			singleTargetPrevious = thisTarget;
		}

		// if the target (perimeter) buildings are empty, assign the centre of the fire site to the agents.
		if(targetBuildings.isEmpty() || targetBuildings.get(0).isEmpty()){
			b = this.teamDivide(me, centreBuildings, fireBrigades, fireModel);
		}else{
			b = this.teamDivide(me, targetBuildings, fireBrigades, fireModel);
		}
		return b;
	}
	
	private List<FireBrigade> sortFireBrigades(List<FireBrigade> brigades){
		FastMap<Integer, FireBrigade> brigadeMap = new FastMap<Integer, FireBrigade>();
		List<Integer> ids = new ArrayList<Integer>();
		for (FireBrigade fb : brigades){
			brigadeMap.put(fb.getID().getValue(), fb);
			ids.add(fb.getID().getValue());
		}
		List<FireBrigade> sorted = new ArrayList<FireBrigade>();
		Collections.sort(ids);
		for(Integer id: ids){
			sorted.add(brigadeMap.get(id));
		}
		return sorted;
	}

	private Building lastTeamDivideBuilding = null;
	
	private boolean singleTarget = false; // if the teamDivide assigns both teams to the same fireSite, this flags to true and we will check to reassign next timestep.
	private String singleTargetPrevious = "";
	private Building previousSingleTarget = null;
	
	/**
 	*Method divides the fireBrigade parameter into two teams and tries to put out the two highest rated
 	*fire sites that it can reach 
 	* @param myFireBrigadeID
 	* @param targetBuildings
 	* 
 	*/
	private Building teamDivide(FireBrigade me, 
			ArrayList<ArrayList<Building>> targetBuildings, List<FireBrigade> fireBrigades, 
//			FirePredictor fireModel){
		FastFirePredictor fireModel){
		
		// numeric sort the fire brigades so the team assignments are always the same
		fireBrigades = sortFireBrigades(fireBrigades);
		
		
		// assign teams, odd and even
		ArrayList<EntityID> evenTeam = new ArrayList<EntityID>();
		ArrayList<EntityID> oddTeam = new ArrayList<EntityID>();			
		for(int i = 0; i<fireBrigades.size(); i++){
			this.strategyBrigade.stopIfInterrupted();
			if(i % 2 == 0){
				evenTeam.add(fireBrigades.get(i).getID());
			}else{
				oddTeam.add(fireBrigades.get(i).getID());
			}
		}
			
		
		// rank order the fire sites according to importance
		ArrayList<ArrayList<Building>> fireSites = 
			this.orderFireSites(targetBuildings, me, fireModel);

		
		// split firesites to even and odd.
		ArrayList<ArrayList<Building>> evenFireSites = new ArrayList<ArrayList<Building>>();
		ArrayList<ArrayList<Building>> oddFireSites = new ArrayList<ArrayList<Building>>();
		if (LOGGER.isDebugEnabled()) { LOGGER.debug("teamDivide, orderedFireSites is: "+fireSites.toString());}
		int fireSiteCount = -1;
		for(ArrayList<Building> buildings : fireSites){
			this.strategyBrigade.stopIfInterrupted();
			fireSiteCount++;
			if (fireSiteCount % 2 == 0){
				evenFireSites.add(buildings);
			} else {
				oddFireSites.add(buildings);
			}
		}

		// send both teams to the same firesite if there is only one.
		singleTarget = false;
		if (oddFireSites.size() == 0){
			oddFireSites = evenFireSites; // if there's only one fire site, both go to it.
			singleTarget = true;
		} else if (evenFireSites.size() == 0){
			evenFireSites = oddFireSites;
			singleTarget = true;
		}

		
		// figure out which team we are on, and use that firesite now
		ArrayList<ArrayList<Building>> fireSitesNow = null;
		ArrayList<EntityID> thisteam = null;
		if(evenTeam.contains(me.getID())){
			fireSitesNow = evenFireSites;
			thisteam = evenTeam;
		} else {
			fireSitesNow = oddFireSites;
			thisteam = oddTeam;
		}

		
		// get a disposable list of fire brigades
		ArrayList<EntityID> unallocatedFireBrigades = new ArrayList<EntityID>();
		for (EntityID fb: thisteam){
			this.strategyBrigade.stopIfInterrupted();
			unallocatedFireBrigades.add(fb);
		}

		boolean avoidinf = false;
		int tried = 0; // how many buildings we have tried to allocate this agent to.
		boolean startAllocating = false;
		FastMap<EntityID, Building> allocatedBuildings = new FastMap<EntityID, Building>();
		
		if (fireSitesNow.size() == 0){
			return null;
		}
		
		if (lastTeamDivideBuilding == null){
			startAllocating = true;
		}
		
		if (LOGGER.isDebugEnabled()) { LOGGER.debug("starting allocations of agents: "+unallocatedFireBrigades);}
		
		
		// this loops over firesites and buildings, assigning fire brigades to buildings that they can reach.
		ArrayList<Building> fireSite = fireSitesNow.remove(0); // pull the first fire site out
		EntityID nextfb = unallocatedFireBrigades.remove(0);
		UNALLOCLOOP: while(true){
			this.strategyBrigade.stopIfInterrupted();
			if (LOGGER.isDebugEnabled()) { LOGGER.debug("start of UNALLOCLOOP with agent "+nextfb);}
			if (LOGGER.isDebugEnabled()) { LOGGER.debug("unallocatedAgents is: "+unallocatedFireBrigades);}
			
			// this firesite is the ideal chosen one based on the rank
			for (Building b: fireSite){
				this.strategyBrigade.stopIfInterrupted();
				if (LOGGER.isDebugEnabled()) { LOGGER.debug("start of firesite loop with building: "+b.getID());}

				if (startAllocating == false && b.getID() == lastTeamDivideBuilding.getID()){
					if (LOGGER.isDebugEnabled()) { LOGGER.debug("hit previous last allocation, so starting to allocate now.");}
					startAllocating = true;
					continue UNALLOCLOOP;
				}
				
				if (this.canIReachTarget(b, nextfb)){
					if (LOGGER.isDebugEnabled()) { LOGGER.debug("Agent "+nextfb+" can reach Building "+b.getID()+", allocating");}
					
					allocatedBuildings.put(nextfb, b);
					lastTeamDivideBuilding = b;
					avoidinf = false; // reset the infinite loop avoider if we match
					
					if (unallocatedFireBrigades.isEmpty()){
						if (LOGGER.isDebugEnabled()) { LOGGER.debug("unallocatedFireBrigades is empty, exiting");}
						break UNALLOCLOOP;
					} else {
						nextfb = unallocatedFireBrigades.remove(0);
						if (LOGGER.isDebugEnabled()) { LOGGER.debug("getting next agent: "+nextfb);}
						tried = 0;
						continue UNALLOCLOOP;
					}
				} else {
					
					
					if (LOGGER.isDebugEnabled()) { LOGGER.debug("Agent "+nextfb+" could not reach Building "+b.getID());}					
					tried++;
					if (tried >= fireSite.size()){
						
						// when we have tried to assign to all buildings in the main fire site,
						// we can try the other fire sites.
						for (ArrayList<Building> altFireSite: fireSitesNow){
							this.strategyBrigade.stopIfInterrupted();
							if (LOGGER.isDebugEnabled()) { LOGGER.debug("Trying to allocated Agent "+nextfb+" to alt Fire Site: "+altFireSite.toString());}
							
							for (Building altB: altFireSite){
								this.strategyBrigade.stopIfInterrupted();
								if (this.canIReachTarget(altB, nextfb)){
									if (LOGGER.isDebugEnabled()) { LOGGER.debug("Agent "+nextfb+" can reach alt Building "+altB.getID()+", allocating");}
									allocatedBuildings.put(nextfb, altB);
									lastTeamDivideBuilding = altB;
									
									if (unallocatedFireBrigades.isEmpty()){
										break UNALLOCLOOP;
									} else {
										nextfb = unallocatedFireBrigades.remove(0);
										tried = 0;
										avoidinf = false; // reset the infinite loop avoider if we match
										continue UNALLOCLOOP; // go back to the other fire site
									}
								} else {
									if (LOGGER.isDebugEnabled()) { LOGGER.debug("Agent "+nextfb+" could not reach alt Building "+altB.getID());}
								}
							}
						}
						if (LOGGER.isDebugEnabled()) { LOGGER.debug("Agent "+nextfb+" will not be allocated.");}
						// couldn't allocate :(
						nextfb = unallocatedFireBrigades.remove(0);
						avoidinf = false;
						tried = 0;
						continue UNALLOCLOOP; // go back to the other fire site
					}
				}
			}
			
			
			// this stops the loop from iterating infinitely, if it runs over the whole thing more than once, we give up assigning and move onto the next agent
			if (avoidinf){
				if (LOGGER.isDebugEnabled()) { LOGGER.debug("hit infinite loop avoider");}
				if (unallocatedFireBrigades.isEmpty()){
					break UNALLOCLOOP;
				} else {
					if (LOGGER.isDebugEnabled()) { LOGGER.debug("so skipping this agent: "+nextfb);}

					nextfb = unallocatedFireBrigades.remove(0);
					tried = 0;
					avoidinf = false;
				}
			} else {
				avoidinf = true; // only allow one loop through for each agent, to avoid infinite loop
			}
		}

		
		if (LOGGER.isDebugEnabled()) { LOGGER.debug("Allocation is: "+allocatedBuildings.toString());}
		
		this.strategyBrigade.stopIfInterrupted();
		
		// return our assigned building back to the agent as the target
		previousSingleTarget = allocatedBuildings.get(me.getID());
		return allocatedBuildings.get(me.getID());
	}
	
	private FireStrategy fireSiteStrategy = null;
	private FireStrategy perimeterStrategy = null;
	private ArrayList<FireBrigade> fireSiteTeam = null;
	private ArrayList<FireBrigade> perimeterTeam = null;
	/**
	 * Uses the oneBuildingEach method on two sets of target buildings, the on-fire buildings and the surrounding buildings.
	 * @param me
	 * @param targetBuildings
	 * @param fireSiteBuildings
	 * @param fireBrigades
	 */
	public Building halfAndHalf(FireBrigade me, 
			ArrayList<ArrayList<Building>> targetBuildings, ArrayList<ArrayList<Building>> fireSiteBuildings, List<FireBrigade> fireBrigades, 
//			FirePredictor fireModel){
		FastFirePredictor fireModel){
		
		if (targetBuildings.isEmpty() || targetBuildings.get(0).isEmpty()){
			targetBuildings = fireSiteBuildings;
		}
		
		FastMap<Integer, HashSet<FireBrigade>> possibleSites = new FastMap<Integer, HashSet<FireBrigade>>();
		
		int counter = 0;
		for(List<Building> fireSite: targetBuildings){
			for(Building b :fireSite){
				for(FireBrigade fireBrigade : fireBrigades){
					if(this.canIReachTarget(b, fireBrigade.getID())){
						if(possibleSites.containsKey(counter)){
							HashSet<FireBrigade> fireBs = possibleSites.get(counter);
							fireBs.add(fireBrigade);
							possibleSites.put(counter, fireBs);
						}else{
							HashSet<FireBrigade> fireBs = new HashSet<FireBrigade>();
							fireBs.add(fireBrigade);
							possibleSites.put(counter, fireBs);
						}
					}
				}
			}
			counter++;
		}
		
		// select which firesite strikes a balance between being important and
		// being reachable by many agents.
		final float agentCoefficient = 0.5f;
		FastMap<Integer, Float> firesiteWeightedRankings = new FastMap<Integer, Float>(); // fireSite to Ranking
		
		Integer topFireSite = null; // best fire site
		float topFireSiteRank = Float.MIN_VALUE;
		for(Integer fireSite : possibleSites.keySet()){
			HashSet<FireBrigade> agents = possibleSites.get(fireSite);
			int numAgents = agents.size();
			
			float rankWeight = fireSiteBuildings.size() - fireSite;
			float weightedRanking = (float)numAgents*agentCoefficient + rankWeight;
			
			if (weightedRanking > topFireSiteRank){
				topFireSite = fireSite;
			}
			firesiteWeightedRankings.put(fireSite, weightedRanking);
		}

		if (topFireSite == null){ // if there are no fires to pick from, return null
			return null;
		}
		
		
		// get a list of fire brigades at the top fire site.
		ArrayList<FireBrigade> topSiteFireBrigades = new ArrayList<FireBrigade>();
		for(FireBrigade fireBrigade: possibleSites.get(topFireSite)){
			topSiteFireBrigades.add(fireBrigade);
		}
		
		if (fireSiteStrategy == null || perimeterStrategy == null ){ // allocate teams
			fireSiteTeam = new ArrayList<FireBrigade>();
			perimeterTeam = new ArrayList<FireBrigade>();

			for(int i = 0; i<topSiteFireBrigades.size(); i++){
				if(i % 2 == 0){
					fireSiteTeam.add(topSiteFireBrigades.get(i));
				}else{
					perimeterTeam.add(topSiteFireBrigades.get(i));
				}
			}
			fireSiteStrategy = new FireStrategy(fireSiteTeam, this.pathPlanner, this.me, this.strategyBrigade);
			perimeterStrategy = new FireStrategy(perimeterTeam, this.pathPlanner, this.me, this.strategyBrigade);
		}
		
		if (fireSiteTeam.contains(me)){
			return fireSiteStrategy.oneBuildingEach(me, fireSiteBuildings, fireModel);
		} else {
			return perimeterStrategy.oneBuildingEach(me, targetBuildings, fireModel);
		}
	}
	
	/**
	 * Checks if a FB agent can reach a target building
	 * @param b
	 * @param fireBrigadeAgents
	 * @return
	 */
	private boolean canIReachTarget(Building b, EntityID me){
		// if we're checking the agent that called this, then do a real test
		if (me.getValue() == this.me.getValue()){
			if (LOGGER.isDebugEnabled()) { LOGGER.debug("Seeing if we can get to building "+b.getID()+" from our location.");}
			
			IPath path = pathPlanner.findShortestPath(me, b.getID());
			if (path.isValid()){
				return true;
			}

			// path isn't valid, so we have to try to go to any of the destinations neighbours
			IPath newPath = pathPlanner.findShortestPath(me, b.getNeighbours());
			if (newPath.isValid()){
				return true;
			}
			
			return false;
		}
		
		// otherwise just say YES so that the other agents get assigned
		return true;
	}

	
	/**
	 * Returns a list of FB agents that can reach the specified building given 
	 * a set of FB agents
	 * @param b
	 * @param fireBrigadeAgents
	 * @return
	 */
	private List<FireBrigade> getFBAgentsThatCanReachBuilding(Building b, 
			List<FireBrigade> fireBrigadeAgents){
		List<FireBrigade> validFireBrigadeAgents = 
			new ArrayList<FireBrigade>();
		for(FireBrigade fireBrigade : fireBrigadeAgents){
			
			
			
			EntityID locationID = fireBrigade.getPosition();
			IPath path = pathPlanner.findShortestPath(locationID, b.getID());
			if (path.isValid()){
				validFireBrigadeAgents.add(fireBrigade);
			}
		}
		return validFireBrigadeAgents;
	}
	
	/**
	 * This method returns an ArrayList of sorted fire sites, and the buildings
	 * within the fire sites are also ordered by their importance
	 * 
	 * @param targetBuildings
	 * @param fBAgent
	 * @return
	 */
	private ArrayList<ArrayList<Building>> orderFireSites(ArrayList<ArrayList<Building>> 
		targetBuildings, FireBrigade fBAgent, 
//		FirePredictor fireModel){
		FastFirePredictor fireModel){
	
		if (LOGGER.isDebugEnabled()) { LOGGER.debug("ordering the fire sites: "+targetBuildings.toString());}
		
		FastMap<Double, List<Building>> fireSitesAndImportance = new FastMap
			<Double, List<Building>>();
		
		//get the total the importances of all predicted buildings on fire
		for(List<Building> fireSite : targetBuildings){
			Double totalImp = fireModel.getTotalImportance(fireSite);
			fireSitesAndImportance.put(totalImp, fireSite);
		}
		
		//get all the importances of the fire sites so we can loop through them
		ArrayList<Double> sortedFireSiteImportances = new ArrayList<Double>(
				fireSitesAndImportance.keySet());
		Collections.sort(sortedFireSiteImportances);
		
		ArrayList<ArrayList<Building>> orderedFireSites = new 
			ArrayList<ArrayList<Building>>();
		
		//loop through the fire sites in order of their importance
		for(Double fireSiteImportance: sortedFireSiteImportances){
			ArrayList<Building> fSite = (ArrayList<Building>) 
				fireSitesAndImportance.get(fireSiteImportance);
			
			//sort the buildings on their importance
			ArrayList<Building> orderedBuildings = (ArrayList<Building>) 
				fireModel.getOrderOfImportantBuildingsToExtinguish(fSite);
			orderedFireSites.add(orderedBuildings);
		}
		
		//return the array sorted by the most important fires and then by the 
		//most important fires
		if (LOGGER.isDebugEnabled()) { LOGGER.debug("ordered: "+orderedFireSites);}
		return orderedFireSites;
	}

	public boolean needToReassign() {
		if (this.singleTarget){ // first condition to force a reassignment is if both teams are assigned to a single fireSite.
			return true;
		}

		return false;
	}
	
}
