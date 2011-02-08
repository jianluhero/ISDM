package iamrescue.agent;

import iamrescue.agent.firebrigade.FastFirePredictor;
import iamrescue.agent.search.ClashRandomisedClosestSearchBehaviour;
import iamrescue.agent.search.ISearchBehaviour;
import iamrescue.belief.IAMWorldModel;
import iamrescue.belief.commupdates.IMessageHandler;
import iamrescue.belief.commupdates.IWorldModelUpdatePropagator;
import iamrescue.belief.commupdates.WorldModelCommsUpdater;
import iamrescue.belief.commupdates.WorldModelUpdatePropagator;
import iamrescue.belief.entities.BlockInfoRoadEntityFactory;
import iamrescue.belief.entities.BlockedEdgesPropertyFactory;
import iamrescue.belief.entities.KnownToBePassablePropertyFactory;
import iamrescue.belief.entities.RoutingInfoBlockade;
import iamrescue.belief.entities.RoutingInfoBlockadeEntityFactory;
import iamrescue.belief.gui.IAMWorldModelViewerComponent;
import iamrescue.belief.inference.BlockDetectingRoutingModule;
import iamrescue.belief.inference.BlockForgettingModule;
import iamrescue.belief.inference.INegativeSenseUpdater;
import iamrescue.belief.inference.NegativeSenseUpdater;
import iamrescue.belief.inference.PassableRoadsDetector;
import iamrescue.belief.inference.RoutingInfoUpdater;
import iamrescue.belief.provenance.LatestProperty;
import iamrescue.communication.ISimulationCommunicationConfiguration;
import iamrescue.communication.SimulationCommunicationConfigurationAdapter;
import iamrescue.communication.messages.Message;
import iamrescue.communication.messages.PingMessage;
import iamrescue.communication.messages.codec.CommunicationBeliefBaseAdapter;
import iamrescue.communication.messages.codec.ICommunicationBeliefBaseAdapter;
import iamrescue.communication.scenario.IAMCommunicationModule;
import iamrescue.execution.IExecutionService;
import iamrescue.execution.RC2ExecutionService;
import iamrescue.execution.command.IPath;
import iamrescue.routing.IRoutingModule;
import iamrescue.routing.costs.SimpleDistanceRoutingCostFunction;
import iamrescue.routing.dijkstra.BidirectionalDijkstrasRoutingModule;
import iamrescue.routing.dijkstra.SimpleDijkstrasRoutingModule;
import iamrescue.routing.util.ISpeedInfo;
import iamrescue.routing.util.SpeedLearningModule;
import iamrescue.util.IntervalTimer;

import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import javolution.util.FastList;
import javolution.util.FastSet;

import org.apache.log4j.Logger;

import rescuecore2.components.AbstractAgent;
import rescuecore2.messages.Command;
import rescuecore2.messages.control.KASense;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardPropertyURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

