package gis2.scenario;

import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.StringTokenizer;

import rescuecore2.log.Logger;

import maps.gml.GMLCoordinates;
import maps.gml.GMLShape;

/**
 * divide into several regions
 * edit probability for each region
 * @author Bing Shi
 *
 */
public abstract class RegionTool extends AbstractTool {
	protected static final int X = 10;
	protected static final int Y = 10;
	
	protected String file;
	protected int xLength;
	protected int yLength;
	private Listener listener;
	
	protected RegionOverlay regionOverlay;
	protected double dis[][];
	
	protected double count;

	protected RegionTool(ScenarioEditor editor, String file) {
		super(editor);
		this.file=file;
		listener = new Listener();
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void activate() {
		editor.getViewer().addMouseListener(listener);
		editor.getViewer().addMouseMotionListener(listener);
		
		editor.getViewer().removeOverlay(editor.getAgentOverlay());
		editor.getViewer().removeOverlay(editor.getFireOverlay());
		editor.getViewer().removeOverlay(editor.getCentreOverlay());
	}

	@Override
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
	
	public void save() {
		File f = editor.getBaseDir();
		File location = new File(f, file);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(location));
			writer.write(xLength + "," + yLength);
			writer.newLine();
			for (int i = 0; i < xLength; i++) {
				for (int j = 0; j < yLength; j++)
					writer.write(dis[i][j] + ",");
			}
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected abstract void processClick(MouseEvent e);

	private class Listener implements MouseListener, MouseMotionListener {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1
					|| e.getButton() == MouseEvent.BUTTON3) {
				processClick(e);
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			Point p = fixEventPoint(e.getPoint());
			GMLCoordinates c = editor.getViewer().getCoordinatesAtPoint(p.x,
					p.y);
			// e.getX and e.getY return the x, y coordinate in the source
			// component of the screen
			// p.x and p.y is the same as e.getX and e.getY since the border is
			// 0
			// c.getX and c.getY return the coordinate in the gml map
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
	}

	protected int[] getRegionIndex(Point ep) {
		Point p = fixEventPoint(ep);
		GMLCoordinates c = editor.getViewer().getCoordinatesAtPoint(p.x, p.y);
		double x = c.getX();
		double y = c.getY();
		double maxX = editor.getMap().getMaxX();
		double maxY = editor.getMap().getMaxY();
		double minX = editor.getMap().getMinX();
		double minY = editor.getMap().getMinY();
		double xSize = (maxX - minX) / xLength;
		double ySize = (maxY - minY) / yLength;
		int result[] = new int[2];
		for (int i = 0; i < xLength; i++) {
			for (int j = 0; j < yLength; j++) {
				if (minX + i * xSize < x && x < minX + (i + 1) * xSize
						&& minY + j * ySize < y && y < minY + (j + 1) * ySize) {
					result[0] = i;
					result[1] = j;
					return result;
				}
			}
		}
		result[0] = -1;
		result[1] = -1;
		return result;
	}

	private Point fixEventPoint(Point p) {
		Insets insets = editor.getViewer().getInsets();
		return new Point(p.x - insets.left, p.y - insets.top);
	}

	protected boolean haveShapes(int index[]) {
		double maxX = editor.getMap().getMaxX();
		double maxY = editor.getMap().getMaxY();
		double minX = editor.getMap().getMinX();
		double minY = editor.getMap().getMinY();
		double xSize = (maxX - minX) / xLength;
		double ySize = (maxY - minY) / yLength;		
		Iterator<GMLShape> shapes = editor.getMap().getAllShapes().iterator();
		while (shapes.hasNext()) {
			GMLShape shape = shapes.next();
			if (shape.getCentreX() >= minX + index[0] * xSize && shape.getCentreX() <=  minX + (index[0] + 1)* xSize&& shape.getCentreY()>=minY + index[1] * ySize && shape.getCentreY()<=minY + (index[1] + 1)* ySize) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * check whether the sum of distribution is 1
	 * @return
	 */
	public boolean isOne()
	{
		double sum=sum();
		if(Math.abs(sum-1)<0.000001)
			return true;
		else {
			return false;
		}
	}
	
	public double sum()
	{
		double sum=0;
		for(int i=0;i<dis.length;i++)
			for(int j=0;j<dis[i].length;j++)
				sum+=dis[i][j];
		return sum;
	}
}
