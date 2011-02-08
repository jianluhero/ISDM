package iamrescue.execution;

import iamrescue.agent.ISimulationTimer;
import iamrescue.belief.IAMWorldModel;
import iamrescue.execution.command.ClearCommand;
import iamrescue.execution.command.DigOutCommand;
import iamrescue.execution.command.ExtinguishCommand;
import iamrescue.execution.command.IIAMAgentCommand;
import iamrescue.execution.command.LoadCommand;
import iamrescue.execution.command.MoveCommand;
import iamrescue.execution.command.RandomMoveCommand;
import iamrescue.execution.command.RandomStepCommand;
import iamrescue.execution.command.RestCommand;
import iamrescue.execution.command.UnloadCommand;
import iamrescue.routing.util.ISpeedInfo;
import iamrescue.util.HumanMovementUtility;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.config.Config;
import rescuecore2.connection.Connection;
import rescuecore2.connection.ConnectionException;
import rescuecore2.messages.AbstractCommand;
import rescuecore2.messages.Message;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.messages.AKClear;
import rescuecore2.standard.messages.AKExtinguish;
import rescuecore2.standard.messages.AKLoad;
import rescuecore2.standard.messages.AKMove;
import rescuecore2.standard.messages.AKRescue;
import rescuecore2.standard.messages.AKRest;
import rescuecore2.standard.messages.AKUnload;
import rescuecore2.worldmodel.EntityID;

public class RC2ExecutionService implements IExecutionService {

	private static final Logger LOGGER = Logger
			.getLogger(RC2ExecutionService.class);

	private Connection connection;
	private EntityID id;
	private AbstractCommand enqueuedCommand;
	private int maxPower;
	private ISimulationTimer timer;
	private IIAMAgentCommand lastEnqueuedCommand;
	private IIAMAgentCommand lastSubmittedCommand;
	private IAMWorldModel worldModel;
	private int lastRandomMoveTime = -1;
	private int consecutiveRandomSteps = 0;
	private int sameMoveCounter = 0;
	private ISpeedInfo speedInfo;

	private boolean iamBuilding;

	private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";

	// Proportion of average speed that is classed as a no-move (over all
	// positions visited).
	private static final double MAX_NO_MOVE_PROPORTION = 0.2;

	// Proportion of average speed that is classed as a no-move (over absolute
	// distance travelled).
	private static final double MAX_NO_MOVE_ABSOLUTE_PROPORTION = 0.1;

	// When stuck, how many random steps should be attempted first before
	// starting to visit neighbours
	private static final int STEP_TRIALS = 3;

	// After how many steps of submitting the same move should I start to move
	// randomly? If set to X, the agent will move randomly on the Xth time the
	// same move is sent.
	private static final int SAME_MOVE_THRESHOLD = 8;

	// How many samples to take for finding a random side-step
	private static final int MAX_SAMPLES = 30;

	// Probability of taking a random move once the necessary conditions hold
	private static final double RANDOM_MOVE_PROBABILITY = 1;

	public RC2ExecutionService(EntityID id, Connection connection,
			Config config, ISimulationTimer timer, IAMWorldModel worldModel,
			ISpeedInfo speedInfo) {
		this.connection = connection;
		this.worldModel = worldModel;
		this.id = id;
		maxPower = config.getIntValue(MAX_POWER_KEY);
		this.timer = timer;
		setSpeedInfo(speedInfo);
		iamBuilding = worldModel.getEntity(id) instanceof Building;
	}

	public void setSpeedInfo(ISpeedInfo speedInfo) {
		this.speedInfo = speedInfo;
	}

