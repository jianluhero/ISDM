package iamrescue.belief.gui;

import iamrescue.agent.firebrigade.FastFirePredictor;
import iamrescue.agent.firebrigade.FirePredictor;
import iamrescue.belief.IAMWorldModel;
import iamrescue.routing.IRoutingModule;
import iamrescue.routing.gui.BuildingImportanceLayer;
import iamrescue.routing.gui.RoutingGraphLayer;
import iamrescue.routing.gui.SearchLayer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import rescuecore2.GUIComponent;
import rescuecore2.config.Config;
import rescuecore2.view.EntityInspector;
import rescuecore2.view.RenderedObject;
import rescuecore2.view.ViewComponent;
import rescuecore2.view.ViewListener;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.WorldModel;

/**
 * A KernelGUIComponent that will view a standard world model.
 */
public class IAMWorldModelViewerComponent implements GUIComponent {
	private static final int SIZE = 500;

	private IAMWorldModelViewer viewer;
	private EntityInspector inspector;
	private JTextField field;
	private JComponent view;
	private WorldModel<? extends Entity> world;

	private RoutingGraphLayer routingGraphLayer;

	private SearchLayer searchLayer;

	private FirePredictor firePredictor;

	private BuildingImportanceLayer fireLayer;

	/**
	 * Construct a StandardWorldModelViewerComponent.
	 */
	public IAMWorldModelViewerComponent() {
		viewer = new IAMWorldModelViewer();
		inspector = new EntityInspector();
		field = new JTextField();
		viewer.setPreferredSize(new Dimension(SIZE, SIZE));
		viewer.addViewListener(new ViewListener() {
			@Override
			public void objectsClicked(ViewComponent v,
					List<RenderedObject> objects) {
				for (RenderedObject next : objects) {
					if (next.getObject() instanceof Entity) {
						inspector.inspect((Entity) next.getObject());
						field.setText("");
						return;
					}
				}
			}

			@Override
			public void objectsRollover(ViewComponent v,
					List<RenderedObject> objects) {
			}
		});
		field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				String s = field.getText();
				try {
					int id = Integer.parseInt(s);
					EntityID eid = new EntityID(id);
					Entity e = world.getEntity(eid);
					inspector.inspect(e);
				} catch (NumberFormatException e) {
					field.setText("");
				}
			}
		});
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, viewer,
				new JScrollPane(inspector));
		view = new JPanel(new BorderLayout());
		view.add(split, BorderLayout.CENTER);
		view.add(field, BorderLayout.NORTH);

	}

	public void showRoutingLayer() {
		routingGraphLayer.setVisible(true);
	}

	public void showFireLayer(FastFirePredictor firePredictor) {
		if (fireLayer == null) {
			fireLayer = new BuildingImportanceLayer(firePredictor);
			viewer.addLayer(fireLayer);
			fireLayer.setVisible(true);
			viewer.repaint();
		}
	}

	public void simulationStarted(Config config, IAMWorldModel worldModel,
			IRoutingModule routing) {
		viewer.initialise(config);
		world = worldModel;
		routingGraphLayer = new RoutingGraphLayer(routing);
		viewer.addLayer(routingGraphLayer);
		searchLayer = new SearchLayer((IAMWorldModel) world);
		viewer.addLayer(searchLayer);
		routingGraphLayer.setVisible(false);
		searchLayer.setVisible(false);
		viewer.view(world);
		viewer.repaint();
	}

	public void timestepCompleted(IAMWorldModel worldModel) {
		viewer.view(worldModel);
		viewer.repaint();
	}

	@Override
	public JComponent getGUIComponent() {
		return view;
	}

	@Override
	public String getGUIComponentName() {
		return "World view";
	}

	/**
	 * 
	 */
	public void showSearchLayer() {
		searchLayer.setVisible(true);
	}
}
