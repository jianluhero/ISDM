package maps.gml;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import rescuecore2.log.Logger;

public class ConstructRoad extends Component {

	public final static double road_Width = 60.0;
	
	private Area area;
	
	//TODO: for very small angles, need to remove it and remove the corresponding point

	/**
	 * according to parallel liens to draw path 
	 */
	public Path2D.Double getPath(List<Point2D.Double> points) {
		ArrayList<Point2D.Double> r1 = new ArrayList<Point2D.Double>();
		ArrayList<Point2D.Double> r2 = new ArrayList<Point2D.Double>();
		if (points.size() == 0 || points.size() == 1) {
			Logger.error("error: the rounting points are not correct!");
			return null;
		}
		if (points.size() == 2) {
			Line2D.Double line = new Line2D.Double(points.get(0),
					points.get(1));
			Line2D.Double[] lines = getPerpendicularPoint(line,road_Width);
			Point2D.Double p1=new Point2D.Double(lines[0].x1, lines[0].y1);
			Point2D.Double p2=new Point2D.Double(lines[0].x2, lines[0].y2);
			Point2D.Double p3=new Point2D.Double(lines[1].x1, lines[1].y1);
			Point2D.Double p4=new Point2D.Double(lines[1].x2, lines[1].y2);
			r1.add(p1);
			r2.add(p2);
			if(Math.abs(points.get(0).getX()-points.get(1).getX())<0.00000001)
			{
				
			}else
			{
				if((points.get(0).getY()-points.get(1).getY())/(points.get(0).getX()-points.get(1).getX())==(p3.y-p1.y)/(p3.x-p1.x))
				{
					r1.add(p3);
					r2.add(p4);
				}
				else {
					r1.add(p4);
					r2.add(p3);
				}
			}
		}
		else {
			for (int i = 0; i < points.size() - 2; i++) {
				double length1=getLineLength(new Line2D.Double(points.get(i),
						points.get(i + 1)));
				double length2=getLineLength(new Line2D.Double(points.get(i+1),
						points.get(i + 2)));
				double minLength=length1>length2?length2:length1;
				double angle=getAngle(new double[] { points.get(i + 1).getX(),
						points.get(i + 1).getY(), 0 }, new double[] { points.get(i).getX(),
								points.get(i).getY(), 0 },
						new double[] { points.get(i + 2).getX(),
								points.get(i + 2).getY(), 0 });
				double w=minLength*Math.sin(angle/2)*2*0.8;
				double roadWidth=w<road_Width?w:road_Width;	
				System.out.println(length1+"--------"+length2+"====="+roadWidth);
				roadWidth=road_Width;
				
				Point2D.Double innerP1;
				Point2D.Double outerP1;
				boolean isR1Inner;
				Point2D.Double innerP2;
				Point2D.Double outerP2;

				Point2D.Double p1;
				Point2D.Double p2;
				if (i == 0) {
					Line2D.Double line = new Line2D.Double(points.get(i),
							points.get(i + 1));
					Line2D.Double[] lines = getPerpendicularPoint(line,roadWidth);
					p1 = new Point2D.Double(lines[0].getX1(), lines[0].getY1());
					p2 = new Point2D.Double(lines[0].getX2(), lines[0].getY2());
				} else {
					p1 = r1.get(r1.size() - 1);
					p2 = r2.get(r2.size() - 1);
				}
				double angle1 = getAngle(new double[] { points.get(i + 1).getX(),
						points.get(i + 1).getY(), 0 }, new double[] { p2.getX(), p2.getY(), 0 },
						new double[] { points.get(i + 2).getX(),
								points.get(i + 2).getY(), 0 });
				double angle2 = getAngle(new double[] { points.get(i + 1).getX(),
						points.get(i + 1).getY(), 0 },
						new double[] { p1.getX(), p1.getY(), 0 }, new double[] {
								points.get(i + 2).getX(), points.get(i + 2).getY(),
								0 });
				if (angle1 > angle2) {
					innerP1 = p1;
					outerP1 = p2;
					isR1Inner = true;
					if (i == 0) {
						r1.add(innerP1);
						r2.add(outerP1);
					}
				} else {
					innerP1 = p2;
					outerP1 = p1;
					isR1Inner = false;
					if (i == 0) {
						r1.add(outerP1);
						r2.add(innerP1);
					}
				}
				
				Line2D.Double line = new Line2D.Double(points.get(i + 1),
						points.get(i + 2));
				Line2D.Double[] lines = getPerpendicularPoint(line,roadWidth);
				Point2D.Double p3 = new Point2D.Double(lines[1].getX1(),
						lines[1].getY1());
				Point2D.Double p4 = new Point2D.Double(lines[1].getX2(),
						lines[1].getY2());
				double angle3 = getAngle(new double[] { points.get(i + 1).getX(),
						points.get(i + 1).getY(), 0 },
						new double[] { p3.getX(), p3.getY(), 0 }, new double[] {
								points.get(i).getX(), points.get(i).getY(), 0 });
				double angle4 = getAngle(new double[] { points.get(i + 1).getX(),
						points.get(i + 1).getY(), 0 },
						new double[] { p4.getX(), p4.getY(), 0 }, new double[] {
								points.get(i).getX(), points.get(i).getY(), 0 });
				if (angle4 > angle3) {
					innerP2 = p3;
					outerP2 = p4;
				} else {
					innerP2 = p4;
					outerP2 = p3;
				}
				if (points.get(i + 1).getX() - points.get(i).getX() != 0
						&& points.get(i + 2).getX() - points.get(i + 1).getX() != 0) {
					// for inner points
					double a1 = (points.get(i + 1).getY() - points.get(i).getY())
							/ (points.get(i + 1).getX() - points.get(i).getX());
					double a2 = (points.get(i + 2).getY() - points.get(i + 1)
							.getY())
							/ (points.get(i + 2).getX() - points.get(i + 1).getX());
					double b1 = innerP1.getY() - a1 * innerP1.getX();
					double b2 = innerP2.getY() - a2 * innerP2.getX();
					double x = (b2 - b1) / (a1 - a2);
					double y = a1 * x + b1;
					if (isR1Inner)
						r1.add(new Point2D.Double(x, y));
					else
						r2.add(new Point2D.Double(x, y));
					// for outer points
					a1 = (points.get(i + 1).getY() - points.get(i).getY())
							/ (points.get(i + 1).getX() - points.get(i).getX());
					a2 = (points.get(i + 2).getY() - points.get(i + 1).getY())
							/ (points.get(i + 2).getX() - points.get(i + 1).getX());
					b1 = outerP1.getY() - a1 * outerP1.getX();
					b2 = outerP2.getY() - a2 * outerP2.getX();
					x = (b2 - b1) / (a1 - a2);
					y = a1 * x + b1;
					if (isR1Inner)
						r2.add(new Point2D.Double(x, y));
					else
						r1.add(new Point2D.Double(x, y));
				} else if (points.get(i + 1).getX() - points.get(i).getX() == 0
						&& points.get(i + 2).getX() - points.get(i + 1).getX() != 0) {
					// for inner points
					double a2 = (points.get(i + 2).getY() - points.get(i + 1)
							.getY())
							/ (points.get(i + 2).getX() - points.get(i + 1).getX());
					double b2 = innerP2.getY() - a2 * innerP2.getX();
					double x = innerP1.getX();
					double y = a2 * x + b2;
					if (isR1Inner)
						r1.add(new Point2D.Double(x, y));
					else
						r2.add(new Point2D.Double(x, y));
					// for outer points
					a2 = (points.get(i + 2).getY() - points.get(i + 1).getY())
							/ (points.get(i + 2).getX() - points.get(i + 1).getX());
					b2 = outerP2.getY() - a2 * outerP2.getX();
					x = outerP1.getX();
					y = a2 * x + b2;
					if (isR1Inner)
						r2.add(new Point2D.Double(x, y));
					else
						r1.add(new Point2D.Double(x, y));
				} else if (points.get(i + 1).getX() - points.get(i).getX() != 0
						&& points.get(i + 2).getX() - points.get(i + 1).getX() == 0) {
					
					// for inner points
					double a1 = (points.get(i + 1).getY() - points.get(i).getY())
							/ (points.get(i + 1).getX() - points.get(i).getX());
					double b1 = innerP1.getY() - a1 * innerP1.getX();
					double x = innerP2.getX();
					double y = a1 * x + b1;
					if (isR1Inner)
						r1.add(new Point2D.Double(x, y));
					else
						r2.add(new Point2D.Double(x, y));
					// for outer points
					a1 = (points.get(i + 1).getY() - points.get(i).getY())
							/ (points.get(i + 1).getX() - points.get(i).getX());
					b1 = outerP1.getY() - a1 * outerP1.getX();
					x = outerP2.getX();
					y = a1 * x + b1;
					if (isR1Inner)
						r2.add(new Point2D.Double(x, y));
					else
						r1.add(new Point2D.Double(x, y));
				} else if (points.get(i + 1).getX() - points.get(i).getX() == 0
						&& points.get(i + 2).getX() - points.get(i + 1).getX() == 0) {
					Logger.error("error: the rounting points are not correct!");
					return null;
				}
				if(i==points.size()-3)
				{
					if(isR1Inner)
					{
						r1.add(innerP2);
						r2.add(outerP2);
					}
					else
					{
						r2.add(innerP2);
						r1.add(outerP2);
					}
				}
			}
		}
		Path2D.Double path=new Path2D.Double();
		path.moveTo(r1.get(0).x, r1.get(0).y);
		for(int i=1;i<r1.size();i++)
		{
			path.lineTo(r1.get(i).x, r1.get(i).y);
			//System.out.println(r1.get(i).x+" "+r1.get(i).y);
		}
		for(int i=0;i<r2.size();i++)
		{
			path.lineTo(r2.get(r2.size()-1-i).x, r2.get(r2.size()-1-i).y);
			//System.out.println(r2.get(i).x+" "+r2.get(i).y);
		}
		path.closePath();
		return path;
	}
	
