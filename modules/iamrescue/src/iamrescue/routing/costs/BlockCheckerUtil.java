/**
 * 
 */
package iamrescue.routing.costs;

import static rescuecore2.misc.geometry.GeometryTools2D.nearlyZero;
import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.entities.BlockInfoRoad;
import iamrescue.belief.entities.RoutingInfoBlockade;
import iamrescue.belief.provenance.IProvenanceInformation;
import iamrescue.execution.command.IPath.BlockedState;
import iamrescue.routing.WorldModelConverter;
import iamrescue.util.PositionXY;

import java.util.ArrayList;
import java.util.List;

import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class BlockCheckerUtil {

	public static final boolean USE_3_LINES = true;

	// 3 * radius of an agent
	public static final int SAFETY_MARGIN_EITHER_SIDE = 500;
	public static final int FORWARD_MARGIN = 500;
	private static final boolean INCLUDE_CENTRE = false;

	public static final double RELAX_THRESHOLD = 0.001;

	public static BlockedState getBlockedState(Area area,
			IAMWorldModel worldModel, PositionXY from, PositionXY to) {
		return getBlockedState(area, worldModel, from, to, INCLUDE_CENTRE);
	}

	public static BlockedState getBlockedState(Area area,
			IAMWorldModel worldModel, PositionXY from, PositionXY to,
			boolean alsoCheckCentre) {

		BlockCache cache = worldModel.getBlockCache();

		/*
		 * PositionXY[] line = new PositionXY[2]; line[0] = from; line[1] = to;
		 * Geometry lineGeometry = SpatialUtils.convertApexes(line);
		 */
		BlockedState state = cache.getBlockedState(area, from, to);
		if (state != null && !alsoCheckCentre) {
			return state;
		}

		state = BlockedState.UNBLOCKED;

		if (area instanceof Road) {

			PositionXY centre = null;
			if (alsoCheckCentre && area.isXDefined() && area.isYDefined()) {
				centre = new PositionXY(area.getX(), area.getY());
				if (centre.equals(from) || centre.equals(to)) {
					// Ignore if from/to is already the centre
					centre = null;
				}
			}

			// Check if there's a block
			if (area.isBlockadesDefined()) {
				List<EntityID> blockades = area.getBlockades();
				if (blockades.size() > 0) {
					List<Line2D> linesToTest = null;
					for (EntityID id : blockades) {
						StandardEntity b = worldModel.getEntity(id);
						if (b != null && b instanceof Blockade) {
							// Test intersection
							Blockade blockade = (Blockade) b;
							// Shape shape = blockade.getShape();

							// Should we rely on communicated info?
							boolean useCommunicatedBlockInfo = shouldPreferCommunicatedBlocked(
									blockade, worldModel);

							// Boolean cached = cache
							// .getUseCommunicatedInfo(blockade);

							// if (cached == null) {

							// cache.setUseCommunicatedInfo(blockade,
							// useCommunicatedBlockInfo);
							// } else {
							// Use cached
							// useCommunicatedBlockInfo = cached;
							// }

							if (useCommunicatedBlockInfo) {
								if (checkIfCommunicatedBlocked(
										(RoutingInfoBlockade) blockade,
										worldModel.getDefaultConverter(), from,
										to)) {
									state = BlockedState.BLOCKED;
									break;
								}
							} else if (blockade.isApexesDefined()) {
								// int[] apexes = blockade.getApexes();
								// Geometry blockadeGeometry = SpatialUtils
								// .convertApexes(apexes);
								// if
								// (blockadeGeometry.intersects(lineGeometry)) {
								// return BlockedState.BLOCKED;
								// }
								if (linesToTest == null) {
									linesToTest = generateLines(from, to);
								}
								if (isBlocking(blockade, linesToTest,
										RELAX_THRESHOLD)) {
									state = BlockedState.BLOCKED;
									break;
								} else if (alsoCheckCentre && centre != null) {
									if (isBlocking(blockade, from, centre)
											|| isBlocking(blockade, to, centre)) {
										state = BlockedState.BLOCKED;
										break;
									}
								}
							} else {
								state = BlockedState.UNKNOWN;
							}
						}
					}
				}
			} else {
				// Blockades are not defined.
				state = BlockedState.UNKNOWN;
				if (area instanceof BlockInfoRoad) {
					BlockInfoRoad road = (BlockInfoRoad) area;
					if (road.isHasBeenPassedDefined()) {
						if (road.hasBeenPassed()) {
							state = BlockedState.UNBLOCKED;
						}
					}
				}

			}
		}
		if (!alsoCheckCentre) {
			cache.setBlockedStateForLastQuery(state);
		}
		return state;
	}

	public static boolean shouldPreferCommunicatedBlocked(Blockade blockade,
			IAMWorldModel worldModel) {
		if (blockade instanceof RoutingInfoBlockade) {
			RoutingInfoBlockade infoBlockade = (RoutingInfoBlockade) blockade;
			if (infoBlockade.isBlockedEdgesDefined()) {
				EntityID blockID = blockade.getID();
				IProvenanceInformation provenance = worldModel.getProvenance(
						blockID, RoutingInfoBlockade.BLOCK_INFO_URN);
				int timeStep = provenance.getLatest().getTimeStep();

				if (!blockade.isApexesDefined()
						|| worldModel.getProvenance(blockID,
								StandardPropertyURN.APEXES).getLatest()
								.getTimeStep() < timeStep) {
					return true;
				}
			}
		}
		return false;
	}

	public static List<Line2D> generateParallelLines(Line2D line, int offset,
			int forwardOffset, int numEachSide) {
		List<Line2D> list = new ArrayList<Line2D>(2 * numEachSide);
		for (int i = 1; i <= numEachSide; i++) {
			double dX = line.getDirection().getX();
			double dY = line.getDirection().getY();

			double length = Math.sqrt(dX * dX + dY * dY);
			if (length < 2 * forwardOffset) {
				return list;
			}

			Point2D fromAbove = new Point2D(line.getOrigin().getX() - offset
					* dY / length + forwardOffset * dX / length, line
					.getOrigin().getY()
					+ i * offset * dX / length + forwardOffset * dY / length);

			Point2D fromBelow = new Point2D(line.getOrigin().getX() + offset
					* dY / length + forwardOffset * dX / length, line
					.getOrigin().getY()
					- i * offset * dX / length + forwardOffset * dY / length);

			Point2D toAbove = new Point2D(fromAbove.getX()
					+ ((length - 2 * forwardOffset) / length) * dX, fromAbove
					.getY()
					+ ((length - 2 * forwardOffset) / length) * dY);
			Point2D toBelow = new Point2D(fromBelow.getX()
					+ ((length - 2 * forwardOffset) / length) * dX, fromBelow
					.getY()
					+ ((length - 2 * forwardOffset) / length) * dY);

			Line2D lineAbove = new Line2D(fromAbove, toAbove);
			Line2D lineBelow = new Line2D(fromBelow, toBelow);
			list.add(lineAbove);
			list.add(lineBelow);
		}
		return list;
	}

	public static boolean checkIfIntersecting(double firstLineX1,
			double firstLineY1, double firstLineX2, double firstLineY2,
			double secondLineX1, double secondLineY1, double secondLineX2,
			double secondLineY2, double relaxThreshold) {

		double d1 = getIntersectionPoint(firstLineX1, firstLineY1, firstLineX2,
				firstLineY2, secondLineX1, secondLineY1, secondLineX2,
				secondLineY2);

		if (d1 < 0 - relaxThreshold || d1 > 1 + relaxThreshold) {

			return false;

		}

		double d2 = getIntersectionPoint(secondLineX1, secondLineY1,
				secondLineX2, secondLineY2, firstLineX1, firstLineY1,
				firstLineX2, firstLineY2);

		return (d2 >= 0 - relaxThreshold && d2 <= 1 + relaxThreshold);
	}

	public static double getIntersectionPoint(double firstLineX1,
			double firstLineY1, double firstLineX2, double firstLineY2,
			double secondLineX1, double secondLineY1, double secondLineX2,
			double secondLineY2) {

		double bxax = firstLineX2 - firstLineX1;
		double byay = firstLineY2 - firstLineY1;

		double dxcx = secondLineX2 - secondLineX1;
		double dycy = secondLineY2 - secondLineY1;

		double cxax = secondLineX1 - firstLineX1;
		double cyay = secondLineY1 - firstLineY1;

		double d = (bxax * dycy) - (byay * dxcx);
		double t = (cxax * dycy) - (cyay * dxcx);

		if (nearlyZero(d)) {
			// d is close to zero: lines are parallel so no intersection
			return Double.NaN;
		}
		return t / d;
	}

	private static boolean isBlocking(Blockade block, List<Line2D> linesToTest,
			double relaxThreshold) {
		if (!block.isApexesDefined()) {
			throw new IllegalArgumentException("The apexes of this "
					+ "blockade are not defined: " + block);
		}
		int[] apexes = block.getApexes();
		for (int i = 0; i < apexes.length; i = i + 2) {
			// Line2D edge = new Line2D(new Point2D(apexes[i], apexes[i + 1]),
			// new Point2D(apexes[(i + 2) % apexes.length], apexes[(i + 3)
			// % apexes.length]));
			double edgeOriginX1 = apexes[i];
			double edgeOriginY1 = apexes[i + 1];
			double edgeEndX1 = apexes[(i + 2) % apexes.length];
			double edgeEndY1 = apexes[(i + 3) % apexes.length];
			for (Line2D line : linesToTest) {
				if (checkIfIntersecting(line.getOrigin().getX(), line
						.getOrigin().getY(), line.getEndPoint().getX(), line
						.getEndPoint().getY(), edgeOriginX1, edgeOriginY1,
						edgeEndX1, edgeEndY1, relaxThreshold)) {
					return true;
				}
			}
		}

		/*
		 * for (Line2D line : lines) { double d1 =
		 * line.getIntersection(lineToHuman); double d2 =
		 * lineToHuman.getIntersection(line); if (d2 >= 0 && d2 <= 1 && d1 >= 0
		 * && d1 <= 1) { return true; } }
		 */
		return false;

	}

	private static List<Line2D> generateLines(PositionXY from, PositionXY to) {
		List<Line2D> linesToTest = new ArrayList<Line2D>(3);

		Line2D lineToHuman = new Line2D(from.toPoint2D(), to.toPoint2D());
		linesToTest.add(lineToHuman);

		if (USE_3_LINES) {
			linesToTest.addAll(generateParallelLines(lineToHuman,
					SAFETY_MARGIN_EITHER_SIDE, FORWARD_MARGIN, 1));
		}

		return linesToTest;
	}

	public static boolean isBlocking(Blockade block, PositionXY from,
			PositionXY to) {

		List<Line2D> linesToTest = generateLines(from, to);

		return isBlocking(block, linesToTest, RELAX_THRESHOLD);

	}

	/**
	 * @param blockade
	 * @param from
	 * @param to
	 * @return
	 */
	public static boolean checkIfCommunicatedBlocked(
			RoutingInfoBlockade blockade, WorldModelConverter converter,
			PositionXY from, PositionXY to) {
		// int[] blockedEdges = blockade.getBlockedEdges();
		PositionXY[] blockedEdgePositions = blockade
				.getBlockedEdgePositions(converter);
		for (int i = 0; i < blockedEdgePositions.length; i = i + 2) {
			PositionXY first = blockedEdgePositions[i];
			PositionXY second = blockedEdgePositions[i + 1];
			if (first.equals(from)) {
				if (second.equals(to)) {
					return true;
				}
			} else if (second.equals(from)) {
				if (first.equals(to)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isIntersectingSafe(Area area, Line2D rayToTest,
			boolean extendToInfinity) {
		int[] apexes = area.getApexList();
		for (int i = 0; i < apexes.length; i = i + 2) {
			// Line2D edge = new Line2D(new Point2D(apexes[i], apexes[i + 1]),
			// new Point2D(apexes[(i + 2) % apexes.length], apexes[(i + 3)
			// % apexes.length]));
			double edgeOriginX1 = apexes[i];
			double edgeOriginY1 = apexes[i + 1];
			double edgeEndX1 = apexes[(i + 2) % apexes.length];
			double edgeEndY1 = apexes[(i + 3) % apexes.length];

			Line2D edge = new Line2D(edgeOriginX1,edgeOriginY1,edgeEndX1-edgeOriginX1,edgeEndY1-edgeOriginY1);
			
			double intersection = edge.getIntersection(rayToTest);
			
			if (!(intersection >= 0 && intersection <= 1)) {
				continue;
			} 
			
			double intersection2  = rayToTest.getIntersection(edge);
			
			if (extendToInfinity) {
				if (intersection2 >= 0) {
					return true;
				}
			} else {
				if (intersection2 >= 0 && intersection2 <= 1) {
					return true;
				}
			}
		}

		/*
		 * for (Line2D line : lines) { double d1 =
		 * line.getIntersection(lineToHuman); double d2 =
		 * lineToHuman.getIntersection(line); if (d2 >= 0 && d2 <= 1 && d1 >= 0
		 * && d1 <= 1) { return true; } }
		 */
		return false;
	}
	
	public static boolean isIntersecting(Area area, Line2D rayToTest,
			boolean extendToInfinity) {
		int[] apexes = area.getApexList();
		for (int i = 0; i < apexes.length; i = i + 2) {
			// Line2D edge = new Line2D(new Point2D(apexes[i], apexes[i + 1]),
			// new Point2D(apexes[(i + 2) % apexes.length], apexes[(i + 3)
			// % apexes.length]));
			double edgeOriginX1 = apexes[i];
			double edgeOriginY1 = apexes[i + 1];
			double edgeEndX1 = apexes[(i + 2) % apexes.length];
			double edgeEndY1 = apexes[(i + 3) % apexes.length];

			double intersection = getIntersectionPoint(edgeOriginX1,
					edgeOriginY1, edgeEndX1, edgeEndY1, rayToTest.getOrigin()
							.getX(), rayToTest.getOrigin().getY(), rayToTest
							.getEndPoint().getX(), rayToTest.getEndPoint()
							.getY());


			if (!(intersection >= 0 && intersection <= 1)) {
				continue;
			} 

			double intersectionPoint2 = getIntersectionPoint(rayToTest
					.getOrigin().getX(), rayToTest.getOrigin().getY(),
					rayToTest.getEndPoint().getX(), rayToTest.getEndPoint()
							.getY(), edgeOriginX1, edgeOriginY1, edgeEndX1,
					edgeEndY1);

			if (extendToInfinity) {
				if (intersectionPoint2 >= 0) {
					return true;
				}
			} else {
				if (intersectionPoint2 >= 0 && intersectionPoint2 <= 1) {
					return true;
				}
			}
		}

		/*
		 * for (Line2D line : lines) { double d1 =
		 * line.getIntersection(lineToHuman); double d2 =
		 * lineToHuman.getIntersection(line); if (d2 >= 0 && d2 <= 1 && d1 >= 0
		 * && d1 <= 1) { return true; } }
		 */
		return false;

	}
}