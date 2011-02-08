package iamrescue.util;

import java.util.Map;
import java.util.Map.Entry;

import javolution.util.FastMap;
import rescuecore2.standard.entities.AmbulanceCentre;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.FireStation;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.PoliceOffice;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.World;

/**
 * Translates between classes and Rescuecore URNs.
 * 
 * @author Sebastian
 * 
 */
public class ClassURNTranslator {
	private static Map<Class<? extends StandardEntity>, StandardEntityURN> classToURN = new FastMap<Class<? extends StandardEntity>, StandardEntityURN>();
	private static Map<StandardEntityURN, Class<? extends StandardEntity>> URNtoClass = new FastMap<StandardEntityURN, Class<? extends StandardEntity>>();

	/**
	 * Returns the class associated with the given urn.
	 * 
	 * @param urn
	 *            The urn
	 * @return The associated class (or null if unknown).
	 */
	public static Class<? extends StandardEntity> getClass(StandardEntityURN urn) {
		return URNtoClass.get(urn);
	}

	/**
	 * Returns the urn associated with the given class.
	 * 
	 * @param seClass
	 *            The class
	 * @return The associated urn (or null if unknown).
	 */
	public static StandardEntityURN getURN(
			Class<? extends StandardEntity> seClass) {
		return classToURN.get(seClass);
	}

	static {
		classToURN.put(AmbulanceCentre.class,
				StandardEntityURN.AMBULANCE_CENTRE);
		classToURN.put(AmbulanceTeam.class, StandardEntityURN.AMBULANCE_TEAM);
		classToURN.put(Blockade.class, StandardEntityURN.BLOCKADE);
		classToURN.put(Building.class, StandardEntityURN.BUILDING);
		classToURN.put(Civilian.class, StandardEntityURN.CIVILIAN);
		classToURN.put(FireBrigade.class, StandardEntityURN.FIRE_BRIGADE);
		classToURN.put(FireStation.class, StandardEntityURN.FIRE_STATION);
		classToURN.put(PoliceForce.class, StandardEntityURN.POLICE_FORCE);
		classToURN.put(PoliceOffice.class, StandardEntityURN.POLICE_OFFICE);
		classToURN.put(Refuge.class, StandardEntityURN.REFUGE);
		classToURN.put(Road.class, StandardEntityURN.ROAD);
		classToURN.put(World.class, StandardEntityURN.WORLD);

		for (Entry<Class<? extends StandardEntity>, StandardEntityURN> entry : classToURN
				.entrySet()) {

		}
	}
}