	/**
	 * TODO: if two lines have very small angles, then if the gap is small than roadwidth
	 * need to remove that point
	 * @param points
	 * @return
	 */
	public ArrayList<ArrayList<Point2D.Double>> getEvacutionRoutine(ArrayList<ArrayList<Point2D.Double>> points)
	{
		area=new Area(getPath(points.get(0)));
		for(int i=1;i<points.size();i++)
		{
			ArrayList<Point2D.Double> p=points.get(i);
			Area temp=new Area(getPath(p));
			area.add(temp);
		}
		PathIterator it=area.getPathIterator(null);
		ArrayList<ArrayList<Point2D.Double>> result=new ArrayList<ArrayList<Point2D.Double>>();
		ArrayList<Point2D.Double> r=new ArrayList<Point2D.Double>();
		while(!it.isDone())
		{
			double[]p=new double[6];
			int value=it.currentSegment(p);
			System.out.println(value);
			if(value==PathIterator.SEG_CLOSE)
			{
				ArrayList<Point2D.Double> r1=new ArrayList<Point2D.Double>(r);
				result.add(r1);
				r.clear();
			}
			else if(value!=PathIterator.SEG_MOVETO){
				if(Math.abs(p[0])>0.000001)
				{
					r.add(new Point2D.Double(p[0],p[1]));
				}
				if(Math.abs(p[2])>0.000001)
				{
					r.add(new Point2D.Double(p[2],p[3]));
				}
				if(Math.abs(p[4])>0.000001)
				{
					r.add(new Point2D.Double(p[4],p[5]));
				}
			}
			it.next();
		}
		return result;
	}
	
