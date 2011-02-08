package iamrescue.agent.ambulanceteam.ambulancetools;

import iamrescue.agent.AbstractIAMAgent;
import iamrescue.agent.ISimulationTimer;
import iamrescue.agent.ambulanceteam.IAMAmbulanceTeam;
import iamrescue.agent.firebrigade.FastFireSite;
import iamrescue.belief.IAMWorldModel;
import iamrescue.execution.command.IPath;
import iamrescue.routing.IRoutingModule;
import iamrescue.routing.util.ISpeedInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Random;
import java.lang.Integer;
import java.lang.Math;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

public class RScheduler  
{
	/**
	 * Variable to print out comments
	 */
	private static final Logger LOGGER = Logger.getLogger(IAMAmbulanceTeam.class);
	
	/**
	 * The current time step
	 */
	private int timeStep;

	/**
	 * The memory of the agent using this scheduler
	 */
	private IAMWorldModel memory;

	/**
	 * The list of ids in my team
	 */
	private AmbulanceTeam[] team;

	/**2
	 * My index in the SORTED version of the team!!
	 */
	private int myIndex;

	/**
	 * List of all buildings
	 */
	private StandardEntity[] allBuildings;
	
	/**
	 * The avoid blockage path planner
	 */
	private IRoutingModule pathPlanner;//no used
	
	/**
	 * The victims from the previous time step
	 */
	//private ArrayList<RescueTask> oldVictims;
	
	/**
	 * The new Strategy: tasks to execute in the next time window
	 */
	public ArrayList<RescueTask> nextTasks;
	
	/**
	 * all well-know tasks
	 */
	private HashMap<EntityID, RescueTask> allTasks;

	/**
	 * Matrix of agent's ids: for each ambulance(row) is reported its strategy for the next time window
	 */
	public int[][] nextAllocations ;
	
	/**
	 * Number of all found civilians
	 */
	private int allVictimsSize;
	
	/**
	 * dimension of the decision window
	 */
	private int window=AladdinInterAgentConstants.ALLOCATION_WINDOW;
	
	/**
	 * Determines how frequently strategy should be recomputed
	 */
	private double ReAllocProb = 0.9;
	
	/**
	
	
	 * to obtain random number in [0,1] interval
	 */
	private Random generator;
	
	/*
	 * deadline function
	 */
	DeadlineFunction deadline = new DeadlineFunction();

	private ISpeedInfo speed;
	private FireTracker fires;
	private ISimulationTimer timer;


	/**
	 * Constructor for the RScheduler
	 * @param _oldVics - the previous set of victims - empty to start with
	 * @param mem - the memory of the agent
	 * @param planner the avoid blockage path planner
	 * @param _ignorePlanner the ignore blockage path planner
	 * @param _team list of ids in my team
	 * @param index my index in the SORTED version of the team ids
	 * @param time the current time step
	 * @param iSpeedInfo 
	 * @param iamAmbulanceTeam 
	 */
	public RScheduler(ArrayList<RescueTask> _oldVics, IAMWorldModel mem,StandardEntity[]_allBuildings, IRoutingModule planner, int time, ISpeedInfo iSpeedInfo, IAMAmbulanceTeam me, ISimulationTimer timer){
		//oldVictims = _oldVics;
		memory = mem;
		pathPlanner = planner;
		timeStep = time;
		allBuildings = _allBuildings;
		speed = iSpeedInfo;
		this.timer=timer;
		
		nextTasks=new ArrayList<RescueTask>();
		allTasks = new HashMap<EntityID, RescueTask>() ;
		
		allVictimsSize = 0;
		generator = new Random();
		
		fires = new FireTracker(timer, mem);
		
	}

	/**
	 * @return myIndex: the index of this agent in the "team" vector
	 */
	public int myIndex(){return myIndex;}
	
