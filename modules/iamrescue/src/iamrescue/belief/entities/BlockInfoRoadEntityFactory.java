package iamrescue.belief.entities;

import rescuecore2.registry.EntityFactory;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

public class BlockInfoRoadEntityFactory implements EntityFactory {

	public static final BlockInfoRoadEntityFactory INSTANCE = new BlockInfoRoadEntityFactory();

	@Override
	public String[] getKnownEntityURNs() {
		return new String[] { StandardEntityURN.ROAD.toString() };
	}

	@Override
	public Entity makeEntity(String urn, EntityID id) {
		if (!urn.equals(StandardEntityURN.ROAD.toString())) {
			throw new IllegalArgumentException("Did not recognise URN: " + urn);
		}
		return new BlockInfoRoad(id);
	}

}
