package iamrescue.agent.police;

import iamrescue.agent.AbstractIAMAgent;
import iamrescue.agent.police.goals.SimpleClearingGoal;
import iamrescue.execution.command.ClearCommand;
import iamrescue.execution.command.IPath;
import iamrescue.execution.command.MoveCommand;
import iamrescue.routing.IRoutingModule;
import iamrescue.routing.Path;
import iamrescue.routing.costs.SimpleDistanceRoutingCostFunction;
import iamrescue.routing.dijkstra.BidirectionalDijkstrasRoutingModule;
import iamrescue.routing.queries.QueryFactory;
import iamrescue.util.PositionXY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javolution.util.FastList;

import org.apache.log4j.Logger;

import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

public class SimpleIAMPoliceForce extends AbstractIAMAgent<PoliceForce> {

	private static final String DISTANCE_KEY = "clear.repair.distance";

	private static final Logger LOGGER = Logger
			.getLogger(SimpleIAMPoliceForce.class);

	private PoliceTaskModule taskModule;
	private IRoutingModule clearRoutingModule;
	private IPath lastPath = null;
	private int distance;

	/*
	 * (non-Javadoc)
	 * 
	 * @see agent.AbstractIAMAgent#postConnect()
	 */
	@Override
	protected void postConnect() {
		super.postConnect();
		LOGGER.debug("Postconnect");
		distance = config.getIntValue(DISTANCE_KEY);
		taskModule = new PoliceTaskModule(getWorldModel(), me().getID(),
				getRoutingModule(), getTimer());
		LOGGER.debug("task module done");
		taskModule.initialise();
		LOGGER.debug("Initialised");

		clearRoutingModule = new BidirectionalDijkstrasRoutingModule(
				getWorldModel(),
				SimpleDistanceRoutingCostFunction.DEFAULT_NO_BLOCK_INSTANCE,
				getTimer());
		LOGGER.debug("CLearing done");
		showWorldModelViewer();
	}

	@Override
	protected void think(int time, ChangeSet changed) {
		LOGGER.debug("Start update");
		taskModule.update();
		LOGGER.debug("End update");

		LOGGER.debug("My task: " + taskModule.getMyCurrentGoal());

		// Am I on a blocked road?
		Area location = (Area) me().getPosition(getWorldModel());
		Blockade block = getTargetBlockade(location, distance);
		if (block != null) {
			LOGGER.debug("Trying to clear");
			ClearCommand clear = new ClearCommand();
			clear.setBlockadeToClear(block);
			getExecutionService().execute(clear);
			return;
		} else {
			// Check if I can step towards a blockade
			if (location.isBlockadesDefined()) {
				List<EntityID> blockades = location.getBlockades();
				for (EntityID entityID : blockades) {
					MoveCommand move = new MoveCommand();
					Blockade b = (Blockade) getWorldModel().getEntity(entityID);
					if (b.getRepairCost() == 0) {
						continue;
					}
					List<PositionXY> positions = new ArrayList<PositionXY>();
					positions.add(new PositionXY(me().getLocation(
							getWorldModel())));
					positions
							.add(new PositionXY(b.getLocation(getWorldModel())));

					move.setPath(new Path(Collections.singletonList(location
							.getID()), positions));
					getExecutionService().execute(move);
					LOGGER.debug("Moving to blockade " + b + " at "
							+ b.getLocation(getWorldModel()));
					return;
				}
			}
		}

		// Check next task
		SimpleClearingGoal nextGoal = taskModule.getMyCurrentGoal();
		if (nextGoal != null) {
			Collection<EntityID> targets = new FastList<EntityID>();

			// Plan clearing path to goal
			if (nextGoal.getTarget().equals(me().getID())) {
				for (StandardEntity se : getWorldModel().getEntitiesOfType(
						StandardEntityURN.REFUGE)) {
					targets.add(se.getID());
				}
			} else {
				targets.add(nextGoal.getTarget());
			}

			IPath path = clearRoutingModule.findShortestPath(QueryFactory
					.createQuery(getID(), targets));

			if (path.isValid()) {

				if (lastPath == null || !path.equals(lastPath)) {
					lastPath = path;
					MoveCommand moveCommand = new MoveCommand();
					moveCommand.setPath(path);
					getExecutionService().execute(moveCommand);
					LOGGER.debug("Moving to goal " + nextGoal);
					return;
				}
			} else {
				LOGGER.warn("Invalid path produced by goal " + nextGoal);
			}
		}
		LOGGER.debug("Searching");
		doDefaultSearch();

		/*
		 * Random r = new Random();
		 * 
		 * // Walk randomly to a blocked road
		 * 
		 * IPath path = null; while (path == null) { EntityID destination =
		 * null; List<Area> blocked = getBlockedAreas(); if (blocked.size() > 0)
		 * { destination = blocked.get(r.nextInt(blocked.size())).getID(); }
		 * else { blocked = getUnknownAreas(); if (blocked.size() > 0) {
		 * destination = blocked.get(r.nextInt(blocked.size())) .getID(); } else
		 * { // Random node short min =
		 * getWorldModel().getShortIndex().getMinID(); short max =
		 * getWorldModel().getShortIndex().getMaxID(); short shortID = (short)
		 * (min + (Math.random() * (max - min))); destination =
		 * getWorldModel().getEntity( getWorldModel().getShortIndex()
		 * .getEntityID(shortID)).getID(); } } LOGGER.debug("Random move from "
		 * + me() + " to " + getWorldModel().getEntity(destination));
		 * 
		 * path = clearRoutingModule.findShortestPath(QueryFactory
		 * .createQuery(getID(), Collections.singleton(destination))); if
		 * (!path.isValid()) { path = null; } } MoveCommand moveCommand = new
		 * MoveCommand(); moveCommand.setPath(path);
		 * getExecutionService().execute(moveCommand);
		 * LOGGER.debug("Random move");
		 */
	}