	public void paint (Graphics g) {
	    Graphics2D g2 = (Graphics2D) g;
	    Point2D.Double p1=new Point2D.Double(100.5, 100.5);
	    Point2D.Double p2=new Point2D.Double(140.5, 140.5);
	    Point2D.Double p3=new Point2D.Double(140.5, 200.5);
	    Point2D.Double p4=new Point2D.Double(100.5, 240.5);
	    Point2D.Double p5=new Point2D.Double(200.5, 240.5);
	    g2.setColor(Color.red);
	  //  g2.draw(new Line2D.Double(p1, p2));
	  //  g2.draw(new Line2D.Double(p2, p3));
	 //   g2.draw(new Line2D.Double(p3, p4));
	//    g2.draw(new Line2D.Double(p4, p5));
	    ArrayList<Point2D.Double> a1=new ArrayList<Point2D.Double>();
	    a1.add(p1);
	    a1.add(p2);
	 	a1.add(p3);
		a1.add(p4);
		a1.add(p5);
	  //  g2.draw(getPath(a1));
	    
	    p1=new Point2D.Double(300.5, 300.5);
	    p2=new Point2D.Double(320.5, 330.5);
	    p3=new Point2D.Double(360.5, 410.5);
	    p4=new Point2D.Double(340.5, 410.5);
	    p5=new Point2D.Double(410.5, 420.5);
	    g2.setColor(Color.green);
	    g2.draw(new Line2D.Double(p1, p2));
	    g2.draw(new Line2D.Double(p2, p3));
	    g2.draw(new Line2D.Double(p3, p4));
	    g2.draw(new Line2D.Double(p4, p5));
	    ArrayList<Point2D.Double> a2=new ArrayList<Point2D.Double>();
	    a2.add(p1);
	    a2.add(p2);
	    a2.add(p3);
	    a2.add(p4);
	    a2.add(p5);
	    g2.draw(getPath(a2));
	    
	    ArrayList<ArrayList<Point2D.Double>> a=new ArrayList<ArrayList<Point2D.Double>>();
	    g2.setColor(Color.blue);
	  //  a.add(a1);
	   	a.add(a2);
	    ArrayList<ArrayList<Point2D.Double>> r=getEvacutionRoutine(a);
	    g2.draw(area);

	    for(int i=0;i<r.size();i++)
	    {
	    	for(int j=0;j<r.get(i).size();j++)
	    	{
	    		System.out.println(r.get(i).get(j).x+" "+r.get(i).get(j).y);
	    	}
	    	System.out.println("============");
	    }	    	
	}