	/**
	 * This method returns the estimated deadline of a given victim with the assigned ambulances
	 * @param victim - the humanoid
	 * @return - the estimated deadline
	 */
	public double getVictimDeadline(Human victim)
	{
		return deadline.getDeadline(victim.getHP());
	}

	/**
	 * This method chooses the next victim to rescue.
	 * @return the next victim to rescue
	 */
	public Human chooseNextVictim()
	{
		Human nextVictim=null;
		if(nextTasks.size()>0)
		{
			nextVictim=nextTasks.get(0).getVictim();
			nextTasks.remove(0);//remove the next victim from the nextTasks vector
		}	
		return nextVictim;
	}
	
	/**
	 * This method chooses what the agent's strategy for the next 'window' time steps.
	 * where 'window' is ALLOCATION_WINDOW constant.
	 * @param allVictims - all found civilians
	 * @param time - current time
	 * @return true if there are some available task, false if nextTasks is empty
	 */
	public boolean chooseStrategy(ArrayList<Task> allVictims, int time)
	{
		timeStep = time;
		allVictimsSize = allVictims.size();

		if(allVictimsSize>0)//at least one task available
		{
			
			// update nextTasks vector
			if(nextTasks.size()>0 )
			{
				RescueTask thisTask = nextTasks.get(0);
				if(thisTask.victim.getBuriedness()==0)
					while(nextTasks.size()>0 && nextTasks.get(0) == thisTask)
						nextTasks.remove(0);
			}
			
			StandardEntity pos = memory.getEntity(team[myIndex].getID());
			
			for(Iterator<RescueTask> it=nextTasks.iterator(); it.hasNext();){
				AbstractIAMAgent.stopIfInterrupted();
				RescueTask task = it.next();
				if(!task.getVictim().isPositionDefined() || !task.getVictim().isXDefined() || !task.getVictim().isYDefined()){
					it.remove();
				} else if(memory.getEntity(task.getVictim().getPosition()) instanceof AmbulanceTeam && task.getVictim().getPosition().getValue() != team[myIndex].getID().getValue()){
					it.remove();
				} else if(task.getVictim().getHP()==0){
					it.remove();
				//} else if(!task.pathStillValid(team[myIndex].getID())){
				} else if(!task.pathStillValid(allVictims)) {
					it.remove();
				}
			}
			
			//recalculate the nextTasks vector with ReAllocProbability or if the agent is in a refuge or if nextTask vector is quite empty
			if(generator.nextDouble() < ReAllocProb || (pos instanceof Refuge) || nextTasks.size() < window*2.0/3)
			{
				if (LOGGER.isTraceEnabled())
					LOGGER.trace(" - calculate strategy victimsize:"+allVictimsSize);
				
				nextTasks.clear();//delete the old vector
				RescueTask task;
				Task t;
				Human victim;
				ArrayList<RescueTask> sortedAllTasks=new ArrayList<RescueTask>(); 

				/* This iterator traverses the list of victims sorted according to shortest deadline first.
				   It creates a RescueTask for each victim and calculates the utility for each task.*/
				for (Iterator<Task> it = allVictims.iterator();it.hasNext();)
				{
					AbstractIAMAgent.stopIfInterrupted();
					t= it.next();
					victim = t.civilian;
					if(allTasks.containsKey(victim.getID()))
					{
						task = (RescueTask)allTasks.get(victim.getID());
						task.path=t.path;
					}
					else
						task=new RescueTask(victim, t.path);
					task.setMarginalUtility(time);
					allTasks.put(victim.getID(), task);
					sortedAllTasks.add(task);
				}
				//tasks sorted according to bigger utility first
				Collections.sort(sortedAllTasks, new TaskComparator());
				//decides what it will do in the next time window
				int k=time+1;

				//calculate the vector of tasks to executed in the next time window
				for(Iterator<RescueTask> it=sortedAllTasks.iterator();it.hasNext() && k<time+1+window;)
				{
					AbstractIAMAgent.stopIfInterrupted();
					task = it.next();
					if(LOGGER.isTraceEnabled())
						LOGGER.trace(" saving victim: "+task.getVictim().getID()+" with utility: "+task.marginalUtility);
					int taskCompletionTime = (int)Math.ceil(task.getCompletionTime(time,true)); 
					while(k<=taskCompletionTime && k<time+1+window)
					{
						nextTasks.add(task);
						k++;
					}
				}
				if(LOGGER.isTraceEnabled() && k<time+1+window)
					LOGGER.trace(" - my stategy is shorter that window");
			}
			else //UPDATE the nextTasks vector: add a task, which is choosen in random way
			{
				RescueTask randomTask = null;
				for(int tries=0;randomTask == null && tries < 5;tries++)
					randomTask = (RescueTask)allTasks.get((allVictims.get(generator.nextInt(allVictims.size()))).civilian.getID());
				while(nextTasks.size()<window && randomTask != null) 
					nextTasks.add(randomTask);
				if (LOGGER.isTraceEnabled())
					LOGGER.trace(" - update strategy victimsize:"+allVictimsSize);
			}

			updateNextAllocations();
			if(LOGGER.isTraceEnabled())
				printMyAllocation();
			return true;
		}
		else
		{//no tasks available
			if (LOGGER.isTraceEnabled())
				LOGGER.trace(" - in chooseStrategy victimsize:"+allVictimsSize);
			return false;
		}
	}

	
	/**
	 * Add the travel time to the strategy of this ambulance
	 * @return strategy - strategy updating with travel time too
	 */
	private ArrayList<Integer> getStrategyWithTravelTime()
	{
		ArrayList<Integer> strategy=new ArrayList<Integer>();
		RescueTask previousTask = null;
		RescueTask thisTask = null;
		int thisTaskID;
		int travelling = 0;
		
		for(Iterator<RescueTask>  it = nextTasks.iterator(); it.hasNext();)
		{
			thisTask = it.next();
			if(thisTask == null)
				continue;
			if(thisTask != previousTask)
			{		
				previousTask = thisTask;
				travelling =  (int)Math.ceil(thisTask.travelTime());
			}
			thisTaskID = thisTask.getVictim().getID().getValue();
			if(travelling==0)
				strategy.add(thisTaskID);
			else
			{
				strategy.add(-1);
				travelling--;
			}
		}
		
		// if enough tasks are not there, then add -2 until window which means i'm travelling because I haven't anything to do
		while(strategy.size()<window)
			strategy.add(-2);
		return strategy;
	}
	