	private void sendMessage(Message message) {
		try {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Sending command: " + message);
			}
			connection.sendMessage(message);
		} catch (ConnectionException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void execute(DigOutCommand rescueCommand) {
		AKRescue rescue = new AKRescue(id, timer.getTime(), rescueCommand
				.getObjectToDigOut().getID());
		this.lastEnqueuedCommand = rescueCommand;
		enqueueCommand(rescue);
	}

	@Override
	public void execute(UnloadCommand unloadCommand) {
		AKUnload unload = new AKUnload(id, timer.getTime());
		this.lastEnqueuedCommand = unloadCommand;
		enqueueCommand(unload);
	}

	@Override
	public void execute(LoadCommand loadCommand) {
		AKLoad load = new AKLoad(id, timer.getTime(), loadCommand
				.getCivilianToLoad().getID());
		this.lastEnqueuedCommand = loadCommand;
		enqueueCommand(load);
	}

	@Override
	public void execute(ClearCommand clearCommand) {
		AKClear clear = new AKClear(id, timer.getTime(), clearCommand
				.getBlockadeToClear().getID());
		this.lastEnqueuedCommand = clearCommand;
		enqueueCommand(clear);
	}

	@Override
	public void execute(ExtinguishCommand extinguishCommand) {
		Building building = extinguishCommand.getBuildingToExtinguish();
		double percentage = extinguishCommand.getPercentageOfFullPower();
		double power = maxPower * percentage;

		int time = timer.getTime();
		EntityID bId = building.getID();
		AKExtinguish ex = new AKExtinguish(id, time, bId, (int) power);
		this.lastEnqueuedCommand = extinguishCommand;
		enqueueCommand(ex);
	}

	@Override
	public void execute(MoveCommand moveCommand) {
		AKMove move = new AKMove(id, timer.getTime(), moveCommand.getPath()
				.getLocations(), moveCommand.getPath().getEndPosition().getX(),
				moveCommand.getPath().getEndPosition().getY());
		this.lastEnqueuedCommand = moveCommand;
		enqueueCommand(move);
	}

	private void enqueueMove(MoveCommand moveCommand) {
		AKMove move = new AKMove(id, timer.getTime(), moveCommand.getPath()
				.getLocations(), moveCommand.getPath().getEndPosition().getX(),
				moveCommand.getPath().getEndPosition().getY());
		this.lastEnqueuedCommand = moveCommand;
		this.enqueuedCommand = move;
	}

	private void enqueueCommand(AbstractCommand command) {
		assert enqueuedCommand == null : "Warning : multiple commands submitted in the same timestep";
		this.enqueuedCommand = command;
	}

	@Override
	public void flushCommands() {
		// enqueuedCommand = generateRandomStep(MAX_SAMPLES);

		if (enqueuedCommand == null) {
			if (!iamBuilding) {
				LOGGER.error("Warning : agent has not submitted "
						+ "any commands in this timestep");
			}
		} else {
			boolean sentMove = false;
			if (timer.getTime() > 3 && needRandomMove()) {
				// Do not step randomly twice in a row.
				if (lastRandomMoveTime != timer.getTime() - 1) {
					sentMove = sendRandomMove();
				}
			}

			if (!sentMove) {
				sendMessage(enqueuedCommand);
				this.lastSubmittedCommand = lastEnqueuedCommand;

			}
			enqueuedCommand = null;
		}
	}

	private AKMove generateRandomStep(int maxTries) {
		Human me = (Human) worldModel.getEntity(id);
		EntityID position = me.getPosition();
		// Area I am on
		Area area = (Area) worldModel.getEntity(position);
		Shape shape = area.getShape();
		Rectangle bounds = shape.getBounds();
		boolean done = false;
		int counter = 1;

		while (!done && counter <= maxTries) {
			int x = (int) (bounds.getMinX() + Math.random()
					* (bounds.getMaxX() - bounds.getMinX()));
			int y = (int) (bounds.getMinY() + Math.random()
					* (bounds.getMaxY() - bounds.getMinY()));
			if (shape.contains(new Point2D.Float(x, y))) {
				done = true;

				List<EntityID> pathLocations = new ArrayList<EntityID>();
				pathLocations.add(position);

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Generated random move to " + x + ", " + y);
				}

				return new AKMove(id, timer.getTime(), pathLocations, x, y);
				/*
				 * this.lastEnqueuedCommand = null; this.enqueuedCommand = move;
				 */
				// 

			}
			counter++;
		}
		return null;
	}

