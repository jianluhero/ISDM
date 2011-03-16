package gis2.scenario;

import java.awt.Color;
import java.awt.Graphics;
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

/**
 * by drag, select specific shapes.
 * @author Bing Shi
 *
 */
public abstract class SelectionTool extends ShapeTool {

	private static final Color SELECTED_REGION_COLOUR = new Color(255, 0, 0, 4);
	private static final Color SELECTED_COLOUR = new Color(255, 0, 0, 128);
	protected ArrayList<EntityID> selectedShapes;
	protected FilledShapeDecorator selected;

	private Listener listener;

	private GMLShape highlightShape;

	private double minX;
	private double maxX;
	private double minY;
	private double maxY;

	private int startX;
	private int startY;

	public SelectionTool(ScenarioEditor editor) {
		super(editor);
		listener = new Listener();
		selectedShapes = new ArrayList<EntityID>();
		selected = new FilledShapeDecorator(SELECTED_COLOUR, SELECTED_COLOUR,
				SELECTED_COLOUR);
		// TODO Auto-generated constructor stub
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

	protected abstract void processMouseRealsed();

	/**
	 * get gml shape ids, which droped in the range of minX, maxX, minY and maxY
	 * 
	 * @return
	 */
	protected void selectedShapes() {
		selectedShapes.clear();
		editor.getViewer().clearAllBuildingDecorators();
		editor.getViewer().clearAllRoadDecorators();
		editor.getViewer().clearAllSpaceDecorators();

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
				if (highlightShape == shape) {
					return;
				}
				if (highlightShape != null) {
					if (isShapeinSelectedShapes(highlightShape)) {
						if (highlightShape instanceof GMLBuilding) {
							editor.getViewer().setBuildingDecorator(selected,
									(GMLBuilding) highlightShape);
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

			startX = p.x;
			startY = p.y;
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
			boolean isClick = true;
			if (Math.abs(maxX - minX) < 0.00001
					|| Math.abs(maxY - minY) < 0.00001) {
				Logger.debug("a mouse click, not a drag");
				isClick = true;
			} else {
				Logger.debug("the range: " + minX + " " + maxY + " " + maxX
						+ " " + minY + " is selected");
				selectedShapes();
				isClick = false;
			}
			editor.setOperation(getName() + ": drag a list of shapes");
			editor.updateOverlays();
			if (!isClick)
				processMouseRealsed();
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			int currentX = e.getPoint().x;
			int currentY = e.getPoint().y;
			Graphics graphics = editor.getViewer().getGraphics().create();
			graphics.setColor(SELECTED_REGION_COLOUR);
			graphics.fillRect(startX, startY, currentX - startX, currentY
					- startY);
			// editor.getViewer().repaint();
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

	protected void highlight(GMLShape newShape) {
		if (!shouldHighlight(newShape)) {
			return;
		}
		if (highlightShape == newShape) {
			return;
		}
		if (highlightShape != null) {
			if (highlightShape instanceof GMLBuilding) {
				editor.getViewer().clearBuildingDecorator(
						(GMLBuilding) highlightShape);
			}
			if (highlightShape instanceof GMLRoad) {
				editor.getViewer().clearRoadDecorator((GMLRoad) highlightShape);
			}
			if (highlightShape instanceof GMLSpace) {
				editor.getViewer().clearSpaceDecorator(
						(GMLSpace) highlightShape);
			}
		}
		highlightShape = newShape;
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
	}
}
