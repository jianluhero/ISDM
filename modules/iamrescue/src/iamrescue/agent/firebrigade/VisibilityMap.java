package iamrescue.agent.firebrigade;

import iamrescue.belief.IAMWorldModel;
import iamrescue.routing.costs.BlockCheckerUtil;
import iamrescue.util.PositionXY;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public class VisibilityMap {

	private Map<EntityID, Set<EntityID>> buildingToRoadMap;

	private static final Logger LOGGER = Logger.getLogger(VisibilityMap.class);

	public VisibilityMap(IAMWorldModel worldModel, int viewDistance) {

		long time = System.currentTimeMillis();

		LOGGER.info("Starting building visbility map.");

		Collection<StandardEntity> buildings = worldModel.getEntitiesOfType(
				StandardEntityURN.BUILDING, StandardEntityURN.REFUGE,
				StandardEntityURN.POLICE_OFFICE,
				StandardEntityURN.FIRE_STATION,
				StandardEntityURN.AMBULANCE_CENTRE);
		/*
		 * Collection<StandardEntity> roads = worldModel
		 * .getEntitiesOfType(StandardEntityURN.ROAD);
		 */

		buildingToRoadMap = new FastMap<EntityID, Set<EntityID>>(buildings
				.size());

		for (StandardEntity building : buildings) {
			Set<EntityID> canView = new FastSet<EntityID>();

			// Get nearby roads
			Collection<StandardEntity> possibleExtinguishPositions = worldModel
					.getObjectsInRange(building, viewDistance);

			PositionXY buildingPosition = new PositionXY(building
					.getLocation(worldModel));

			for (StandardEntity standardEntity : possibleExtinguishPositions) {
				if (standardEntity instanceof Road) {
					Road road = (Road) standardEntity;

					PositionXY locationXY = new PositionXY(road.getX(), road
							.getY());
					if (locationXY.distanceTo(buildingPosition) < viewDistance) {
						Line2D myLineOfSight = new Line2D(locationXY
								.toPoint2D(), buildingPosition.toPoint2D());
						boolean blocking = false;
						for (StandardEntity inRange : possibleExtinguishPositions) {
							if (inRange instanceof Building
									&& !inRange.getID()
											.equals(building.getID())) {
								if (BlockCheckerUtil.isIntersecting(
										(Area) inRange, myLineOfSight, false)) {
									blocking = true;
									break;
								}
							}
						}
						if (!blocking) {
							canView.add(road.getID());
						}
					}
				}
			}

			buildingToRoadMap.put(building.getID(), canView);

		}

		LOGGER.info("Done building visbility map after "
				+ (System.currentTimeMillis() - time) + " ms.");
	}

	public Set<EntityID> getRoadsToExtinguishFrom(EntityID buildingID) {
		return buildingToRoadMap.get(buildingID);
	}
}
