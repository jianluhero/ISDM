package iamrescue.belief.entities;

import rescuecore2.registry.PropertyFactory;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.IntArrayProperty;

public class BlockedEdgesPropertyFactory implements PropertyFactory {

	private static final String[] KNOWN_PROPERTY_URN = new String[] { RoutingInfoBlockade.BLOCK_INFO_URN };

	public static final BlockedEdgesPropertyFactory INSTANCE = new BlockedEdgesPropertyFactory();

	@Override
	public String[] getKnownPropertyURNs() {
		return KNOWN_PROPERTY_URN;
	}

	@Override
	public Property makeProperty(String urn) {
		if (!urn.equals(RoutingInfoBlockade.BLOCK_INFO_URN)) {
			throw new IllegalArgumentException("Did not recognise URN " + urn);
		}
		return new IntArrayProperty(urn);
	}
}
