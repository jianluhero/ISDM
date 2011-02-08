/**
 * 
 */
package iamrescue.communication.messages.codec.property;

import iamrescue.belief.entities.RoutingInfoBlockade;
import iamrescue.communication.messages.codec.BitStreamDecoder;
import iamrescue.communication.messages.codec.BitStreamEncoder;
import iamrescue.routing.WorldModelConverter;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;
import rescuecore2.worldmodel.properties.IntArrayProperty;

/**
 * @author Sebastian
 * 
 */
public class BlockedNeighboursCodec implements PropertyCodec {

	// private WorldModelConverter converter;

	private static final Logger LOGGER = Logger
			.getLogger(BlockedNeighboursCodec.class);

	public BlockedNeighboursCodec() {// WorldModelConverter converter) {
		// this.converter = converter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.codec.property.PropertyCodec#decode(
	 * rescuecore2.worldmodel.Entity,
	 * iamrescue.communication.messages.codec.BitStreamDecoder)
	 */
	@Override
	public Property decode(Entity object, BitStreamDecoder decoder) {
		WorldModelConverter converter = decoder.getBeliefBase().getConverter();

		IntArrayProperty property = new IntArrayProperty(
				RoutingInfoBlockade.BLOCK_INFO_URN);

		int arrayLength = decoder.readNumber();

		if (arrayLength == 0) {
			property.setValue(new int[0]);
		} else {

			List<Integer> list = new ArrayList<Integer>();

			boolean[] edges = new boolean[arrayLength];

			for (int i = 0; i < arrayLength; i++) {
				edges[i] = decoder.readBoolean();
			}

			// This works out the number of neighbours based on length of array
			// (quadratic formula)
			int neighbours = (int) Math.round(0.5 + Math
					.sqrt(0.25 + 2 * arrayLength));

			int counter = 0;

			for (int i = 0; i < neighbours - 1; i++) {
				for (int j = i + 1; j < neighbours; j++) {
					if (edges[counter++]) {
						list.add(i);
						list.add(j);
					}
				}
			}

			int[] blocked = new int[list.size()];
			for (int i = 0; i < list.size(); i++) {
				blocked[i] = list.get(i);
			}

			property.setValue(blocked);

		}

		return property;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.codec.property.PropertyCodec#encode(
	 * rescuecore2.worldmodel.Entity, rescuecore2.worldmodel.Property,
	 * iamrescue.communication.messages.codec.BitStreamEncoder)
	 */
	@Override
	public void encode(Entity object, Property property,
			BitStreamEncoder encoder) {
		WorldModelConverter converter = encoder.getBeliefBase().getConverter();
		RoutingInfoBlockade b = (RoutingInfoBlockade) object;
		//LOGGER.info(b.getFullDescription());
		// Work out all possible edges
		EntityID position = b.getPosition();
		int positionInt = position.getValue();

		List<Integer> neighbours = converter.getSortedNeighbours(positionInt);

		RoutingInfoBlockade block = (RoutingInfoBlockade) object;
		int[] blockedEdges = block.getBlockedEdges();

		// int highestIndex = 0;

		// List<Integer> encoded = new ArrayList<Integer>();

		boolean[] encoded = new boolean[neighbours.size()
				* (neighbours.size() - 1) / 2];

		int counterBlocked = 0;
		int counterEncoded = 0;

		boolean atLeastOne = false;

		for (int i = 0; i < neighbours.size() - 1
				&& counterBlocked < blockedEdges.length; i++) {
			for (int j = i + 1; j < neighbours.size()
					&& counterBlocked < blockedEdges.length; j++) {
				if (blockedEdges[counterBlocked] == i
						&& blockedEdges[counterBlocked + 1] == j) {
					encoded[counterEncoded] = true;
					atLeastOne = true;
					counterBlocked += 2;
				} else {
					encoded[counterEncoded] = false;
				}
				counterEncoded++;
			}
		}

		if (counterBlocked != blockedEdges.length) {
			throw new IllegalStateException("Did not encode all blocks!");
		}

		if (atLeastOne) {
			encoder.appendNumber(encoded.length);

			for (int i = 0; i < encoded.length; i++) {
				encoder.appendBoolean(encoded[i]);
			}
		} else {
			encoder.appendNumber(0);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.communication.messages.codec.property.PropertyCodec#getPropertyKey
	 * ()
	 */
	@Override
	public String getPropertyKey() {
		return RoutingInfoBlockade.BLOCK_INFO_URN;
	}
}