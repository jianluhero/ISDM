package iamrescue.util.blocks;

import iamrescue.belief.IAMWorldModel;
import iamrescue.execution.command.IPath;
import iamrescue.routing.costs.BlockCheckerUtil;
import iamrescue.util.PositionXY;

import java.util.ArrayList;
import java.util.List;

import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.worldmodel.EntityID;

public class BlockDetectionUtil {
	public static List<Blockade> getObstructingBlockades(Area area,
			PositionXY from, PositionXY to, IAMWorldModel worldModel) {
		List<Blockade> obstructions = new ArrayList<Blockade>();

		if (area.isBlockadesDefined()) {
		//	Line2D path = new Line2D(from.toPoint2D(), to.toPoint2D());
			List<EntityID> blockades = area.getBlockades();
			for (EntityID id : blockades) {

				Blockade blockade = (Blockade) worldModel.getEntity(id);
				if (blockade != null && blockade.isApexesDefined()
						&& blockade.isRepairCostDefined()) {
					
					if (BlockCheckerUtil.isBlocking(blockade, from, to)) {
						obstructions.add(blockade);
					}
					
					/*int[] apexes = blockade.getApexes();
					for (int i = 0; i < apexes.length; i = i + 2) {
						Line2D edge = new Line2D(new Point2D(apexes[i],
								apexes[i + 1]), new Point2D(apexes[(i + 2)
								% apexes.length], apexes[(i + 3)
								% apexes.length]));
						double intersection1 = edge.getIntersection(path);
						if (intersection1 >= 0 && intersection1 <= 1) {
							double intersection2 = path.getIntersection(edge);
							if (intersection2 >= 0 && intersection2 <= 1) {
								obstructions.add(blockade);
								break;
							}
						}
					}*/
				}
			}
		}
		return obstructions;
	}

	private static double getDistanceToBlockade(PositionXY position,
			Blockade blockade, double threshold) {
		PositionXY blockadePosition = new PositionXY(blockade.getX(),blockade.getY());
		double bestDistance = position.distanceTo(blockadePosition) ;
		if (bestDistance <= threshold) {
			return bestDistance;
		}
		
		for (Line2D line : GeometryTools2D.pointsToLines(GeometryTools2D
				.vertexArrayToPoints(blockade.getApexes()), true)) {
			Point2D closest = GeometryTools2D.getClosestPointOnSegment(line,
					position.toPoint2D());
			double distance = GeometryTools2D.getDistance(position.toPoint2D(),
					closest);
			if (distance <= threshold) {
				return distance;
			}
			if (bestDistance > distance) {
				bestDistance = distance;
			}
		}

		return bestDistance;
	}

	public static double getDistanceToBlockade(PositionXY position,
			Blockade blockade) {
		return getDistanceToBlockade(position, blockade, -1);
	}

	public static boolean isWithinDistanceToBlockade(PositionXY position,
			Blockade blockade, double maxDistance) {
		return getDistanceToBlockade(position, blockade, maxDistance) <= maxDistance;
	}

	public static List<Blockade> findObstructingBlockades(IPath path,
			IAMWorldModel worldModel) {
		List<EntityID> locations = path.getLocations();
		List<PositionXY> xyPath = path.getXYPath();
		assert locations.size() == xyPath.size() - 1;

		List<Blockade> blockades = new ArrayList<Blockade>();

		for (int i = 0; i < locations.size(); i++) {
			EntityID id = locations.get(i);
			PositionXY from = xyPath.get(i);
			PositionXY to = xyPath.get(i + 1);
			List<Blockade> obstructingBlockades = getObstructingBlockades(
					(Area) worldModel.getEntity(id), from, to, worldModel);
			blockades.addAll(obstructingBlockades);
		}

		return blockades;
	}
}
