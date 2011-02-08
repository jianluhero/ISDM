package iamrescue.domain;

/**
 * This enum contains all property keys an Entity can have
 * 
 * @author rs06r
 * 
 */
public enum RescueObjectProperty {
	WIND_DIRECTION {
		@Override
		public Class<?> getValueType() {
			return Short.class;
		}
	},
	WIND_FORCE {
		@Override
		public Class<?> getValueType() {
			return Short.class;
		}
	},
	REPAIR_COST {
		@Override
		public Class<?> getValueType() {
			return Byte.class;
		}
	},
	BLOCK {
		@Override
		public Class<?> getValueType() {
			return Integer.class;
		}
	},
	// POSITION_XY {
	// @Override
	// public Class<?> getValueType() {
	// return PositionXY.class;
	// }
	// },
	// POSITION_HISTORY, POSITION {
	// @Override
	// public Class<?> getValueType() {
	// return ISpatialObject.class;
	// }
	// },
	BURIEDNESS {
		@Override
		public Class<?> getValueType() {
			return Byte.class;
		}
	},
	DAMAGE {
		@Override
		public Class<?> getValueType() {
			return Short.class;
		}

		@Override
		public int getMaxValue() {
			return 1500;
		}

		@Override
		public int getMinValue() {
			return 0;
		}
	},
	DIRECTION {
		@Override
		public Class<?> getValueType() {
			return Short.class;
		}
	},
	HP {
		@Override
		public Class<?> getValueType() {
			return Short.class;
		}

		@Override
		public int getMaxValue() {
			return 10000;
		}

		@Override
		public int getMinValue() {
			return 0;
		}
	},
	POSITION_EXTRA {
		@Override
		public Class<?> getValueType() {
			return Integer.class;
		}
	},
	STAMINA {
		@Override
		public Class<?> getValueType() {
			return Short.class;
		}
	},
	WATER_QUANTITY {
		@Override
		public Class<?> getValueType() {
			return Short.class;
		}
	},
	IGNITED {
		@Override
		public Class<?> getValueType() {
			return Byte.class;
		}
	},
	FIERYNESS {
		@Override
		public Class<?> getValueType() {
			return Byte.class;
		}
	},
	TEMPERATURE {
		@Override
		public int getMaxValue() {
			return 1500;
		}

		@Override
		public int getMinValue() {
			return 0;
		}

		@Override
		public Class<?> getValueType() {
			return Short.class;
		}
	},
	BROKENNESS {
		@Override
		public Class<?> getValueType() {
			return Byte.class;
		}

		@Override
		public int getMaxValue() {
			return 100;
		}

		@Override
		public int getMinValue() {
			return 0;
		}
	},
	KNOWN_TO_BE_PASSABLE_FROM_HEAD {
		@Override
		public Class<?> getValueType() {
			return Boolean.class;
		}
	},
	KNOWN_TO_BE_PASSABLE_FROM_TAIL {
		@Override
		public Class<?> getValueType() {
			return Boolean.class;
		}
	},
	ANNOTATION_VALUE, GROUP_CHANGE, SENSE_POSITIONS, POSSIBLE_LOCATIONS, CARS_PASS_TO_HEAD, CARS_PASS_TO_TAIL, HUMANS_PASS_TO_TAIL, HUMANS_PASS_TO_HEAD {
		@Override
		public int getMinValue() {
			return 0;
		}

		@Override
		public int getMaxValue() {
			return 1000;
		}

		@Override
		public Class<?> getValueType() {
			return Short.class;
		}

	},
	OCCUPANTS, LAST_POSITIONS;

	public Class<?> getValueType() {
		throw new UnsupportedOperationException();
	}

	public int getMaxValue() {
		throw new UnsupportedOperationException();
	}

	public int getMinValue() {
		throw new UnsupportedOperationException();
	}
}
