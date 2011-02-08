package iamrescue.routing.costs;

import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.entities.BlockInfoRoad;
import iamrescue.belief.entities.RoutingInfoBlockade;
import iamrescue.util.PositionXY;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.config.Config;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

public class ClearingCostFunction extends AbstractRoutingCostFunction {

	private IAMWorldModel worldModel;
	private double clearRate;
	private Set<EntityID> ignored;
	private Map<EntityID, Integer> alreadyClearing;
	private double defaultCostPerMetre = 0.2;

	public ClearingCostFunction(IAMWorldModel worldModel, Config config) {
		this.worldModel = worldModel;
		// clearRate = config.getIntValue("misc.clear.rate");
		clearRate = config.getIntValue("clear.repair.rate");
		ignored = new FastSet<EntityID>();
		alreadyClearing = new FastMap<EntityID, Integer>();
	}

	public void addIgnored(EntityID id) {
		this.ignored.add(id);
	}

	/**
	 * @param defaultCost
	 *            the defaultCost to set
	 */
	public void setDefaultCostPerMetre(double defaultCost) {
		this.defaultCostPerMetre = defaultCost;
	}

	public void addAlreadyClearing(EntityID id) {
		Integer integer = alreadyClearing.get(id);
		if (integer == null) {
			alreadyClearing.put(id, 1);
		} else {
			alreadyClearing.put(id, integer + 1);
		}
	}

	public void setAlreadyClearing(EntityID id, int already) {
		if (already == 0) {
			alreadyClearing.remove(id);
		} else {
			alreadyClearing.put(id, already);
		}
	}

	public int getAlreadyClearing(EntityID id) {
		Integer already = alreadyClearing.get(id);
		if (already == null) {
			return 0;
		} else {
			return already;
		}

	}

	public void clearIgnored() {
		this.ignored.clear();
	}

	public void removeIgnored(EntityID id) {
		this.ignored.remove(id);
	}

	@Override
	public double getTravelCost(Area area, PositionXY from, PositionXY to) {

		if (ignored.size() > 0 && ignored.contains(area.getID())) {
			return 0;
		}

		if (area instanceof Building) {
			return 0;
		}

		double cost = 0;

		if (area.isBlockadesDefined()) {
			Line2D path = new Line2D(from.toPoint2D(), to.toPoint2D());
			List<EntityID> blockades = area.getBlockades();
			for (EntityID id : blockades) {
				if (ignored.size() > 0 && ignored.contains(id)) {
					continue;
				}
				Blockade blockade = (Blockade) worldModel.getEntity(id);
				if (blockade == null) {
					continue;
				}

				double thisClearCost = 0;

				if (blockade.isRepairCostDefined()) {
					if (blockade instanceof RoutingInfoBlockade
							&& BlockCheckerUtil
									.shouldPreferCommunicatedBlocked(blockade,
											worldModel)) {
						if (BlockCheckerUtil.checkIfCommunicatedBlocked(
								(RoutingInfoBlockade) blockade, worldModel
										.getDefaultConverter(), from, to)) {
							thisClearCost = (int) (blockade.getRepairCost() / clearRate);
						}
					} else if (blockade.isApexesDefined()) {
						int[] apexes = blockade.getApexes();
						for (int i = 0; i < apexes.length; i = i + 2) {
							Line2D edge = new Line2D(new Point2D(apexes[i],
									apexes[i + 1]), new Point2D(apexes[(i + 2)
									% apexes.length], apexes[(i + 3)
									% apexes.length]));
							double intersection1 = edge.getIntersection(path);
							if (intersection1 >= 0 && intersection1 <= 1) {
								double intersection2 = path
										.getIntersection(edge);
								if (intersection2 >= 0 && intersection2 <= 1) {
									thisClearCost = (int) (blockade
											.getRepairCost() / clearRate);

									break;
								}
							}
						}
					}
				}

				if (alreadyClearing.size() > 0) {
					Integer alreadyClearingBlock = alreadyClearing.get(blockade
							.getID());
					if (alreadyClearingBlock != null) {
						thisClearCost = thisClearCost
								/ (alreadyClearingBlock + 1.0);
					}
				}
				// ceil is expensive, so bit of a hack here.
				cost += (int) (thisClearCost + 0.999999);

			}
		} else {
			if (area instanceof BlockInfoRoad) {
				BlockInfoRoad road = (BlockInfoRoad) area;
				if (road.isHasBeenPassedDefined() && road.hasBeenPassed()) {
					cost = 0;
				} else {
					cost = defaultCostPerMetre * from.distanceTo(to) / 1000.0;
				}
			} else {
				cost = defaultCostPerMetre * from.distanceTo(to) / 1000.0;
			}
		}

		// Already clearing?
		if (alreadyClearing.size() > 0) {
			Integer already = alreadyClearing.get(area.getID());

			if (already != null) {
				cost = cost / (already + 1);
			}
		}

		return cost;
	}
}
