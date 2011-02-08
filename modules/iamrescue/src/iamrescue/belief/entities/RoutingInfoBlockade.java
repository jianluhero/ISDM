/**
 * 
 */
package iamrescue.belief.entities;

import static rescuecore2.standard.Constants.PROPERTY_URN_PREFIX;
import iamrescue.routing.WorldModelConverter;
import iamrescue.routing.WorldModelConverter.SimpleGraphNode;
import iamrescue.util.PositionXY;

import java.util.List;

import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.EntityListener;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.IntArrayProperty;

/**
 * @author Sebastian
 * 
 */
public class RoutingInfoBlockade extends Blockade implements EntityListener {

	public static final String BLOCK_INFO_URN = PROPERTY_URN_PREFIX
			+ "blockedneighours";

	private IntArrayProperty blockedEdges;

	private PositionXY[] positionArray = null;

	private int[] apexesCached = null;

	/**
	 * Construct a BlockInfoRoad object with entirely undefined property values.
	 * 
	 * @param id
	 *            The ID of this entity.
	 */
	public RoutingInfoBlockade(EntityID id) {
		super(id);
		blockedEdges = new IntArrayProperty(BLOCK_INFO_URN);
		registerProperties(blockedEdges);
		addEntityListener(this);
	}

	/**
	 * BlockInfoRoad copy constructor.
	 * 
	 * @param other
	 *            The BlockInfoRoad to copy.
	 */
	public RoutingInfoBlockade(RoutingInfoBlockade other) {
		super(other);
		blockedEdges = new IntArrayProperty(other.blockedEdges);
		registerProperties(blockedEdges);
		addEntityListener(this);
	}

	@Override
	protected Entity copyImpl() {
		return new BlockInfoRoad(getID());
	}

	public int[] getBlockedEdges() {
		return blockedEdges.getValue();
	}

	public boolean isBlockedEdgesDefined() {
		return blockedEdges.isDefined();
	}

	public PositionXY[] getBlockedEdgePositions(WorldModelConverter converter) {
		if (!blockedEdges.isDefined() || !isPositionDefined()) {
			throw new IllegalArgumentException(
					"Blocked edges or position are not defined: "
							+ getFullDescription());
		} else {
			if (positionArray == null) {
				List<Integer> sortedNeighbours = converter
						.getSortedNeighbours(getPosition().getValue());
				int[] blocked = blockedEdges.getValue();
				PositionXY[] positions = new PositionXY[blocked.length];
				for (int i = 0; i < blocked.length; i = i + 2) {
					int fromSimple = sortedNeighbours.get(blocked[i]);
					int toSimple = sortedNeighbours.get(blocked[i + 1]);
					SimpleGraphNode fromNode = converter
							.getSimpleGraphNode(fromSimple);
					SimpleGraphNode toNode = converter
							.getSimpleGraphNode(toSimple);
					positions[i] = fromNode.getRepresentativePoint();
					positions[i + 1] = toNode.getRepresentativePoint();
				}
				positionArray = positions;
			}
			return positionArray;
		}
	}

	public void setBlockedEdges(int[] blockedEdges) {
		this.blockedEdges.setValue(blockedEdges);
	}

	public IntArrayProperty getBlockedEdgesProperty() {
		return blockedEdges;
	}

	public void undefineBlockedEdges() {
		blockedEdges.undefine();
	}

	@Override
	public Property getProperty(String urn) {
		if (urn.equals(BLOCK_INFO_URN)) {
			return blockedEdges;
		} else {
			return super.getProperty(urn);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * rescuecore2.worldmodel.EntityListener#propertyChanged(rescuecore2.worldmodel
	 * .Entity, rescuecore2.worldmodel.Property, java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void propertyChanged(Entity e, Property p, Object oldValue,
			Object newValue) {
		if (p.getURN().equals(StandardPropertyURN.POSITION.toString())
				|| p.getURN().equals(BLOCK_INFO_URN)) {
			positionArray = null;
		} else if (p.getURN().equals(StandardPropertyURN.APEXES.toString())) {
			apexesCached = null;
		}
	}
	
	@Override
	public int[] getApexes() {
		if (apexesCached == null) {
			apexesCached  = super.getApexes();
		}return apexesCached;
	}
}