	/**
	 * Add this ambulance'id to the strategy of this ambulance, in order to send the strategy to othe ambulances
	 * @return strategy - ambulance'id + strategy updating with travel time
	 */
	public int[] getStrategyToSend()
	{
		ArrayList<Integer> strategyWithTravelTime=new ArrayList<Integer>();
		strategyWithTravelTime.addAll(getStrategyWithTravelTime());
		Object[] ar = strategyWithTravelTime.toArray();
		int[] ret = new int[ar.length];
		for(int i=0; i<ret.length; i++){
			ret[i] = (Integer) ar[i];
		}
		return ret;
	}
	
	public int[] encodeStrategyToSend(){
		int[] strategy = getStrategyToSend();
		
		//encode this as an alternating array of tasks and durations
		//-1 -1 -1 123 123 123 123 -1 -1 456 456 456 -2 -2 -2 -2 would become
		//-1 3 123 4 -1 2 456 3 -2 4
		ArrayList<Integer> encoded = new ArrayList<Integer>();
		int current = strategy[0];
		int time = 1;
		for(int i=1; i<strategy.length; i++){
			if(strategy[i]==current){
				time++;
			} else {
				encoded.add(current);
				encoded.add(time);
				current=strategy[i];
				time=1;
			}
		}
		encoded.add(current);
		encoded.add(time);
		int[] ret = new int[encoded.size()];
		for(int i=0; i<ret.length; i++){
			ret[i]=encoded.get(i);
		}
		
		return ret;
	}
	
