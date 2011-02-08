package iamrescue.belief.entities;

import rescuecore2.registry.EntityFactory;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

public class RoutingInfoBlockadeEntityFactory implements EntityFactory {

	public static final RoutingInfoBlockadeEntityFactory INSTANCE = new RoutingInfoBlockadeEntityFactory();

	@Override
	public String[] getKnownEntityURNs() {
		return new String[] { StandardEntityURN.BLOCKADE.toString() };
	}

	@Override
	public Entity makeEntity(String urn, EntityID id) {
		if (!urn.equals(StandardEntityURN.BLOCKADE.toString())) {
			throw new IllegalArgumentException("Did not recognise URN: " + urn);
		}
		return new RoutingInfoBlockade(id);
	}
}
