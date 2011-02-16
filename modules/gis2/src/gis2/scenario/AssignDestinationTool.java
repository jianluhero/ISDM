package gis2.scenario;

import gis2.Destination;

import java.awt.Color;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Iterator;

import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;

import maps.gml.GMLBuilding;
import maps.gml.GMLCoordinates;
import maps.gml.GMLRoad;
import maps.gml.GMLShape;
import maps.gml.GMLSpace;
import maps.gml.view.FilledShapeDecorator;

public class AssignDestinationTool extends ShapeTool {

	private static final Color SELECTED_COLOUR = new Color(255, 0, 0, 128);

	private Listener listener;

	protected FilledShapeDecorator selected;

	private GMLShape highlightShape;

	private double minX;
	private double maxX;
	private double minY;
	private double maxY;

	ArrayList<EntityID> selectedShapes;

	protected AssignDestinationTool(ScenarioEditor editor) {
		super(editor);
		listener = new Listener();
		selected = new FilledShapeDecorator(SELECTED_COLOUR, SELECTED_COLOUR,
				SELECTED_COLOUR);
		selectedShapes = new ArrayList<EntityID>();
	}

	@Override
	public String getName() {
		return "Assign Destination for Civilians";
	}

	@Override
	protected void processClick(GMLShape shape) {
		ArrayList<Destination> destinations = editor.getScenario()
				.getDestination();
		for (int i = 0; i < selectedShapes.size(); i++) {
			EntityID id = selectedShapes.get(i);
			for (int j = 0; j < destinations.size(); j++) {
				Destination destination = destinations.get(j);
				if (destination.getStart() == id.getValue()) {
					ArrayList<Integer> ends = new ArrayList<Integer>();
					ends.add(shape.getID());
					destination.setEnds(ends);
				}
			}
		}
		editor.setChanged();
		editor.updateOverlays();
		editor.getViewer().clearAllBuildingDecorators();
		editor.getViewer().clearAllRoadDecorators();
		editor.getViewer().clearAllSpaceDecorators();
		selectedShapes.clear();
		// editor.addEdit(new AssignDestinationEdit(shape.getID()));
	}

	@Override
	public void activate() {
		editor.getViewer().addMouseListener(listener);
		editor.getViewer().addMouseMotionListener(listener);
		highlightShape = null;
	}

	@Override
	public void deactivate() {
		editor.getViewer().removeMouseListener(listener);
		editor.getViewer().removeMouseMotionListener(listener);
		editor.getViewer().clearAllBuildingDecorators();
		editor.getViewer().clearAllRoadDecorators();
		editor.getViewer().clearAllSpaceDecorators();
		editor.getViewer().repaint();

		selectedShapes.clear();
	}

	/**
	 * get gml shape ids, which droped in the range of minX, maxX, minY and maxY
	 * 
	 * @return
	 */
	protected void selectedShapes() {
		Logger.debug("select shapes: ");
		Iterator<GMLShape> shapes = editor.getMap().getAllShapes().iterator();
		while (shapes.hasNext()) {
			GMLShape shape = shapes.next();
			if (shape.getCentreX() >= minX && shape.getCentreX() <= maxX
					&& shape.getCentreY() >= minY && shape.getCentreY() <= maxY) {
				selectedShapes.add(new EntityID(shape.getID()));
				Logger.debug("shape " + shape.getID() + " is selected");
				if (shape instanceof GMLBuilding) {
					editor.getViewer().setBuildingDecorator(selected,
							(GMLBuilding) shape);
				}
				if (shape instanceof GMLRoad) {
					editor.getViewer().setRoadDecorator(selected,
							(GMLRoad) shape);
				}
				if (shape instanceof GMLSpace) {
					editor.getViewer().setSpaceDecorator(selected,
							(GMLSpace) shape);
				}
			}
			editor.getViewer().repaint();
		}
	}

	private class Listener implements MouseListener, MouseMotionListener {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (highlightShape != null && e.getButton() == MouseEvent.BUTTON1) {
				processClick(highlightShape);
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			Point p = fixEventPoint(e.getPoint());
			GMLCoordinates c = editor.getViewer().getCoordinatesAtPoint(p.x,
					p.y);