	public int getMyTeamIndex(int agent){
		for(int i=0; i<team.length; i++){
			if(agent == team[i].getID().getValue()){
				return i;
			}
		}
		return Integer.MAX_VALUE;
	}
	
	public int[] decodeStrategy(int[] encoded){
		ArrayList<Integer> decoded = new ArrayList<Integer>();
		for(int i=0; i<encoded.length;i++){
			for(int j=0; j<encoded[i+1]; j++){
				decoded.add(encoded[i]);
			}
			i++;
		}
		int[] ret = new int[decoded.size()];
		for(int i=0; i<ret.length; i++){
			ret[i]=decoded.get(i);
		}
		return ret;
	}

	/**
	 * Update the nextAllocations matrix.
	 * It writes the ids of next victims to rescue or -1(for travel time) in the row (myIndex) .
	 */
	private void updateNextAllocations()
	{
		ArrayList<Integer> strategyWithTravelTime=getStrategyWithTravelTime();
		int nextTime = 0;

		for(Iterator<Integer> it = strategyWithTravelTime.iterator();it.hasNext() && nextTime<window;)
		{
			nextAllocations[myIndex][nextTime] = it.next();
			nextTime++;
		}
	}
	
	
	/**
	 * This method  the next Victims to rescue
	 * 
	 */
	private void printMyAllocation()
	{
		String output = " nextStrategy: ";
		for(int i=0; i<nextTasks.size() ;i++)
			output = output + nextTasks.get(i).getVictim().getID()+" ";
		output = output+ "\nnextAllocations: ";
		for(int i=0;i<nextAllocations[myIndex].length;i++)
			output = output + nextAllocations[myIndex][i]+" ";
		LOGGER.trace(output);
	}	
	
	/**
	 * Compute distance between victim and ambulance based on direct distance
	 * @param victim
	 * @param ambulance
	 * @return distance in mm
	 */
	private double computePathDistance(RescueTask victim, StandardEntity ambulance)
	{
		if(LOGGER.isTraceEnabled()){
			LOGGER.trace(" in path distance " + ambulance.getID().getValue() + " " + victim.getVictim().getID().getValue());
			LOGGER.trace(" locations " + ((Human) ambulance).getPosition().getValue()+ " " +  victim.getVictim().getPosition().getValue());
		}
		/*IPath oldPath = victim.path;
		IPath path;
		if(victim.getVictim().getPosition().getValue()==oldPath.getLocations().get(oldPath.getLocations().size()-1).getValue() &&
				((Human)ambulance).getPosition().getValue()==oldPath.getLocations().get(0).getValue()){
			//path is still valid
			path=oldPath;
		} else {
			//its not;
			path= pathPlanner.findShortestPath(ambulance.getID(),victim.getVictim().getID());
			this.ambulance.pathCount++;
			if(LOGGER.isTraceEnabled())
				LOGGER.trace(" PATH:setting new path in compute distance");
			victim.path=path;
		}*/
		double cost =  speed.getTimeToTravelPath(victim.path);
		if(LOGGER.isTraceEnabled()){
			LOGGER.trace(" time to travel is " + cost);
		}
		return cost;
	}
	
	
	/**
	 * Contains the victim and the assigned ambulances. 
	 * This class is used by the scheduler to keep track of allocated tasks.
	 */
	public class RescueTask
	{
		/**
		 * the victim to rescue
		 */
		private Human victim;
		
		/**
		 * path at start
		 */
		public IPath path;
		
		/**
		 * d.factor to calculate utility functions
		 */
		private double beta=0.9;
		
		/**
		 * the ambulance's utility
		 */
		private double marginalUtility;
		
		/**
		 * The constructor 
		 * @param h - the victim
		 */
		public RescueTask(Human h, IPath p)
		{
			victim = h;	
			path=p;
		}
			
