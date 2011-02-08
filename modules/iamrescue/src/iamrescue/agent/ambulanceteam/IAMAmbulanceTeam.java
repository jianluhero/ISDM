package iamrescue.agent.ambulanceteam;

import iamrescue.agent.AbstractIAMAgent;
import iamrescue.agent.ambulanceteam.ambulancetools.AllocationMessage;
import iamrescue.agent.ambulanceteam.ambulancetools.AllocationHandler;
import iamrescue.agent.ambulanceteam.ambulancetools.RScheduler;
import iamrescue.agent.ambulanceteam.ambulancetools.Task;
import iamrescue.agent.ambulanceteam.ambulancetools.RScheduler.RescueTask;
import iamrescue.execution.command.DigOutCommand;
import iamrescue.execution.command.IPath;
import iamrescue.execution.command.LoadCommand;
import iamrescue.execution.command.MoveCommand;
import iamrescue.execution.command.RestCommand;
import iamrescue.execution.command.UnloadCommand;
import iamrescue.routing.IRoutingModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

public class IAMAmbulanceTeam extends AbstractIAMAgent<StandardEntity> {

	private static int LONG_TRAVEL_TIME_IGNORE = 15;
	private static int IGNORE_UNTIL_TIME_STEP = 150;
	
	/*private static int TIME_STEP_THRESHOLD = 3;
	private static double SEARCH_PROBABILITY  = 0.05;
	
	private static int LONG_TIME_STEP_THRESHOLD = 20;
	private static double LONG_SEARCH_PROBABILITY  = 0.2;
	private static int LONG_SEARCH_STEPS  = 10;
	*/
	//private int  
	
	@Override
	protected List<StandardEntityURN> getAgentTypes() {
		// TODO Auto-generated method stub
		List<StandardEntityURN> list =  new ArrayList<StandardEntityURN>();
		list.add(StandardEntityURN.AMBULANCE_TEAM);
		return list;
	}

	
	public StandardEntityURN getAgentType() {
		return StandardEntityURN.AMBULANCE_TEAM;
	}
	
	private static final Logger LOGGER = Logger.getLogger(IAMAmbulanceTeam.class);

	/**
	 * This implementation of an ambulance team uses RScheduler to decide the next civilian to rescue.
	 * If it doesn't know anything then it doesn't move.
	 */
	
	int [] d=new int[300], r=new int[300], ir=new int[300], f=new int[300];
	int ind=0;

	/**
	 * The list of ids in my team
	 */
	private AmbulanceTeam[] team;

	/**
	 * determines if the agent is buried
	 */
	//private boolean  wasBuried;

	/**
	 * determines if the agent is stuck
	 */
	//private boolean iamStuck;

	/**
	 * determines if the agent has got a loaded victim
	 */
	private boolean loadedVictim;

	/**
	 * the victim to save in this time step
	 */
	protected Human target;

	/**
	 * the scheduler used to compute which victim to go to.
	 */
	private RScheduler scheduler;

	/**
	 * the list of well-known targets
	 */
	private ArrayList<StandardEntity> targets;

	/**
	 * 
	 */
	private int myIndex;
	private int myTeamSize;
	private IRoutingModule pathPlanner;
	private int timeStep;
	private IPath path;
	private Collection<EntityID> allRefuges;
	private boolean teamSetUp = false;



//		---------------methods-------------------//	

	/**
	 * Construct a new AladdinAmbulanceTeam
	 */
	public IAMAmbulanceTeam() 
	{
		// Identify the agent type
		super();
		// the list of well-know targets
		targets = new ArrayList<StandardEntity>();
	}

	/**
	 * Get a reference the the AmbulanceTeam controlled by this agent
	 * @return The AmbulanceTeam controlled by this agent
	 */
	protected StandardEntity me() 
	{
		// Identify the agent
		return (StandardEntity) getWorldModel().getEntity(this.getID());
	}

	/**
	 * Returns the list of targets.
	 * @return the list of targets.
	 */
	public ArrayList<StandardEntity> getTargets() 
	{return targets;}

	/**
	 * Returns the scheduler used to compute which victim to go to.
	 * @return the scheduler.
	 */
	public RScheduler scheduler()
	{return scheduler;}
	
