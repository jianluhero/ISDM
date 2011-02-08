/**
 * 
 */
package iamrescue.belief;

import java.util.Collection;

import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public interface IBuildingSearchUtility {

	/**
	 * 
	 * @return All buildings that have been seen and are known to be safe (i.e,
	 *         not on fire), and haven't been entered yet.
	 */
	public Collection<EntityID> getSafeUnsearchedBuildings();

	/**
	 * 
	 * @return All buildings that have never been seen (and not entered) by any.
	 */
	public Collection<EntityID> getUnknownBuildings();
	
	/**
	 * 
	 * @return All modulated buildings that have been seen and are known to be safe (i.e,
	 *         not on fire), and haven't been entered yet.
	 */
	public Collection<EntityID> getModulatedSafeUnsearchedBuildings();

	/**
	 * 
	 * @return All modulated buildings that have never been seen (and not entered) by any.
	 */
	public Collection<EntityID> getModulatedUnknownBuildings();
	
	/**
	 * 
	 * @return All high priority buildings that have never been seen (and not entered) by any.
	 */
	public Collection<EntityID> getSafeHigh();

	/**
	 * 
	 * @return All high priority buildings that have never been seen (and not entered) by any.
	 */
	public Collection<EntityID> getUnknownHigh();
}