			GMLShape shape = editor.getMap().findShapeUnder(c.getX(), c.getY());

			if (selectedShapes.size() > 0) {
				if (shape != null && isShapeinSelectedShapes(shape)) {
					if (highlightShape == shape) {
						return;
					}
					if (highlightShape != null) {
						if (isShapeinSelectedShapes(highlightShape)) {
							if (highlightShape instanceof GMLBuilding) {
								editor.getViewer().setBuildingDecorator(
										selected, (GMLBuilding) highlightShape);
							}
							if (highlightShape instanceof GMLRoad) {
								editor.getViewer().setRoadDecorator(selected,
										(GMLRoad) highlightShape);
							}
							if (highlightShape instanceof GMLSpace) {
								editor.getViewer().setSpaceDecorator(selected,
										(GMLSpace) highlightShape);
							}
						} else {
							if (highlightShape instanceof GMLBuilding) {
								editor.getViewer().clearBuildingDecorator(
										(GMLBuilding) highlightShape);
							}
							if (highlightShape instanceof GMLRoad) {
								editor.getViewer().clearRoadDecorator(
										(GMLRoad) highlightShape);
							}
							if (highlightShape instanceof GMLSpace) {
								editor.getViewer().clearSpaceDecorator(
										(GMLSpace) highlightShape);
							}
						}
					}
					highlightShape = shape;
					if (highlightShape != null) {
						if (highlightShape instanceof GMLBuilding) {
							editor.getViewer().setBuildingDecorator(highlight,
									(GMLBuilding) highlightShape);
						}
						if (highlightShape instanceof GMLRoad) {
							editor.getViewer().setRoadDecorator(highlight,
									(GMLRoad) highlightShape);
						}
						if (highlightShape instanceof GMLSpace) {
							editor.getViewer().setSpaceDecorator(highlight,
									(GMLSpace) highlightShape);
						}
					}
					editor.getViewer().repaint();

				} else {
					highlight(shape);
				}
			} else {
				highlight(shape);
			}
			// if(selectedShapes)
			// e.getX and e.getY return the x, y coordinate in the source
			// component of the screen
			// p.x and p.y is the same as e.getX and e.getY since the border is
			// 0
			// c.getX and c.getY return the coordinate in the gml map
		}

		public boolean isShapeinSelectedShapes(GMLShape shape) {
			for (int i = 0; i < selectedShapes.size(); i++) {
				if (selectedShapes.get(i).getValue() == shape.getID())
					return true;
			}
			return false;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			Point p = fixEventPoint(e.getPoint());
			GMLCoordinates c = editor.getViewer().getCoordinatesAtPoint(p.x,
					p.y);
			minX = c.getX();
			minY = c.getY();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			Point p = fixEventPoint(e.getPoint());
			GMLCoordinates c = editor.getViewer().getCoordinatesAtPoint(p.x,
					p.y);
			maxX = c.getX();
			maxY = c.getY();
			// sort
			double temp;
			if (maxX < minX) {
				temp = maxX;
				maxX = minX;
				minX = temp;
			}
			if (maxY < minY) {
				temp = maxY;
				maxY = minY;
				minY = temp;
			}
			if (Math.abs(maxX - minX) < 0.00001
					|| Math.abs(maxY - minY) < 0.00001)
				Logger.debug("a mouse click, not a drag");
			else {
				Logger.debug("the range: " + minX + " " + maxY + " " + maxX
						+ " " + minY + " is selected");
				selectedShapes();
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

		private Point fixEventPoint(Point p) {
			Insets insets = editor.getViewer().getInsets();
			return new Point(p.x - insets.left, p.y - insets.top);
		}
	}

	@Override
	protected boolean shouldHighlight(GMLShape shape) {
		// TODO Auto-generated method stub
		return true;
	}

	/*
	 * private class AssignDestinationEdit extends AbstractUndoableEdit {
	 * private int id;
	 * 
	 * public AssignDestinationEdit(int id) { this.id = id; }
	 * 
	 * @Override public void undo() { super.undo();
	 * editor.getScenario().removeCivilian(id); editor.updateOverlays(); }
	 * 
	 * @Override public void redo() { super.redo();
	 * editor.getScenario().addCivilian(id); editor.updateOverlays(); } }
	 */
}