	 // "main" method
	  public static void main(String[] av) {
	    JFrame f = new JFrame("Evacuation Routines");
	    Container cp = f.getContentPane();

	    //cp.setBackground(Color.white);
	    ConstructRoad r = new ConstructRoad();
	    cp.setLayout(new BorderLayout());
	    cp.add(BorderLayout.CENTER, r);
	    f.pack();
	    f.setSize(new Dimension(400,300));
	    f.setVisible(true);
	    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	  }


	public double getAngle(double p0[], double p1[], double p2[]) {
		double[] v0 = Geometry.createVector(p0, p1);
		double[] v1 = Geometry.createVector(p0, p2);
		double dotProduct = Geometry.computeDotProduct(v0, v1);
		double length1 = Geometry.length(v0);
		double length2 = Geometry.length(v1);
		double denominator = length1 * length2;
		double product = denominator != 0.0 ? dotProduct / denominator : 0.0;
		double angle = Math.acos(product);
		return angle;
	}

	public Line2D.Double[] getPerpendicularPoint(Line2D.Double line, double roadWidth) {
		Line2D.Double result[] = new Line2D.Double[2];
		double a;
		if (line.y1 != line.y2) {
			if (line.x1 == line.x2)
				a = 0;
			else
				a = -1 / ((line.y1 - line.y2) / (line.x1 - line.x2));

			double b = line.y1 - a * line.x1;
			double c = roadWidth / 2;
			double x1 = c/Math.sqrt(1+a*a)+line.x1;
			double y1 = a * x1 + b;
			double x2 = -c/Math.sqrt(1+a*a)+line.x1;
			double y2 = a * x2 + b;
			result[0] = new Line2D.Double(x1, y1, x2, y2);

			b = line.y2 - a * line.x2;
			x1 =  c/Math.sqrt(1+a*a)+line.x2;
			y1 = a * x1 + b;
			x2 = -c/Math.sqrt(1+a*a)+line.x2;
			y2 = a * x2 + b;
			result[1] = new Line2D.Double(x1, y1, x2, y2);
		} else {
			double x1 = line.x1;
			double y1 = line.y1 - roadWidth / 2;
			double x2 = line.x1;
			double y2 = line.y1 + roadWidth / 2;
			result[0] = new Line2D.Double(x1, y1, x2, y2);

			x1 = line.x2;
			y1 = line.y2 - roadWidth / 2;
			x2 = line.x2;
			y2 = line.y2 + roadWidth / 2;
			result[1] = new Line2D.Double(x1, y1, x2, y2);
		}
		return result;
	}

	public double getLineLength(Line2D.Double line) {
		double x1 = line.getX1();
		double y1 = line.getY1();
		double x2 = line.getX2();
		double y2 = line.getY2();
		return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
	}
}