	private List<Area> getUnknownAreas() {
		List<Area> areas = new ArrayList<Area>();
		for (StandardEntity se : getWorldModel().getEntitiesOfType(
				StandardEntityURN.ROAD)) {
			Area area = (Area) se;
			if (!area.isBlockadesDefined()) {
				areas.add(area);
			}
		}
		return areas;

	}

	private List<Area> getBlockedAreas() {
		List<Area> areas = new ArrayList<Area>();
		for (StandardEntity se : getWorldModel().getEntitiesOfType(
				StandardEntityURN.ROAD)) {
			Area area = (Area) se;
			if (area.isBlockadesDefined() && area.getBlockades().size() > 0) {
				areas.add(area);
			}
		}
		for (StandardEntity se : getWorldModel().getEntitiesOfType(
				StandardEntityURN.BUILDING)) {
			Area area = (Area) se;
			if (area.isBlockadesDefined() && area.getBlockades().size() > 0) {
				areas.add(area);
			}
		}
		return areas;
	}

	/*
	 * private Blockade getTargetBlockade() {
	 * Logger.debug("Looking for target blockade"); Area location = (Area)
	 * self().getPosition(getWorldModel());
	 * Logger.debug("Looking in current location"); Blockade result =
	 * getTargetBlockade(location, distance); if (result != null) { return
	 * result; } Logger.debug("Looking in neighbouring locations"); for
	 * (EntityID next : location.getNeighbours()) { location = (Area)
	 * model.getEntity(next); result = getTargetBlockade(location, distance); if
	 * ) (result != null) { return result; } } return null; }
	 */

	private Blockade getTargetBlockade(Area area, int maxDistance) {
		LOGGER.debug("Looking for nearest blockade in " + area);
		if (!area.isBlockadesDefined()) {
			LOGGER.debug("Blockades undefined");
			return null;
		}
		List<EntityID> ids = area.getBlockades();
		// Find the first blockade that is in range.

		PoliceForce self = (PoliceForce) me();
		int x = self.getX();
		int y = self.getY();
		for (EntityID next : ids) {
			Blockade b = (Blockade) model.getEntity(next);
			if (b.isApexesDefined()) {
				double d = findDistanceTo(b, x, y);
				LOGGER.debug("Distance to " + b + " = " + d);
				if (maxDistance < 0 || d < maxDistance) {
					LOGGER.debug("In range");
					return b;
				}
			}
		}
		LOGGER.debug("No blockades in range");
		return null;
	}

	private int findDistanceTo(Blockade b, int x, int y) {
		LOGGER.debug("Finding distance to " + b + " from " + x + ", " + y);
		List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D
				.vertexArrayToPoints(b.getApexes()), true);
		double best = Double.MAX_VALUE;
		Point2D origin = new Point2D(x, y);
		for (Line2D next : lines) {
			Point2D closest = GeometryTools2D.getClosestPointOnSegment(next,
					origin);
			double d = GeometryTools2D.getDistance(origin, closest);
			LOGGER.debug("Next line: " + next + ", closest point: " + closest
					+ ", distance: " + d);
			if (d < best) {
				best = d;
				LOGGER.debug("New best distance");
			}

		}
		return (int) best;
	}
	
	
	protected List<StandardEntityURN> getAgentTypes() {
		return Collections.singletonList(StandardEntityURN.POLICE_FORCE);
	}
	

	/* (non-Javadoc)
	 * @see iamrescue.agent.AbstractIAMAgent#fallback(int, rescuecore2.worldmodel.ChangeSet)
	 */
	@Override
	protected void fallback(int time, ChangeSet changed) {
		// TODO Auto-generated method stub
		
	}
}
