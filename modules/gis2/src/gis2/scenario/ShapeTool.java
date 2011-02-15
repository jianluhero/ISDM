package gis2.scenario;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.Color;
import java.awt.Point;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;

import maps.gml.view.FilledShapeDecorator;
import maps.gml.GMLRoad;
import maps.gml.GMLBuilding;
import maps.gml.GMLSpace;
import maps.gml.GMLShape;
import maps.gml.GMLCoordinates;

/**
   Abstract base class for tools that operate on GML shapes.
*/
public abstract class ShapeTool extends AbstractTool {
    private static final Color HIGHLIGHT_COLOUR = new Color(0, 0, 255, 128);
    private static final Color SELECTED_COLOUR = new Color(255, 0, 0, 128);

    private Listener listener;
    private FilledShapeDecorator highlight;
    
    protected FilledShapeDecorator selected;

    private GMLShape highlightShape;

    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    
    ArrayList<EntityID>selectedShapes;
    /**
       Construct a ShapeTool.
       @param editor The editor instance.
    */
    public ShapeTool(ScenarioEditor editor) {
        super(editor);
        listener = new Listener();
        highlight = new FilledShapeDecorator(HIGHLIGHT_COLOUR, HIGHLIGHT_COLOUR, HIGHLIGHT_COLOUR);
        
        selected = new FilledShapeDecorator(SELECTED_COLOUR, SELECTED_COLOUR, SELECTED_COLOUR);
        selectedShapes=new ArrayList<EntityID>();
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
    }

    /**
       Handle a click on a shape.
       @param shape The shape that was clicked.
    */
    protected abstract void processClick(GMLShape shape);

    /**
       Find out if a shape should be highlighted or not. Only highlighted shapes can be clicked.
       @param shape The shape to check.
       @return True if the shape should be highlighted, false otherwise.
    */
    protected abstract boolean shouldHighlight(GMLShape shape);

    private void highlight(GMLShape newShape) {
        if (!shouldHighlight(newShape)) {
            return;
        }
        if (highlightShape == newShape) {
            return;
        }
        if (highlightShape != null) {
            if (highlightShape instanceof GMLBuilding) {
                editor.getViewer().clearBuildingDecorator((GMLBuilding)highlightShape);
            }
            if (highlightShape instanceof GMLRoad) {
                editor.getViewer().clearRoadDecorator((GMLRoad)highlightShape);
            }
            if (highlightShape instanceof GMLSpace) {
                editor.getViewer().clearSpaceDecorator((GMLSpace)highlightShape);
            }
        }
        highlightShape = newShape;
        if (highlightShape != null) {
            if (highlightShape instanceof GMLBuilding) {
                editor.getViewer().setBuildingDecorator(highlight, (GMLBuilding)highlightShape);
            }
            if (highlightShape instanceof GMLRoad) {
                editor.getViewer().setRoadDecorator(highlight, (GMLRoad)highlightShape);
            }
            if (highlightShape instanceof GMLSpace) {
                editor.getViewer().setSpaceDecorator(highlight, (GMLSpace)highlightShape);
            }
        }
        editor.getViewer().repaint();
    }

    /**
     * get gml shape ids, which droped in the range of minX, maxX, minY and maxY
     * @return
     */
    protected void selectedShapes()
    {
    	Logger.debug("select shapes: ");
    	Iterator<GMLShape> shapes=editor.getMap().getAllShapes().iterator();
    	while(shapes.hasNext())
    	{
    		GMLShape shape=shapes.next();
    		if(shape.getCentreX()>=minX&&shape.getCentreX()<=maxX&&shape.getCentreY()>=minY&&shape.getCentreY()<=maxY)
    		{	
    			selectedShapes.add(new EntityID(shape.getID()));
    			Logger.debug("shape "+shape.getID()+" is selected");
    			if (shape instanceof GMLBuilding) {
                    editor.getViewer().setBuildingDecorator(selected, (GMLBuilding)shape);
                }
                if (shape instanceof GMLRoad) {
                    editor.getViewer().setRoadDecorator(selected, (GMLRoad)shape);
                }
                if (shape instanceof GMLSpace) {
                    editor.getViewer().setSpaceDecorator(selected, (GMLSpace)shape);
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
            GMLCoordinates c = editor.getViewer().getCoordinatesAtPoint(p.x, p.y);
            highlight(editor.getMap().findShapeUnder(c.getX(), c.getY()));
            
            //e.getX and e.getY return the x, y coordinate in the source component of the screen
            //p.x and p.y is the same as e.getX and e.getY since the border is 0
            //c.getX and c.getY return the coordinate in the gml map
        }
        
        public boolean isShapeinSelectedShapes(GMLShape shape)
        {
        	for(int i=0;i<selectedShapes.size();i++)
        	{
        		if(selectedShapes.get(i).getValue()==shape.getID())
        			return true;
        	}
        	return false;
        }

        @Override
        public void mousePressed(MouseEvent e) {
        	Point p = fixEventPoint(e.getPoint());
            GMLCoordinates c = editor.getViewer().getCoordinatesAtPoint(p.x, p.y);
            minX=c.getX();
            minY=c.getY();
        }
        @Override
        public void mouseReleased(MouseEvent e) {
        	Point p = fixEventPoint(e.getPoint());
            GMLCoordinates c = editor.getViewer().getCoordinatesAtPoint(p.x, p.y);
            maxX=c.getX();
            maxY=c.getY();
            //sort
            double temp;
            if(maxX<minX)
            {
            	temp=maxX;
            	maxX=minX;
            	minX=temp;
            }
            if(maxY<minY)
            {
            	temp=maxY;
            	maxY=minY;
            	minY=temp;
            }
            if(Math.abs(maxX-minX)<0.00001||Math.abs(maxY-minY)<0.00001)
            	Logger.debug("a mouse click, not a drag");
            else {
					 Logger.debug("the range: "+minX+" "+maxY+" "+maxX+" "+minY+" is selected");
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
}