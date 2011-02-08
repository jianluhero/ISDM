package iamrescue.agent.ambulanceteam.ambulancetools;

import iamrescue.belief.IAMWorldModel;
import iamrescue.routing.IRoutingModule;

import java.util.*;


import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

public class DestinationSorter {
    
    /**
       Sort a list of RescueObjects by distance. This list will be sorted in place.
       @param objects The objects to be sorted. When the method returns this list will be sorted.
       @param reference The RescueObject to measure distances from
       @param memory The memory of the agent doing the sorting
    */
	public static void sortByEuclideanDistance(List<?> objects, StandardEntity reference, IAMWorldModel memory) {
        synchronized(DISTANCE_SORTER) {
            DISTANCE_SORTER.memory = memory;
            DISTANCE_SORTER.reference = reference;
            Collections.sort(objects,DISTANCE_SORTER);
        }
    }

    /**
       Sort an array of RescueObjects by distance. This array will be sorted in place.
       @param objects The objects to be sorted. When the method returns this array will be sorted.
       @param reference The RescueObject to measure distances from
       @param memory The memory of the agent doing the sorting
    */
	public static void sortByEuclideanDistance(StandardEntity[] objects, StandardEntity reference, IAMWorldModel memory) {
        synchronized(DISTANCE_SORTER) {
            DISTANCE_SORTER.memory = memory;
            DISTANCE_SORTER.reference = reference;
            Arrays.sort(objects,DISTANCE_SORTER);
        }
    }
    public static int compareByEuclideanDistance(StandardEntity o1, StandardEntity o2, StandardEntity reference, IAMWorldModel memory) {
             double d1 = getSquareDistanceBetween(reference,(StandardEntity)o1,memory);
             double d2 = getSquareDistanceBetween(reference,(StandardEntity)o2,memory);
             if (d1 < d2) // Object o1 is closer
                 return -1;
             if (d1 > d2) // Object o2 is closer
                 return 1;
         // They are the same distance (or we couldn't find one of them). Return the lower id first
         return 0;
    }

    /**
       A Comparator for use when sorting RescueObjects by distance
    */
    private static class EuclideanDistanceSorter implements Comparator<Object> {
    	IAMWorldModel memory;
        StandardEntity reference;

        public int compare(Object o1, Object o2) {
                double d1 = getSquareDistanceBetween(reference,(StandardEntity)o1,memory);
                double d2 = getSquareDistanceBetween(reference,(StandardEntity)o2,memory);
                if (d1 < d2) // Object o1 is closer
                    return -1;
                if (d1 > d2) // Object o2 is closer
                    return 1;
            // They are the same distance (or we couldn't find one of them). Return the lower id first
            return 0;

        }
    }
    
    /**
    Sort a list of RescueObjects by distance. This list will be sorted in place.
    @param objects The objects to be sorted. When the method returns this list will be sorted.
    @param reference The RescueObject to measure distances from
    @param memory The memory of the agent doing the sorting
 */
 public static void sortByPathCost(List<?> objects, StandardEntity reference, IRoutingModule planner, IAMWorldModel memory) {
     synchronized(PATH_COST_DISTANCE_SORTER) {
    	 //PATH_COST_DISTANCE_SORTER.memory = memory;
    	 PATH_COST_DISTANCE_SORTER.reference = reference;
    	 PATH_COST_DISTANCE_SORTER.pathPlanner = planner;
         Collections.sort(objects,PATH_COST_DISTANCE_SORTER);
     }
 }

 /**
    Sort an array of RescueObjects by distance. This array will be sorted in place.
    @param objects The objects to be sorted. When the method returns this array will be sorted.
    @param reference The RescueObject to measure distances from
    @param memory The memory of the agent doing the sorting
 */
 public static void sortByPathCost(StandardEntity[] objects, StandardEntity reference,IRoutingModule planner, IAMWorldModel memory) {
     synchronized(PATH_COST_DISTANCE_SORTER) {
         //PATH_COST_DISTANCE_SORTER.memory = memory;
         PATH_COST_DISTANCE_SORTER.reference = reference;
         PATH_COST_DISTANCE_SORTER.pathPlanner = planner;
         
         Arrays.sort(objects,PATH_COST_DISTANCE_SORTER);
     }
 }

    
    /**
     * A comparator for use when sorting RescueObjects by path cost.
     * @author sdr
     *
     */
    private static class PathCostDistanceSorter implements Comparator<Object> {
    	//IAMWorldModel memory;
        StandardEntity reference;
        IRoutingModule pathPlanner;
        
        @SuppressWarnings("null")
		public int compare(Object o1, Object o2) {
        	
        	Collection<EntityID> dests = null;
        	//HashSet dests = new HashSet();
        	dests.add((EntityID) o1);
        	dests.add((EntityID) o2);
        	List<EntityID> temp = pathPlanner.findShortestPath(reference.getID(), dests).getLocations();
        	if(temp.get(temp.size())==((StandardEntity)o1).getID()){
        		return -1;
        	} else {
        		return 1;
        	}

        }
    }
    

    private final static EuclideanDistanceSorter DISTANCE_SORTER = new EuclideanDistanceSorter();
    
    private final static PathCostDistanceSorter PATH_COST_DISTANCE_SORTER = new PathCostDistanceSorter();

	public static double getSquareDistanceBetween(StandardEntity a, StandardEntity b,IAMWorldModel memory){
		int x1=0, x2=0, y1=0, y2=0;
		//int[] xy;
		try{
	
			x1 = a.getLocation(memory).first();
			y1 = a.getLocation(memory).second();
		}
		catch(Exception e){
			System.out.println("Could not find "+a+" "+e);
		}
		try{	
			x2 = b.getLocation(memory).first();
			y2 = b.getLocation(memory).second();
		}
		catch(Exception e){
	
		}
		double x = x2-x1;
		x = x*x;
		double y = y2-y1;
		y = y*y;
		return (x+y);
	}


    
}

