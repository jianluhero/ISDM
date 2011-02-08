package iamrescue.communication.messages.codec.property;

import iamrescue.belief.entities.BlockInfoRoad;
import iamrescue.belief.entities.RoutingInfoBlockade;

import java.util.Map;

import javolution.util.FastMap;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.StandardPropertyURN;

public class PropertyEncoderStore {

	public static final int COORD_ACCURACY = 100;

	private Map<String, PropertyCodec> encoders = new FastMap<String, PropertyCodec>();
	private Config config;

	public PropertyEncoderStore(Config config) {
		this.config = config;
		initialise();
	}

	private void initialise() {
		// For clearing: clear.repair.rate: 10
		// : 1000
		// : 100

		// Encode buriedness using clear rate for precision

		encoders.put(StandardPropertyURN.BURIEDNESS.toString(),
				new PrecisionIntPropertyCodec(StandardPropertyURN.BURIEDNESS
						.toString(), 1));

		encoders.put(StandardPropertyURN.POSITION.toString(),
				new EntityRefPropertyCodec(StandardPropertyURN.POSITION
						.toString()));

		int hpPrecision = config.getIntValue(
				"perception.standard.hp-precision", 1000);

		// To be prudent, since we may not actually receive these parameters.
		if (hpPrecision > 100) {
			hpPrecision = 100;
		}
		encoders.put(StandardPropertyURN.HP.toString(),
				new PrecisionIntPropertyCodec(
						StandardPropertyURN.HP.toString(), hpPrecision));

		// To be prudent, since we may not actually receive these parameters.
		int damagePrecision = config.getIntValue(
				"perception.standard.damage-precision", 100);
		if (damagePrecision > 10) {
			damagePrecision = 10;
		}
		encoders.put(StandardPropertyURN.DAMAGE.toString(),
				new PrecisionIntPropertyCodec(StandardPropertyURN.DAMAGE
						.toString(), damagePrecision));

		encoders.put(StandardPropertyURN.TEMPERATURE.toString(),
				new PrecisionIntPropertyCodec(StandardPropertyURN.TEMPERATURE
						.toString(), 1));
		encoders.put(StandardPropertyURN.FIERYNESS.toString(),
				new IntPropertyCodec(StandardPropertyURN.FIERYNESS.toString()));
		encoders.put(StandardPropertyURN.X.toString(),
				new CoordinatePropertyCodec(StandardPropertyURN.X.toString(),
						COORD_ACCURACY));
		encoders.put(StandardPropertyURN.Y.toString(),
				new CoordinatePropertyCodec(StandardPropertyURN.Y.toString(),
						COORD_ACCURACY));
		encoders
				.put(StandardPropertyURN.BROKENNESS.toString(),
						new IntPropertyCodec(StandardPropertyURN.BROKENNESS
								.toString()));
		encoders.put(StandardPropertyURN.IGNITION.toString(),
				new BooleanPropertyCodec(StandardPropertyURN.IGNITION
						.toString()));

		encoders.put(BlockInfoRoad.HAS_BEEN_PASSED_URN,
				new BooleanPropertyCodec(BlockInfoRoad.HAS_BEEN_PASSED_URN));

		encoders.put(BlockInfoRoad.IMPORTANCE_URN, new IntPropertyCodec(
				BlockInfoRoad.IMPORTANCE_URN));

		encoders
				.put(StandardPropertyURN.APEXES.toString(),
						new IntArrayPropertyCodec(StandardPropertyURN.APEXES
								.toString()));
		encoders.put(StandardPropertyURN.BLOCKADES.toString(),
				new EntityRefListPropertyCodec(StandardPropertyURN.BLOCKADES
						.toString()));

		encoders.put(RoutingInfoBlockade.BLOCK_INFO_URN,
				new BlockedNeighboursCodec());

		int clearRate = config.getIntValue("clear.repair.rate", 10);
		encoders.put(StandardPropertyURN.REPAIR_COST.toString(),
				new PrecisionIntPropertyCodec(StandardPropertyURN.REPAIR_COST
						.toString(), clearRate));
	}

	public PropertyCodec get(String urn) {
		PropertyCodec propertyCodec = encoders.get(urn);

		if (propertyCodec == null) {
			throw new IllegalArgumentException("No property codec found for "
					+ urn);
		} else {
			return propertyCodec;
		}
	}
}