		/**
		 * Return the deadline of this task
		 * @return deadline
		 */
		public double getDeadline() {
			ArrayList<Building> buildings = getFireSites();
			double min = Double.MAX_VALUE;
			double fireSpeed = 2177;
			for(Iterator<Building> it = buildings.iterator(); it.hasNext();){
				Building b = it.next();
				double time = timer.getTime() + (timeToHere(b,victim)/fireSpeed);
				if(time<min){
					min=time;
				}
			}
			return Math.min(min, timer.getTime()+getVictimDeadline(victim));
		}
		
		private double timeToHere(Building b, Human victim2) {
			
			return memory.getDistance(b.getID(), victim.getID());
		}

		ArrayList<Building> getFireSites(){
			ArrayList<Building> fringeBuildings = new ArrayList<Building>();
			for (FastFireSite fireSite : fires.getFireSites()) {
				fringeBuildings.addAll(fireSite.getFringe());
			}
			return fringeBuildings;
		}
		
		/**
		 * Return the victim to rescue
		 * @return victim
		*/
		public Human getVictim()	{return victim;}
 
		 /**
		 * Calculate how much time is necessary to reach and rescue the victim.
		 * @return processingTime - time that is necessary to reach and rescue the victim
		 */
		private double getProcessingTime()
		{
			double distance = computePathDistance(this,team[myIndex]);
			double travelTime=distance;
			return (victim.getBuriedness()+travelTime);
		}
		
		public boolean pathStillValid(ArrayList<Task> toDo){
			for(Iterator<Task> it=toDo.iterator(); it.hasNext();){
				Task t = it.next();
				if(t.civilian.getID().getValue()==victim.getID().getValue()){
					this.path=t.path;
					return true;
				}
			}
			return false;
		}
		
		public boolean pathStillValid(EntityID location){
			if(path.isValid()){
				if(victim.getPosition().getValue()==path.getLocations().get(path.getLocations().size()-1).getValue() &&
					location.getValue()==path.getLocations().get(0).getValue()){
				//path is still valid
				} else {
				//its not;
					path= pathPlanner.findShortestPath(location,victim.getID());
				}
			} else {
				path= pathPlanner.findShortestPath(location,victim.getID());
			}
			return path.isValid();
		}
		
		
		
		 /**
		 * Calculate how much time is necessary to reach the victim.
		 * @return travelTime - time that is necessary to reach the victim
		 */
		private double travelTime()
		{
			double distance= computePathDistance(this,team[myIndex]);
			return distance;
		}
		
		/**
		 * Calculate the task's utility
		 * @return ask's utility
		 */
		private double utility(double completionTime, double deadline)
		{
			double utility=0;
			if(completionTime<=deadline)
				utility=Math.pow(beta,completionTime);
			if(LOGGER.isTraceEnabled())
				LOGGER.trace(" : "+victim+" has Utility:"+utility+", completionTime:"+completionTime+", deadline:"+deadline);
			return utility;
		}
				
		/**
		 * Calculate the time that is necessary to complete this task
		 * @param timeStep - the current time
		 * @param partecipation - it is true, if this agent collaborate to the task
		 * @return completitionTime
		*/
	