public abstract class AbstractIAMAgent<E extends StandardEntity> extends
		AbstractAgent<IAMWorldModel, E> {

	private static final boolean NAME_THREADS = true;
	private static final boolean USE_BLOCK_DETECTION = true;
	private static final boolean USE_FALLBACK_BEHAVIOUR = false;
	private static final int FORGET_BLOCKADES_AFTER = 50;
	private static final String IGNORE_KEY = "kernel.agents.ignoreuntil";

	private final Object SYNC_BLOCK = new Object();
	private IAMCommunicationModule communicationModule;
	private IRoutingModule routing;
	private IRoutingModule reusableRouting = null;
	private IExecutionService executionService;
	private SimulationTimer timer = new SimulationTimer();
	private WorldModelCommsUpdater worldModelCommsUpdater;
	private IWorldModelUpdatePropagator propagator;
	// private ISpatialIndex spatialIndex = null;
	private INegativeSenseUpdater negativeUpdater;
	private PassableRoadsDetector passableRoadsDetector;
	private ISearchBehaviour searchBehaviour;
	private IntervalTimer intervalTimer = new IntervalTimer();
	private static final Logger LOGGER = Logger
			.getLogger(AbstractIAMAgent.class);
	private IAMWorldModelViewerComponent viewer = null;
	private RoutingInfoUpdater blockUpdater;
	private SpeedLearningModule speedInfo;
	private SimpleDistanceRoutingCostFunction routingCostFunction;
	private ExecutorService ThinkExecution;
	private int thinkRuntime = 0;
	private int ignoreUntil;
	private boolean iAmCentre = false;
	private ShoutGenerator shoutGenerator;
	private BlockForgettingModule blockForgetter = null;
	private int fallbackRuntime;
	private boolean myTypeIsCentre;

	/**
	 * Shows a GUI containing the agent's local world model. This brings up a
	 * JFrame that remains visible throughout the simuation.
	 * 
	 */
	protected void showWorldModelViewer() {

		synchronized (SYNC_BLOCK) {
			if (viewer == null) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							JFrame frame = new JFrame("World Model view of "
									+ me());
							viewer = new IAMWorldModelViewerComponent();
							JPanel main = new JPanel(new BorderLayout());
							main.add(viewer.getGUIComponent(),
									BorderLayout.CENTER);
							frame.add(main);
							frame.pack();
							frame.setVisible(true);
							viewer.simulationStarted(config, getWorldModel(),
									getRoutingModule());
						}
					});
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	protected void showRoutingViewer() {
		showWorldModelViewer();

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				synchronized (SYNC_BLOCK) {
					viewer.showRoutingLayer();
				}
			}
		});
	}

	protected void showFireImportanceModel(FastFirePredictor firePredictor) {
		showWorldModelViewer();

		final FastFirePredictor predictor = firePredictor;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				synchronized (SYNC_BLOCK) {
					viewer.showFireLayer(predictor);
				}
			}
		});
	}

	protected void showSearchViewer() {
		showWorldModelViewer();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				synchronized (SYNC_BLOCK) {
					viewer.showSearchLayer();
				}
			}
		});
	}

	// Override this to set the time before properties are updated.
	@Override
	protected final void processSense(KASense sense) {
		int time = sense.getTime();
		timer.setTime(time);
		if (NAME_THREADS) {
			Thread.currentThread().setName(me().toString() + " " + time);
		} else {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info(me().toString() + " is starting time step " + time);
			}
		}
		super.processSense(sense);
	}

	@Override
	protected final void think(int time, ChangeSet changed,
			Collection<Command> heard) {

		intervalTimer.start();

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Starting think method with changeset :" + changed
					+ " at time " + timer.getTime());
		}
		try {
			handleWorldUpdates(changed, heard);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("EXCEPTION OCCURED");
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Handling world updates took " + intervalTimer.lap()
					+ "ns");
		}

		// Now signal start of turn
		timer.fireTimeStepStarted();

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Timer listeners took " + intervalTimer.lap() + "ns");
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Calling abstract think method");
		}

		// initialse search;

		// System.out
		// .println(me()
		// + " knows about "
		// + getWorldModel().getEntitiesOfType(
		// StandardEntityURN.BLOCKADE));

		// Do shouting behaviour
		if (!myTypeIsCentre) {
			shoutGenerator.generateShouts();
		}

		if (USE_FALLBACK_BEHAVIOUR) {
			runThinkWithFallBack(time, changed);
		} else {
			try {
				think(time, changed);
			} catch (Exception e) {
				LOGGER.error("An exception occurred: " + e);
				e.printStackTrace();
				doDefaultSearch();
			}
		}

		// Main think method
		// think(time, changed);

		// at this point, the execution module has unprocessed commands and the
		// communication module has unsent messages

		// Only send messages at time step 3 and later
		if (timer.getTime() >= ignoreUntil) {
			communicationModule.flushOutbox();
		}

		executionService.flushCommands();

		long delay = intervalTimer.getTime();

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Think method done after " + delay / 1000000.0 + "ms");
		}

		if (delay > 300000000) {
			LOGGER.warn("Think method took " + delay / 1000000.0 + "ms!");
		}

		if (viewer != null) {
			viewer.timestepCompleted(getWorldModel());
		}

	}

	private void runThinkWithFallBack(int time, ChangeSet changed) {
		// Execution service for fallback behaviour
		Future thinkFuture = null;
		Future fallbackFuture = null;
		try {
			thinkFuture = ThinkExecution.submit(new ThinkRunner(time, changed));
			thinkFuture.get(thinkRuntime, TimeUnit.MILLISECONDS);
			thinkFuture.cancel(true);
		} catch (InterruptedException e) {
			LOGGER.error(" - INTERRUPTED EXCEPTION!");
			e.printStackTrace();
			try {
				thinkFuture.cancel(true);
				if (executionService.NotSubmittedCommand()) {
					fallbackFuture = ThinkExecution.submit(new FallbackRunner(
							time, changed));
					fallbackFuture.get(fallbackRuntime, TimeUnit.MILLISECONDS);
					fallbackFuture.cancel(true);
				}
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ExecutionException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (TimeoutException e3) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (ExecutionException e) {
			LOGGER.error(" - EXECUTION EXCEPTION!");
			LOGGER.error(e.toString());
			e.printStackTrace();
			try {
				thinkFuture.cancel(true);
				if (executionService.NotSubmittedCommand()) {
					fallbackFuture = ThinkExecution.submit(new FallbackRunner(
							time, changed));
					fallbackFuture.get(fallbackRuntime, TimeUnit.MILLISECONDS);
					fallbackFuture.cancel(true);
				}
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ExecutionException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (TimeoutException e3) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (TimeoutException e) {
			LOGGER.error(" - TIMEOUT EXCEPTION!");
			e.printStackTrace();
			try {
				thinkFuture.cancel(true);
				if (executionService.NotSubmittedCommand()) {
					fallbackFuture = ThinkExecution.submit(new FallbackRunner(
							time, changed));
					fallbackFuture.get(fallbackRuntime, TimeUnit.MILLISECONDS);
					fallbackFuture.cancel(true);
				}
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ExecutionException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (TimeoutException e3) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/**
	 * @param changed
	 * @param changed
	 * @param heard
	 * 
	 */
	private void handleWorldUpdates(ChangeSet changed, Collection<Command> heard) {

		IntervalTimer updateTimer = new IntervalTimer();
		updateTimer.start();

		// Get only the real differences between the changed ChangeSet and what
		// was in the world model at the beginning of the time step.
		ChangeSet sensedChanges = getWorldModel().getLastSensedChanges();
		// sensedChanges = changed;

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Newly sensed: " + sensedChanges);
		}

		// Now do negative updates
		try {
			ChangeSet negativeUpdates = negativeUpdater
					.removeUnseenEntities(changed);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Negative updates took " + updateTimer.lap()
						+ "ns");
				LOGGER.debug("Negative updates: " + negativeUpdates);
			}

			// Add these to the sensed changes
			mergeChangeSets(sensedChanges, negativeUpdates);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("EXCETION OCCURED");
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER
					.debug("Merging change sets took " + updateTimer.lap()
							+ "ns");
		}

		// Now infer passable roads
		try {
			ChangeSet passableUpdates = passableRoadsDetector
					.inferPassableRoads();

			if (LOGGER.isDebugEnabled()) {
				LOGGER
						.debug("Passable update took " + updateTimer.lap()
								+ "ns");
				LOGGER.debug("Passable updates: " + passableUpdates);
			}

			mergeChangeSets(sensedChanges, passableUpdates);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("EXCEPTION OCCURED");
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER
					.debug("Merging change sets took " + updateTimer.lap()
							+ "ns");
		}

		try {
			// Now infer blockage routing updates
			ChangeSet blockedUpdates = blockUpdater.inferUpdatedBlockades();

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Routing block update took " + updateTimer.lap()
						+ "ns");
				LOGGER.debug("Routing block updates: " + blockedUpdates);
			}

			mergeChangeSets(sensedChanges, blockedUpdates);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("EXCEPTION OCCURED");
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER
					.debug("Merging change sets took " + updateTimer.lap()
							+ "ns");
		}

		LOGGER.debug("Heard " + heard.size() + " messages.");

		// process all incoming messages
		// heard = filterOldMessages(heard);

		/*
		 * if (LOGGER.isDebugEnabled()) {
		 * LOGGER.debug("Filtering messages took " + updateTimer.lap() + "ns");
		 * }
		 */

		if (LOGGER.isTraceEnabled()) {
			for (Command c : heard) {
				LOGGER.trace("Heard: " + c.toString());
			}
		}

		communicationModule.hear(heard);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Decoding messages took " + updateTimer.lap() + "ns");
		}

		// Now check if I am a centre
		boolean amICentre = false;
		try {
			amICentre = communicationModule.amICentre();
		} catch (Exception e) {
			LOGGER.warn("Caught exception. This means I am not a centre.");
			e.printStackTrace();
		}

		if (amICentre != iAmCentre) {
			iAmCentre = amICentre;
			if (iAmCentre) {
				LOGGER.info("Detected centre role");
				worldModelCommsUpdater.setCentreRole(communicationModule
						.getChannelsToOwnTeam());
			} else {
				LOGGER.info("Detected no longer centre role");
				worldModelCommsUpdater.unsetCentreRole();
			}
		}

		// Update world model based on communicated messages
		List<Message> updates = worldModelCommsUpdater.update();

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Merging update messages with world model took "
					+ updateTimer.lap() + "ns");
		}

		// Now forward messages if necessary
		if (iAmCentre) {
			for (Message message : updates) {
				communicationModule.enqueueRadioMessageToOwnTeam(message);
			}
			if (updates.size() == 0) {
				// Make sure to send a ping every time step if nothing else is
				// sent
				communicationModule
						.enqueueRadioMessageToOwnTeam(new PingMessage());
			}
		}

		// Make sure all position information is consistent (always includes x,y
		// and position updates if any change)
		ensurePositionConsistency(sensedChanges);

		// Remove updates that would be propagated by others
		filterOutRedundantUpdates(sensedChanges, changed);

		// Send sensed entities to other agents (using only changed properties)
		propagator.sendUpdates(sensedChanges, me().getID());

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Sending update messages took " + updateTimer.lap()
					+ "ns");
		}

		// Finally remove old blocks
		if (blockForgetter != null && FORGET_BLOCKADES_AFTER > 0) {
			blockForgetter.forgetOldBlockades();
		}

		// ensureBlockadeConsistency();
	}

	// private void ensureBlockadeConsistency() {

	// }

	/**
	 * @param sensedChanges
	 * @param changed
	 */
	private void filterOutRedundantUpdates(ChangeSet sensedChanges,
			ChangeSet changed) {
		Set<EntityID> changedEntities = changed.getChangedEntities();
		Set<StandardEntity> otherAgents = new FastSet<StandardEntity>();
		for (EntityID id : changedEntities) {
			StandardEntity entity = getWorldModel().getEntity(id);
			// if (entity instanceof Human && !(entity instanceof Civilian) &&
			// entity) {

			// }
		}

	}

	/**
	 * @param sensedChanges
	 */
	private void ensurePositionConsistency(ChangeSet sensedChanges) {
		Set<EntityID> changedEntities = sensedChanges.getChangedEntities();
		for (EntityID changed : changedEntities) {
			StandardEntity entity = getWorldModel().getEntity(changed);
			if (entity instanceof Human) {
				Set<Property> changedProperties = sensedChanges
						.getChangedProperties(entity.getID());
				Set<String> included = new FastSet<String>();
				for (Property p : changedProperties) {
					if (p.getURN().equals(StandardPropertyURN.X.toString())
							|| p.getURN().equals(
									StandardPropertyURN.Y.toString())
							|| p.getURN().equals(
									StandardPropertyURN.POSITION.toString())) {
						included.add(p.getURN());
					}
				}
				if (included.size() > 0 && included.size() < 3) {
					// Not all were included.
					if (!included.contains(StandardPropertyURN.X.toString())) {
						sensedChanges.addChange(entity, entity
								.getProperty(StandardPropertyURN.X.toString()));
					}
					if (!included.contains(StandardPropertyURN.Y.toString())) {
						sensedChanges.addChange(entity, entity
								.getProperty(StandardPropertyURN.Y.toString()));
					}
					if (!included.contains(StandardPropertyURN.POSITION
							.toString())) {
						sensedChanges.addChange(entity, entity
								.getProperty(StandardPropertyURN.POSITION
										.toString()));
					}
				}
			}
		}
	}

	/**
	 * 
	 * @return The simulation timer.
	 */
	protected ISimulationTimer getTimer() {
		return timer;
	}

	/**
	 * This removes out-of-date messages. Used as a workaround for current bug
	 * with server that sends old messages.
	 */
	private Collection<Command> filterOldMessages(Collection<Command> heard) {
		Collection<Command> newHeard = new FastList<Command>();
		for (Command command : heard) {
			if (command.getTime() >= timer.getTime() - 6) {
				newHeard.add(command);
			} else {
				LOGGER.trace("Ignoring old command: " + command);
			}
		}
		return newHeard;
	}

	/**
	 * Merges negative updates into sensed changes. At the end of the method,
	 * sensedChanges will contain all changes.
	 * 
	 * @param sensedChanges
	 *            The original changes
	 * @param negativeUpdates
	 *            The negative updates to merge into the original changes.
	 */
	private void mergeChangeSets(ChangeSet sensedChanges,
			ChangeSet negativeUpdates) {
		Set<EntityID> changedEntities = negativeUpdates.getChangedEntities();
		for (EntityID entityID : changedEntities) {
			String entityURN = negativeUpdates.getEntityURN(entityID);
			Set<Property> changedProperties = negativeUpdates
					.getChangedProperties(entityID);
			for (Property property : changedProperties) {
				sensedChanges.addChange(entityID, entityURN, property);
			}
		}
	}

	/**
	 * Main fallback method that is called every time step if the think fails.
	 * Logic for the agent goes here.
	 * 
	 * @param time
	 *            The current time step.
	 * @param changed
	 *            Everything that has been observed this time step.
	 */
	protected abstract void fallback(int time, ChangeSet changed);

	/**
	 * Main think method that is called every time step. Logic for the agent
	 * goes here.
	 * 
	 * @param time
	 *            The current time step.
	 * @param changed
	 *            Everything that has been observed this time step.
	 */
	protected abstract void think(int time, ChangeSet changed);

	@Override
	protected void postConnect() {
		Thread.currentThread().setName(me().toString());

		LOGGER.info("Starting postconnect");

		/*
		 * Registry.getCurrentRegistry().registerEntityFactory(
		 * StandardEntityURN.BLOCKADE.toString(), new
		 * RoutingInfoBlockadeEntityFactory());
		 * 
		 * Registry.getCurrentRegistry().registerPropertyFactory(
		 * RoutingInfoBlockade.BLOCK_INFO_URN, new
		 * BlockedEdgesPropertyFactory());
		 */

		try {

			super.postConnect();

			getWorldModel().buildShortIndex();

			// Only create speed info object if I am human
			if (getWorldModel().getEntity(getID()) instanceof Human) {
				speedInfo = new SpeedLearningModule(getWorldModel(), getID(),
						getTimer());
			}

			executionService = new RC2ExecutionService(getID(), connection,
					config, timer, getWorldModel(), speedInfo);

			routingCostFunction = new SimpleDistanceRoutingCostFunction(
					getWorldModel(), false);

			routingCostFunction.setBurningPenalty(5000000);

			if (USE_BLOCK_DETECTION) {
				routing = getReusableRouting();// new
				// BlockDetectingRoutingModule(getWorldModel(),
				// routingCostFunction, timer, executionService, getID(),
				// speedInfo, true);
			} else {
				routing = new BidirectionalDijkstrasRoutingModule(
						getWorldModel(), routingCostFunction, timer);
			}

			ICommunicationBeliefBaseAdapter communicationBeliefBaseAdapter = new CommunicationBeliefBaseAdapter(
					getWorldModel(), config, routing.getConverter());

			getWorldModel().setDefaultConverter(routing.getConverter());

			ISimulationCommunicationConfiguration configuration = new SimulationCommunicationConfigurationAdapter(
					config, getID(), getWorldModel().getEntity(getID())
							.getStandardURN(), getWorldModel());

			communicationModule = new IAMCommunicationModule(getID(), timer,
					communicationBeliefBaseAdapter, configuration, connection);

			worldModelCommsUpdater = new WorldModelCommsUpdater(
					getWorldModel(), communicationModule, timer, config,
					getID());

			if (!getWorldModel().getEntitiesOfType(
					StandardEntityURN.AMBULANCE_CENTRE,
					StandardEntityURN.FIRE_STATION,
					StandardEntityURN.POLICE_OFFICE).contains(me())) {
				myTypeIsCentre = false;
				this.shoutGenerator = new ShoutGenerator(getWorldModel(),
						getCommunicationModule());
			} else {
				myTypeIsCentre = true;
			}

			/*
			 * executionService = new RC2ExecutionService(getID(), connection,
			 * config, timer, getWorldModel());
			 */

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Starting to build routing.");
			}

			propagator = new WorldModelUpdatePropagator(getWorldModel(),
					communicationModule, timer, getRoutingModule()
							.getConverter());

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Starting to build spatial index.");
			}

			// spatialIndex = new SpatialIndex(getWorldModel());

			negativeUpdater = new NegativeSenseUpdater(config, getWorldModel(),
					getID(), timer, new LatestProperty());

			passableRoadsDetector = new PassableRoadsDetector(getWorldModel(),
					getID());

			if ((me().getURN().equals(
					StandardEntityURN.AMBULANCE_TEAM.toString()) || me()
					.getURN().equals(StandardEntityURN.FIRE_BRIGADE.toString()))
					&& FORGET_BLOCKADES_AFTER > 0) {
				this.blockForgetter = new BlockForgettingModule(
						getWorldModel(), new LatestProperty(), timer,
						FORGET_BLOCKADES_AFTER);
			}

			getWorldModel().postConnect();

			searchBehaviour = new ClashRandomisedClosestSearchBehaviour(
					getWorldModel(), getID(), executionService,
					getRoutingModule(), config, getCommunicationModule());

			blockUpdater = new RoutingInfoUpdater(getWorldModel(),
					getRoutingModule().getConverter(), getTimer(),
					new LatestProperty());

			// make execution service
			ThinkExecution = Executors.newFixedThreadPool(1);
			double temp = config.getFloatValue("kernel.agents.think-time") * 0.8;
			thinkRuntime = (int) temp;
			temp = config.getFloatValue("kernel.agents.think-time") * 0.2;
			fallbackRuntime = (int) temp;

			this.ignoreUntil = config.getIntValue(IGNORE_KEY, 3);

			getWorldModel().getStuckMemory().enableAutoUpdate(getWorldModel());

			if (!getWorldModel().initialisedSearch()) {
				getWorldModel().initialiseSearch(getID());
			}

			LOGGER.info("Finished postconnect");
		} catch (Exception e) {
			LOGGER.error("Exception during postconnect! " + e);
			e.printStackTrace();
		}
	}

	public static void stopIfInterrupted() {
		if (Thread.currentThread().isInterrupted()) {
			throw new RuntimeException();
		}
	}

	/**
	 * Causes the agent to follow the default search behaviour. This will
	 * automatically submit a command, so the think method can return after
	 * calling this.
	 */
	public void doDefaultSearch() {
		searchBehaviour.doDefaultSearch();
	}

	/**
	 * Returns the agent's default routing module.
	 * 
	 * @return The agent's routing module.
	 */
	public IRoutingModule getRoutingModule() {
		return getReusableRouting();
		// return routing;
	}

	/**
	 * Returns the communication module. Use this to send messages.
	 * 
	 * @return The communication module.
	 */
	public IAMCommunicationModule getCommunicationModule() {
		return communicationModule;
	}

	/**
	 * Returns execution service. Use this to submit commands to the server.
	 * 
	 * @return The execution service.
	 */
	public IExecutionService getExecutionService() {
		return executionService;
	}

	@Override
	protected final IAMWorldModel createWorldModel() {
		IAMWorldModel wm = new IAMWorldModel(timer, config);
		return wm;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * rescuecore2.components.AbstractComponent#getPreferredRegistry(rescuecore2
	 * .registry.Registry)
	 */
	@Override
	public Registry getPreferredRegistry(Registry parent) {
		Registry registry = new Registry(parent);

		registry.registerEntityFactory(StandardEntityURN.BLOCKADE.toString(),
				RoutingInfoBlockadeEntityFactory.INSTANCE);

		registry.registerPropertyFactory(RoutingInfoBlockade.BLOCK_INFO_URN,
				BlockedEdgesPropertyFactory.INSTANCE);

		registry.registerEntityFactory(StandardEntityURN.ROAD.toString(),
				BlockInfoRoadEntityFactory.INSTANCE);

		registry
				.registerPropertyFactory(KnownToBePassablePropertyFactory.INSTANCE);

		return registry;
	}

	/**
	 * Returns the agent's world model.
	 * 
	 * @return THe world model
	 */
	protected IAMWorldModel getWorldModel() {
		return model;
	}

	public ISpeedInfo getSpeedInfo() {
		return speedInfo;
	}

	@Override
	public final String[] getRequestedEntityURNs() {
		List<StandardEntityURN> agentTypes = getAgentTypes();

		String[] result = new String[agentTypes.size()];

		for (int i = 0; i < result.length; i++) {
			result[i] = agentTypes.get(i).toString();
		}

		return result;
	}

	/**
	 * Returns the spatial index used by this agent (creates one if necessary).
	 * 
	 * @return The spatial index.
	 */
	/*
	 * public ISpatialIndex getSpatialIndex() { if (spatialIndex == null) {
	 * spatialIndex = new SpatialIndex(getWorldModel()); } return spatialIndex;
	 * }
	 */

	/**
	 * Subclasses need to define which agents they want to control
	 * 
	 * @return
	 */
	protected abstract List<StandardEntityURN> getAgentTypes();

	protected void addUpdateHandler(IMessageHandler handler) {
		worldModelCommsUpdater.addUpdateHandler(handler);
	}

	/**
	 * Plans a path from the agent's current position to another entity
	 * (building, road, agent...).
	 * 
	 * @param id
	 *            The target ID.
	 * @return The shortest path or an invalid path if none could be found
	 *         (e.g., if blocked).
	 */
	protected IPath planPath(EntityID id) {
		return getRoutingModule().findShortestPath(getID(), id);
	}

	/**
	 * Plans a path from the agent's current position to the closest of a set of
	 * other entities (buildings, roads, agents...).
	 * 
	 * @param ids
	 *            The target IDs.
	 * @return The shortest path to the closest entity or an invalid path if
	 *         none could be found (e.g., if all are blocked).
	 */
	protected IPath planPath(Collection<EntityID> ids) {
		return getRoutingModule().findShortestPath(getID(), ids);
	}

	/**
	 * @return the reusableRouting
	 */
	public IRoutingModule getReusableRouting() {
		if (reusableRouting == null) {
			if (USE_BLOCK_DETECTION) {
				reusableRouting = new BlockDetectingRoutingModule(
						getWorldModel(), routingCostFunction, timer,
						executionService, getID(), speedInfo, false);
			} else {
				reusableRouting = new SimpleDijkstrasRoutingModule(
						getWorldModel(), routingCostFunction, timer);
			}
		}
		return reusableRouting;
	}

	private class ThinkRunner implements Runnable {

		private ChangeSet changeSet;
		private int time;

		public ThinkRunner(int time2, ChangeSet changed) {
			time = time2;
			changeSet = changed;
		}

		@Override
		public void run() {
			Thread.currentThread().setName(me().toString() + " " + time);
			think(time, changeSet);
		}

	}

	private class FallbackRunner implements Runnable {

		private ChangeSet changeSet;
		private int time;

		public FallbackRunner(int time2, ChangeSet changed) {
			time = time2;
			changeSet = changed;
		}

		@Override
		public void run() {
			Thread.currentThread().setName(me().toString() + " " + time);
			fallback(time, changeSet);
		}

	}
}
