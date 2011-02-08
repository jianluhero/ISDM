package iamrescue.belief.entities;

import rescuecore2.registry.PropertyFactory;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.BooleanProperty;

public class KnownToBePassablePropertyFactory implements PropertyFactory {

	private static final String[] KNOWN_PROPERTY_URN = new String[] { BlockInfoRoad.HAS_BEEN_PASSED_URN };

	public static final KnownToBePassablePropertyFactory INSTANCE = new KnownToBePassablePropertyFactory();

	@Override
	public String[] getKnownPropertyURNs() {
		return KNOWN_PROPERTY_URN;
	}

	@Override
	public Property makeProperty(String urn) {
		if (!urn.equals(BlockInfoRoad.HAS_BEEN_PASSED_URN)) {
			throw new IllegalArgumentException("Did not recognise URN " + urn);
		}
		return new BooleanProperty(urn);
	}

}