	/**
	 * Initialise the agent
	 * @param knowledge - The knowledge.
	 * @param self - The agent itself.
	 */
	public void initialiseAmbulance() 
	{
		//super.initialise();
		pathPlanner = this.getRoutingModule();
		myTeamSize=getWorldModel().getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM).size();

		if(LOGGER.isTraceEnabled())
			LOGGER.trace("Ambulance Version 2.01");

		// loaded a victim?
		loadedVictim = false;
		//iamStuck = false;

		// the team of Ambulances
		

		scheduler = new RScheduler(new ArrayList<RescueTask>(), getWorldModel(),getWorldModel().getEntitiesOfType(StandardEntityURN.BUILDING).toArray(new Building[0]), pathPlanner, timeStep,getSpeedInfo(),this,getTimer());
		
		if ( LOGGER.isTraceEnabled() ) 
			LOGGER.trace(" - initialise(): myTeamSize="+myTeamSize);
		
		Collection<StandardEntity> refs = getWorldModel().getEntitiesOfType(StandardEntityURN.REFUGE);
		Iterator<StandardEntity> it = refs.iterator();
		allRefuges = new ArrayList<EntityID>();
		while (it.hasNext()){
			StandardEntity temp = (StandardEntity)it.next();
			allRefuges.add(temp.getID());
		}
		
	}
	
	/**
	 * Sets the index and team size of this agent.
	 * @param team The agent team.
	 */
	private void setMyIndex(AmbulanceTeam[] team) {
		int[] ids = new int[team.length];
		for (int i=0;i<team.length;i++) {
			ids[i]=team[i].getID().getValue();
		}
		Arrays.sort(ids);
		for(int i=0;i<team.length;i++){
			team[i] = (AmbulanceTeam) getWorldModel().getEntity(new EntityID(ids[i]));
		}
		myIndex = Arrays.binarySearch(ids,this.getID().getValue());
		myTeamSize = team.length;
	}

	/**
	 * Main method - it is executed in each time step
	 */
	public void think(int time, ChangeSet changed) {
		AbstractIAMAgent.stopIfInterrupted();
		if(!teamSetUp){
			team = getWorldModel().getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM).toArray(new AmbulanceTeam[0]);
			setMyIndex(team);
			scheduler.setmyIndex(myIndex);
			scheduler.setTeam(team);
			teamSetUp=true;
			if(LOGGER.isTraceEnabled())
				printSharedID();
		}
		
		boolean takenAction = false;
		timeStep = time;
		if(LOGGER.isTraceEnabled()) 
			LOGGER.trace(" - entering act function");

		// issues a rescue message, if it is buried
		if (((Human)me()).getBuriedness()>0) 
		{
			if(LOGGER.isTraceEnabled())
				LOGGER.trace(" - buried: sending rescue message");
			sendRest(timeStep);
			return;
		}
		
		// moves to closest refuge, if it is damaged
		
		if (((Human)me()).getDamage()>0)
		{
			if(allRefuges.size()>0){
				moveToClosestRefuge();
				if(LOGGER.isTraceEnabled())
					LOGGER.trace(" - damaged: moving to regufe");
				return;
			}
		}
		
		Human nextTarget = null;
		Collection<StandardEntity> all_humans = getWorldModel().getEntitiesOfType(StandardEntityURN.CIVILIAN, StandardEntityURN.AMBULANCE_TEAM,StandardEntityURN.FIRE_BRIGADE,StandardEntityURN.POLICE_FORCE);
		ArrayList<Task> vics = getVictims(all_humans);
		boolean availableTask=false;
	//	if(vics.size()>0){
		availableTask=scheduler.chooseStrategy(vics, timeStep); //chooses next victims in the ordered set of all victims
	/*	} else {
			all_humans = getWorldModel().getEntitiesOfType(StandardEntityURN.CIVILIAN);
			vics = getVictims(all_humans);
			availableTask=scheduler.chooseStrategy(vics, timeStep);
		}*/
		AbstractIAMAgent.stopIfInterrupted();
			
		if(availableTask)
		{
			//int[] nextTasksID = scheduler.encodeStrategyToSend();
			//pack informations
			//this.getCommunicationModule().enqueueRadioMessageToOwnTeam(new AllocationMessage(nextTasksID,time));
			//chose next task
			nextTarget= scheduler.chooseNextVictim();
		}
		else if(LOGGER.isTraceEnabled())
		{
			LOGGER.trace(" - next strategy's not available - scheduler cannot find any target");
		}
		
		if(!all_humans.contains(target)){
			//summat funny
			target=null;
		}

		if(target!=null){
			if(LOGGER.isTraceEnabled())
				LOGGER.trace(" - in non null target ");
			takenAction = makeAction(vics);//execute the current target
			AbstractIAMAgent.stopIfInterrupted();
			if(!takenAction){
				//need to do some searching
				if(LOGGER.isTraceEnabled())
					LOGGER.trace(" - moving to search building ");
				target=null;
				AbstractIAMAgent.stopIfInterrupted();
				this.doDefaultSearch();
			}
		} else {
			target = nextTarget;//set target for the next time step
			if(nextTarget!=null){
				takenAction = makeAction(vics);//execute the next task in this time step
				AbstractIAMAgent.stopIfInterrupted();
			} else {
				//need to do some searching
				target=null;
				if(LOGGER.isTraceEnabled())
					LOGGER.trace(" - moving to search building ");
				AbstractIAMAgent.stopIfInterrupted();
				this.doDefaultSearch();
			}
		}
		if(LOGGER.isTraceEnabled())
			LOGGER.trace(" - finished thinking");
		return;
	}

	private void printSharedID() {
		LOGGER.trace(" - Shared Allocation for Agent is " + myIndex);
		for(int i=0; i<team.length; i++){
			LOGGER.trace(" - allocation: " + team[i].getID().getValue());
		}
	}

	/**
	 * Computes the list of civilians which are buried and need to be rescued
	 * @return sorted list of victims according to their deadline
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<Task> getVictims(Collection<StandardEntity> all_civilians) 
	{
		ArrayList<Task> civsWithPaths = new ArrayList<Task>();
		if (!all_civilians.isEmpty()) 
		{
			Human civilian;
			StandardEntity civilianLocation;
			int howfiery = 0;
			
			// remove those that are not buried
			for (Iterator civ_iterator = all_civilians.iterator();civ_iterator.hasNext();) 
			{
				
				
				civilian = (Human) civ_iterator.next();
				if(civilian.getID().getValue()==getID().getValue()){
					civ_iterator.remove();
					continue;
				}
				if(civilian.isPositionDefined()){
					if(LOGGER.isTraceEnabled())			
						LOGGER.trace(" - checking civilians " + civilian.getID() + " " + civilian.getPosition().getValue());
					civilianLocation= getWorldModel().getEntity(civilian.getPosition());
						if (civilianLocation instanceof Building)
						{
							if(((Building)civilianLocation).isFierynessDefined()){
								howfiery = ((Building)civilianLocation).getFieryness();
								if (howfiery>0 && howfiery <= 3)
								{
									civ_iterator.remove();
									continue;
								}
							}
							/*else{
								civ_iterator.remove();
								continue;
							}*/
						}
						else //remove civilians that are not in a building
						{
							civ_iterator.remove();
							continue;
						}
						
						if(civilian.isHPDefined()){
							if(civilian.getHP()==0){
								civ_iterator.remove();
								continue;
							}
						} else {
							civ_iterator.remove();
							continue;
						}
					
						
						if (civilianLocation instanceof AmbulanceTeam || civilianLocation instanceof Refuge )
						{
							//remove this agent as it's not buried or there is no route to its
							civ_iterator.remove();
							continue;
						}
						if(civilian.isBuriednessDefined()){
							if(civilian.getBuriedness() == 0 && !(civilianLocation instanceof Building)){
								//remove this agent as it's not buried or there is no route to its
								civ_iterator.remove();
								continue;
							}
							
							if(!(civilian instanceof Civilian) && civilian.getBuriedness()==0){
								civ_iterator.remove();
								continue;
							}
						} else {
							civ_iterator.remove();
							continue;
						}
						path = pathPlanner.findShortestPath(getID(),civilian.getID());
						if(!path.isValid()){
							civ_iterator.remove();
							continue;
						}
						
						double travelTime = getSpeedInfo().getTimeToTravelPath(path);
						if (travelTime >=LONG_TRAVEL_TIME_IGNORE && getTimer().getTime() > IGNORE_UNTIL_TIME_STEP) {
							civ_iterator.remove();
							continue;
						}
						
						
						double deadline =  scheduler.getVictimDeadline(civilian);
						if (deadline <=0 )
							civ_iterator.remove();
						else
							civsWithPaths.add(new Task(civilian, path));
					}
				}
		}
		
		return civsWithPaths;
	}
	
	private StandardEntity getLocation(){
		return ((Human) me()).getPosition(getWorldModel());
	}
	
	protected void sendUnload(int time) {
		AbstractIAMAgent.stopIfInterrupted();
		this.getExecutionService().execute(new UnloadCommand());
    }
	
	protected void sendRest(int time){
		AbstractIAMAgent.stopIfInterrupted();
		this.getExecutionService().execute(new RestCommand());
	}
	
	protected void sendLoad(int time, Civilian target) {
		AbstractIAMAgent.stopIfInterrupted();
		if(LOGGER.isTraceEnabled())
			LOGGER.trace(" - should be loading: " + target.getFullDescription());
		this.getExecutionService().execute(new LoadCommand(target));
    }
	
	protected void sendRescue(int time, Human target) {
		AbstractIAMAgent.stopIfInterrupted();
		this.getExecutionService().execute(new DigOutCommand(target));
    }
	
	protected void sendMove(int time, IPath path) {
		AbstractIAMAgent.stopIfInterrupted();
		MoveCommand move = new MoveCommand();
		move.setPath(path);
		this.getExecutionService().execute(move);
    }

	/**
	 * Decide the action to execute in this time step
	 * @return true if it did an action, false if it did not anything
	 */	
	private boolean makeAction(ArrayList<Task> vics)
	{
		if(LOGGER.isTraceEnabled())
			LOGGER.trace(" - doing something with "+target +" who is at " + target.getPosition().getValue() + " and I am at " + getLocation().getID().getValue());
		boolean takenAction = false;
		boolean defined = target.isBuriednessDefined() && target.isHPDefined() && target.isPositionDefined() && !(getWorldModel().getEntity(target.getPosition()) instanceof AmbulanceTeam);
		if(!defined){
			target=null;
			return false;
		} else if (target.getHP()==0){
			if(loadedVictim){
				sendUnload(timeStep);
				target = null;
				loadedVictim = false;
				return true;
			} else {
				target = null;
				loadedVictim = false;
				return false;
			}
		}
		
		if(loadedVictim)//it has a loaded victim
		{
			if(getLocation() instanceof Refuge)//leave the victim in the refuge
			{
				sendUnload(timeStep);
				if(LOGGER.isTraceEnabled())
					LOGGER.trace(" - unload victim:"+target);
				target = null;
				loadedVictim = false;
				takenAction=true;
			}
			else//bring the victim to the closest refuge
			{
				if(this.allRefuges.size()>0){
					moveToClosestRefuge();
					if(LOGGER.isTraceEnabled())
						LOGGER.trace(" - moving to refuge");
					takenAction=true;
				} else {
					if(getLocation() instanceof Road) {
						sendUnload(timeStep);
						if(LOGGER.isTraceEnabled()) 
							LOGGER.trace(" - unload victim:"+target);
						target = null;
						loadedVictim = false;
						takenAction=true;
					} else {
						moveToClosestRoad();
						if(LOGGER.isTraceEnabled())
							LOGGER.trace(" - moving to road to drop");
						takenAction=true;
					}
				}
			}
		}
		else //it has not a loaded victim
		{
			StandardEntity targetPosition = getWorldModel().getEntity(target.getPosition());
			if(targetPosition instanceof Refuge || targetPosition instanceof AmbulanceTeam || targetPosition == null)//target is just saved
			{
				target = null;
				takenAction =false;
				if(LOGGER.isTraceEnabled())
					LOGGER.trace(" - "+target+" has been saved by someone else");
			}
			else
			{
				if(getLocation()==targetPosition && targetPosition instanceof Building)
				{
					if(target.getBuriedness()==0)//VICTIM IS NOT buried 
					{
						if(target instanceof Civilian && loadVictim()){
							sendLoad(timeStep, (Civilian) target);
							loadedVictim = true;
							if(LOGGER.isTraceEnabled())
								LOGGER.trace(" - loading victim: "+target);
							takenAction=true;
						}
						else 
						{
							sendRest(timeStep);
							takenAction =true;
							if(LOGGER.isTraceEnabled())
								LOGGER.trace(" - finished rescuing victim: "+target);
							target = null;
						}
					}
					else //victim is buried
					{
						sendRescue(timeStep, target);
						if(LOGGER.isTraceEnabled())
							LOGGER.trace(" - rescuing victim: "+target);
						takenAction=true;
					}
				}
				else//move to victim
				{		
					//find path
					path=null;
					for(Iterator<Task> it=vics.iterator(); it.hasNext();){
						Task r = it.next();
						if(r.civilian.getID().getValue()==target.getID().getValue()){
							path = r.path;
						}
					}
					if (path == null) {
						path = pathPlanner.findShortestPath(getID(), target
								.getID());
					}
					if (path.isValid()) 
					{
						if(LOGGER.isTraceEnabled())
							LOGGER.trace(" - moving to victim: "+target);
						
						//boolean tookRandomSearch = false;
						sendMove(timeStep, path);
						takenAction=true;
					}
					else { 
						if(LOGGER.isTraceEnabled())
							LOGGER.trace(" - path to target is null! and i cannot move to victim: "+target);
						target=null;
						takenAction=false;
					}
				}
			}
		}
		return takenAction;
	}

	/**
	 * Load a civilian if it is unburied and move it to a close refuge
	 */ 
	private boolean loadVictim()
	{
		AmbulanceTeam tempAmb;
		if (target.getPosition().getValue() == getLocation().getID().getValue()) 
		{
			for (int i = 0; i < team.length; ++i) 
			{
				tempAmb = (AmbulanceTeam) team[i];
				if (model.getEntity(tempAmb.getPosition()) instanceof Building && (tempAmb.getPosition().getValue() == target.getPosition().getValue()))
				{
					if (tempAmb.getID() == me().getID()){
						return true;
					} else
						return false;
				}
			}
			return false;
		}
		return false;
	}

	/**
	 * Move to a close refuge - usually called to save a humanoid or to go somewhere safe.
	 *
	 */
	protected void moveToClosestRefuge() 
	{
		path = pathPlanner.findShortestPath(getLocation().getID(), allRefuges);
		if (path.isValid())
		{
			sendMove(timeStep, path);
			return;
		} else {
			//cant get to a refuge
			//sendRest(timeStep);
			doDefaultSearch();
			return;
		}
	}
	
	protected void moveToClosestRoad(){
		List<EntityID> neighbours = ((Area)getLocation()).getNeighbours();
		if(getLocation() instanceof Road){
			neighbours.add(getLocation().getID());
		}
		path = pathPlanner.findShortestPath(getID(), neighbours);
		if(path.isValid()){
			sendMove(timeStep, path);
			return;
		} else {
			sendRest(timeStep);
			return;
		}
	}

	/**
	 * 
	 */
	public class CivilianToSort
	{
		public StandardEntity civilian;
		public double deadline;
		public CivilianToSort(StandardEntity _civilian, double _deadline)
		{
			civilian = _civilian;
			deadline = _deadline;
		}
	}

	public class DeadlineComparator implements Comparator<CivilianToSort>
	{
		/**
		 * Compares deadlines
		 * @params d1 - first victim
		 * @params d2 - second victim
		 * @return a negative integer, zero or a positive integer 
		 *         as the first argument's deadline is less, equal to, or greater than the second argument's deadline
		 */
		public int compare(CivilianToSort a, CivilianToSort b)
		{
			CivilianToSort vicA = (CivilianToSort)  a;
			CivilianToSort vicB = (CivilianToSort ) b;
			double deadlineA = vicA.deadline;
			double deadlineB = vicB.deadline;
			if (deadlineA > deadlineB) return(1);//<=will sort in descending order of deadlines
			else if (deadlineA == deadlineB) return 0;
			else return(-1);
		}
	}

	public void postConnect() {
		super.postConnect();
		//showWorldModelViewer();
//		showRoutingViewer();
		initialiseAmbulance();
		addUpdateHandler(new AllocationHandler(this.getID().getValue(), scheduler));
	}

	@Override
	protected void fallback(int time, ChangeSet changed) {
		if (LOGGER.isTraceEnabled())
		{
			LOGGER.trace(" " + getID().getValue() + " - DOING FALLBACK!");
		}
		if(!teamSetUp){
			team = getWorldModel().getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM).toArray(new AmbulanceTeam[0]);
			setMyIndex(team);
			scheduler.setmyIndex(myIndex);
			scheduler.setTeam(team);
			teamSetUp=true;
			if(LOGGER.isTraceEnabled())
				printSharedID();
		}
		//doDefaultSearch();
		//return;
		
		// Am I transporting a civilian to a refuge?
		EntityID onb = someoneOnBoard();
        if (onb !=null) {
            // Am I at a refuge?
            if (getLocation() instanceof Refuge) {
                // Unload!
            	if (LOGGER.isTraceEnabled())
        		{
        			LOGGER.trace(" unloading!");
        		}
                sendUnload(time);
                return;
            }
            else {
                // Move to a refuge
            	IPath path = pathPlanner.findShortestPath(getLocation().getID(), allRefuges);
                if (path.isValid()) {
                	if (LOGGER.isTraceEnabled())
            		{
            			LOGGER.trace(" moving to refuge!");
            		}
                    sendMove(time, path);
                    return;
                }
                // What do I do now? Might as well carry on and see if we can dig someone else out.
                if (LOGGER.isTraceEnabled())
        		{
        			LOGGER.trace(" no refuges so searching!");
        		}
                doDefaultSearch();
                return;
            }
        }
        //find out if someone is in the same position
        Human toSave = inThisPosition();
        if (toSave !=null) {
        	 if ((toSave instanceof Civilian) && toSave.getBuriedness() == 0 && !(getLocation() instanceof Refuge)) {
                 // Load
             	if (LOGGER.isTraceEnabled())
         		{
         			LOGGER.trace(" loading!");
         		}
                 sendLoad(time, (Civilian)toSave);
                 return;
             }
             if (toSave.getBuriedness() > 0) {
                 // Rescue
             	if (LOGGER.isTraceEnabled())
         		{
         			LOGGER.trace(" rescuing!");
         		}
                 sendRescue(time, toSave);
                 return;
             }
        }
        else {
                // Try to move to the target
        	ArrayList<EntityID> civs = new ArrayList<EntityID>();
        	for (StandardEntity next : getWorldModel().getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
        		civs.add(next.getID());
        	}
        	IPath path = pathPlanner.findShortestPath(getLocation().getID(), civs);
        	if (path.isValid()) {
                	if (LOGGER.isTraceEnabled())
            		{
            			LOGGER.trace(" going to target!");
            		}
                    sendMove(time, path);
                    return;
                }
        }
        
        if (LOGGER.isTraceEnabled())
		{
			LOGGER.trace(" searching with nothing to do!");
		}
		doDefaultSearch();
		return;
	}
	
	 private EntityID someoneOnBoard() {
	        for (StandardEntity next : getWorldModel().getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
	            if (((Human)next).getPosition().equals(getID())) {
	            	if (LOGGER.isTraceEnabled())
	        		{
	        			LOGGER.trace(" someone is onboard!");
	        		}
	                return next.getID();
	            }
	        }
	        return null;
	    }
	 
	 private Human inThisPosition() {
	        for (StandardEntity next : this.getWorldModel().getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
	            if (((Human)next).getPosition().equals(getLocation())) {
	            	if (LOGGER.isTraceEnabled())
	        		{
	        			LOGGER.trace(" someone is here!");
	        		}
	                return (Human)next;
	            }
	        }
	        return null;
	    }
	
}//end of AladdinAmbulanceTeam class

