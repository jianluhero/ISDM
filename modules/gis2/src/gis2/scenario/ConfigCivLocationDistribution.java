package gis2.scenario;

import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.StringTokenizer;

import rescuecore2.log.Logger;

import maps.gml.GMLCoordinates;
import maps.gml.GMLShape;


public class ConfigCivLocationDistribution extends AbstractTool {

	private static final String file = "destination";
	private static final int X = 10;
	private static final int Y = 10;
	private RegionOverlay regionOverlay;
	 private Listener listener;
	
	private int xLength;
	private int yLength;

	private double locationDis[][];
	
	private double count[][];

	protected ConfigCivLocationDistribution(ScenarioEditor editor) {
		super(editor);
		listener = new Listener();
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getName() {
		return "Config Des. Dis.";
	}

	 public void activate() {
		 editor.getViewer().addMouseListener(listener);
	        editor.getViewer().addMouseMotionListener(listener);
	        editor.getViewer().removeOverlay(editor.getAgentOverlay());
	        editor.getViewer().removeOverlay(editor.getFireOverlay());
	        editor.getViewer().removeOverlay(editor.getCentreOverlay());
	        init();
	    }
	 
	 public void deactivate() {
		  editor.getViewer().addOverlay(editor.getAgentOverlay());
	        editor.getViewer().addOverlay(editor.getFireOverlay());
	        editor.getViewer().addOverlay(editor.getCentreOverlay());
	        editor.getViewer().removeOverlay(regionOverlay);	
	        editor.getViewer().removeMouseListener(listener);
	        editor.getViewer().removeMouseMotionListener(listener);
	        editor.getViewer().clearAllBuildingDecorators();
	        editor.getViewer().clearAllRoadDecorators();
	        editor.getViewer().clearAllSpaceDecorators();
	        editor.getViewer().repaint();
	    }

	
	public void init()
	{
		load();
		if(locationDis.length==0){//no distribution exist, create default one
			xLength=X;
			yLength=Y;
			locationDis = new double[xLength][yLength];
		}		
		regionOverlay=new RegionOverlay(editor,xLength,yLength,locationDis);
		editor.getViewer().addOverlay(regionOverlay);
		editor.updateOverlays();
		editor.setOperation(getName());
		count=new double[xLength][yLength];
	}
	/**
	 * load destination distribution from file
	 */
	public void load() {
		File f = editor.getBaseDir();
		File location = new File(f, file);

		try {
			BufferedReader reader = new BufferedReader(new FileReader(location));
			String l = reader.readLine();
			if (l != null) {
				StringTokenizer st = new StringTokenizer(l, ",");
				xLength = Integer.valueOf(st.nextToken());
				yLength = Integer.valueOf(st.nextToken());
				locationDis = new double[xLength][yLength];
				String dis = reader.readLine();
				if (dis != null) {
					StringTokenizer st1 = new StringTokenizer(dis, ",");
					for (int i = 0; i < xLength; i++) {
						for (int j = 0; j < yLength; j++) {
							locationDis[i][j] = Double.valueOf(st1.nextToken());
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void processClick(MouseEvent e) {
		locationDis=new double[xLength][yLength];
		regionOverlay.setDis(locationDis);
		editor.repaint();
		if(e.getButton()==1)//increase probability
		{
			
		}
		else if(e.getButton()==3)//decrease probability
		{
			
		}		
	}
	
	private class Listener implements MouseListener, MouseMotionListener {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                processClick(e);                
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            Point p = fixEventPoint(e.getPoint());
            GMLCoordinates c = editor.getViewer().getCoordinatesAtPoint(p.x, p.y);
            //e.getX and e.getY return the x, y coordinate in the source component of the screen
            //p.x and p.y is the same as e.getX and e.getY since the border is 0
            //c.getX and c.getY return the coordinate in the gml map
        }

        @Override
        public void mousePressed(MouseEvent e) {        	
        }
        @Override
        public void mouseReleased(MouseEvent e) {
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
