package iamrescue.agent;

import rescuecore2.standard.entities.StandardEntityURN;

public class AgentTypeUtils {

	public static boolean isPlatoon(StandardEntityURN urn) {
		switch (urn) {
		case AMBULANCE_TEAM:
		case FIRE_BRIGADE:
		case POLICE_FORCE:
			return true;
		default:
			return false;
		}
	}

	public static boolean isCentre(StandardEntityURN urn) {
		switch (urn) {
		case AMBULANCE_CENTRE:
		case FIRE_STATION:
		case POLICE_OFFICE:
			return true;
		default:
			return false;
		}
	}

	public static StandardEntityURN getAssociatedPlatoon(StandardEntityURN urn) {
		switch (urn) {
		case AMBULANCE_CENTRE:
			return StandardEntityURN.AMBULANCE_TEAM;
		case FIRE_STATION:
			return StandardEntityURN.FIRE_BRIGADE;
		case POLICE_OFFICE:
			return StandardEntityURN.POLICE_FORCE;
		}

		return null;
	}

}