		private double getCompletionTime(int timeStep, boolean partecipation)
		{
			double completionTime=timeStep+1;//minimum value

			if(timeStep<=3)//at beginning
			{
				if(allVictimsSize>0)
					completionTime=timeStep+this.getProcessingTime()/(team.length/allVictimsSize);//ambulances are shared equally between the victims;
			}
			else//during the simulation
			{
				if(LOGGER.isTraceEnabled() && partecipation){
					LOGGER.trace(" with me");
				} else {
					if(LOGGER.isTraceEnabled()){
						LOGGER.trace(" without me");
					}
				}
				int needProcessing=(int)Math.ceil(this.getProcessingTime());
				double estimation=(double)needProcessing;
				if(LOGGER.isTraceEnabled()){
					LOGGER.trace(" " + partecipation + " initial processing time " + estimation);
				}
				
				int j=0;
				int time=timeStep+1;    
				while(j<window && needProcessing>0)
				{
					for (int i=0;i<team.length;i++)
						if(partecipation)
						{
							if((this.getVictim().getID()).getValue()==nextAllocations [i][j]){
								needProcessing--;
								//if(LOGGER.isTraceEnabled()){
								//	LOGGER.trace("reduced processing for true task");
								//}
							}
						}
						else {
							if(i!=myIndex && (this.getVictim()).getID().getValue()==nextAllocations [i][j]){
								needProcessing--;
								//if(LOGGER.isTraceEnabled()){
								//	LOGGER.trace("reduced processing for false task");
								//}
							}
						}
					j++;
					time++;		
				}
				if(LOGGER.isTraceEnabled()){
					LOGGER.trace(" time digging " + time + " new processing " + needProcessing);
				}
				completionTime = time;
				
				if(needProcessing>0)//if more processing time is necessary
				{
					estimation=((double)window*estimation)/(estimation-(double)needProcessing);
					if(LOGGER.isTraceEnabled()){
						LOGGER.trace(" still to do " + estimation);
					}
					completionTime+=(int)estimation;
					//completionTime+=((int)estimation-needProcessing);
				}
				if(LOGGER.isTraceEnabled()){
					LOGGER.trace(" complete time " + completionTime);
				}
				
			}
			return completionTime;	
		}	
		
		/**
		 * Set the agent's utility, which is the marginal utility
		 */
		private void setMarginalUtility(int time)
		{
			double deadline = getDeadline();//time+getVictimDeadline(victim);
			double utilityDoing = utility(getCompletionTime(time, true),deadline);
			double utilityNotDoing = utility(getCompletionTime(time, false),deadline);
			//deadline = deadline - timer.getTime();
			marginalUtility = (utilityDoing - utilityNotDoing)- (0.05*deadline)  - (0.95*travelTime());// 
			if(LOGGER.isTraceEnabled() && utilityDoing > 0 && utilityNotDoing == 0)
				LOGGER.trace(" - SUCCESS marginal utility is useful for task: "+this.getVictim().getID());
		}

		/**
		 * Compares task's utilities
		 * @params a - first task 
		 * @params b - second task
		 * @return a negative integer, zero or a positive integer 
		 *         as the first argument is greater than, equal to o less than the second
		 */
		/*	private int compareTo(RescueTask a)
		{
			if (a.marginalUtility<this.marginalUtility) return 1;
			else if (a.marginalUtility>this.marginalUtility) return -1;
			else return 0;
		}
		 */

		/**
		 * Compares this task's victim with task a' victim
		 * @params task a 
		 * @return true or false
		 */
		/*	private boolean equals (RescueTask a)
		{return a.getVictim().equals(this.getVictim());}	
		*/
			
	}//end RescueTask
	
	public class TaskComparator implements Comparator<Object>
	{
		/**
		 * Compares marginal utilities
		 * @params e1 - first task 
		 * @params e2 - second task
		 * @return a negative integer, zero or a positive integer 
		 *         as the first argument is greater than, equal to o less than the second
		 */
		public int compare(Object e1, Object e2) {

			if(e1 instanceof RescueTask && e2 instanceof RescueTask) 
			{
				if( ((RescueTask)e1).marginalUtility > ((RescueTask)e2).marginalUtility)
					return -1;
				else if( ((RescueTask)e1).marginalUtility == ((RescueTask)e2).marginalUtility)
					return 0;
				else
					return 1;
			}
			return 0;
		}
	}

	public void setmyIndex(int myIndex2) {
		// TODO Auto-generated method stub
		myIndex=myIndex2;
	}

	public void setTeam(AmbulanceTeam[] team2) {
		team = team2;
		nextAllocations=new int[team.length][window];
	}
	
	
	
}//end RScheduler