	private AKMove generateRandomNeighbourStep() {
		Human me = (Human) worldModel.getEntity(id);
		EntityID position = me.getPosition();
		// Area I am on
		Area area = (Area) worldModel.getEntity(position);

		List<EntityID> neighbours = area.getNeighbours();

		if (neighbours.size() == 0) {
			LOGGER.error("Could not create random move on "
					+ area.getFullDescription());
			return null;
		} else {
			Set<EntityID> tried = new FastSet<EntityID>();
			EntityID target = null;
			boolean ignore = false;
			List<EntityID> pathLocations;
			do {
				ignore = false;
				target = neighbours.get((int) (Math.random() * neighbours
						.size()));
				pathLocations = new ArrayList<EntityID>();
				pathLocations.add(position);
				pathLocations.add(target);
				tried.add(target);
				StandardEntity entity = worldModel.getEntity(target);
				if (entity instanceof Building) {
					Building b = (Building) entity;
					if (b.isFierynessDefined() && b.getFieryness() >= 1
							&& b.getFieryness() <= 3) {
						ignore = true;
					}
				}
			} while (ignore && tried.size() < neighbours.size());

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Generated random move to neighbour " + target);
			}

			return new AKMove(id, timer.getTime(), pathLocations);
		}
	}

	/**
	 * 
	 */
	private boolean sendRandomMove() {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Submitting random move, "
					+ "as enqueued command has been submitted repeatedly: "
					+ enqueuedCommand);
		}

		boolean randomlySteppedLastTime = (lastRandomMoveTime >= timer
				.getTime() - 2);
		boolean skipStep = randomlySteppedLastTime
				&& (consecutiveRandomSteps >= STEP_TRIALS);

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Last random step: " + lastRandomMoveTime);
			LOGGER.info("Consecutive steps: " + consecutiveRandomSteps);
		}

		AKMove move = null;

		if (!skipStep) {
			move = generateRandomStep(MAX_SAMPLES);
			if (move != null) {
				sendMessage(move);
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("Sent random step: " + move);
				}
			}
		}

		if (move == null) {
			// Go to random neighbour
			move = generateRandomNeighbourStep();
			sendMessage(move);
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Sent random neighbour step: " + move);
			}
		}

		if (move != null) {
			if (randomlySteppedLastTime) {
				consecutiveRandomSteps++;
			} else {
				consecutiveRandomSteps = 1;
			}
			lastRandomMoveTime = timer.getTime();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @return
	 */
	private boolean needRandomMove() {
		if (timer.getTime() > 4) {
			if (lastEnqueuedCommand != null && lastSubmittedCommand != null) {
				if (lastEnqueuedCommand instanceof MoveCommand
						&& lastSubmittedCommand instanceof MoveCommand) {
					MoveCommand lastEnqMove = (MoveCommand) lastEnqueuedCommand;
					MoveCommand lastSubMove = (MoveCommand) lastSubmittedCommand;

					List<EntityID> enqLocations = lastEnqMove.getPath()
							.getLocations();
					List<EntityID> subLocations = lastSubMove.getPath()
							.getLocations();

					if (enqLocations.equals(subLocations)) {
						sameMoveCounter++;
						return needRandomMoveGivenSameCommand();
					} else {
						// Did not submit same move
						sameMoveCounter = 0;
					}
				}
			}
		}
		return false;
	}

	/**
	 * @return
	 */
	private boolean needRandomMoveGivenSameCommand() {
		boolean conditionsMet = false;
		if (sameMoveCounter >= SAME_MOVE_THRESHOLD) {
			conditionsMet = true;
		} else {
			StandardEntity me = worldModel.getEntity(id);
			if (me instanceof Human) {

				int distanceJustTravelled = HumanMovementUtility
						.getDistanceJustTravelled((Human) me, worldModel);
				if (distanceJustTravelled == -1) {
					LOGGER.error("Could not determine"
							+ " distance last travelled");
				} else {
					if (distanceJustTravelled < MAX_NO_MOVE_PROPORTION
							* speedInfo.getDistancePerTimeStep()) {
						conditionsMet = true;
					}
				}

				if (!conditionsMet) {
					int absoluteDistanceTravelled = HumanMovementUtility
							.getAbsoluteDistanceJustTravelled((Human) me,
									worldModel, timer);
					if (absoluteDistanceTravelled == -1) {
						LOGGER.error("Could not determine"
								+ " absolute distance " + "last travelled");
					} else {
						if (absoluteDistanceTravelled < MAX_NO_MOVE_ABSOLUTE_PROPORTION
								* speedInfo.getDistancePerTimeStep()) {
							conditionsMet = true;
						}
					}
				}
			}
		}
		if (conditionsMet) {
			return Math.random() <= RANDOM_MOVE_PROBABILITY;
		} else {
			return false;
		}
	}

	public IIAMAgentCommand getLastSubmittedCommand() {
		return lastSubmittedCommand;
	}

	@Override
	public void performCommand(IIAMAgentCommand command)
			throws UnknownCommandException, IllegalStateException {
		command.checkValidity();
		command.execute(this);
	}

	@Override
	public void execute(RestCommand command) {
		this.lastEnqueuedCommand = command;
		enqueueCommand(new AKRest(id, timer.getTime()));
	}

	@Override
	public int getLastRandomMoveTime() {
		return lastRandomMoveTime;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.execution.IExecutionService#execute(iamrescue.execution.command
	 * .RandomMoveCommand)
	 */
	@Override
	public void execute(RandomMoveCommand randomMoveCommand) {
		AKMove move = generateRandomNeighbourStep();
		this.lastEnqueuedCommand = randomMoveCommand;
		if (move == null) {
			move = generateRandomStep(MAX_SAMPLES);
			if (move == null) {
				LOGGER.warn("Could not generate random move. "
						+ "Sending rest instead.");
				execute(new RestCommand());
				return;
			}
		}
		enqueueCommand(move);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * iamrescue.execution.IExecutionService#execute(iamrescue.execution.command
	 * .RandomStepCommand)
	 */
	@Override
	public void execute(RandomStepCommand randomStepCommand) {
		AKMove move = generateRandomStep(MAX_SAMPLES);
		this.lastEnqueuedCommand = randomStepCommand;
		if (move == null) {
			move = generateRandomNeighbourStep();
			if (move == null) {
				LOGGER.warn("Could not generate random step. "
						+ "Sending rest instead.");
				execute(new RestCommand());
				return;
			}
		}
		enqueueCommand(move);
	}

	@Override
	public boolean NotSubmittedCommand() {
		return enqueuedCommand == null;
	}

	@Override
	public AbstractCommand getEnqueuedCommand() {
		return enqueuedCommand;
	}

	/**
	 * @return the consecutiveRandomSteps
	 */
	public int getConsecutiveRandomSteps() {
		return consecutiveRandomSteps;
	}

}
