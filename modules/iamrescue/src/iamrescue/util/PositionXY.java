package iamrescue.util;

import java.io.Serializable;

import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;

/**
 * Represent a point with x,y coordinates.
 * 
 * @author ss2
 * 
 */
public class PositionXY implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7679113812046297498L;

	private final int x;

	private final int y;

	/**
	 * Creates a new position
	 * 
	 * @param x
	 *            X-coordinate
	 * @param y
	 *            Y-coordinate
	 */
	public PositionXY(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public PositionXY(Pair<Integer, Integer> xy) {
		this.x = xy.first();
		this.y = xy.second();
	}

	public PositionXY roundPosition(Point2D doublePoint) {
		return new PositionXY((int) Math.round(doublePoint.getX()), (int) Math
				.round(doublePoint.getY()));
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public boolean equals(Object other) {
		boolean equal;
		if (other == null) {
			equal = false;
		} else if (!(other instanceof PositionXY)) {
			equal = false;
		} else {
			PositionXY otherP = (PositionXY) other;
			equal = (this.x == otherP.x && this.y == otherP.y);
		}
		return equal;
	}

	public double distanceTo(PositionXY other) {
		double diffX = other.getX() - getX();
		double diffY = other.getY() - getY();

		return Math.sqrt(diffX * diffX + diffY * diffY);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return x + 13 * y;
	}

	public double squareDistanceTo(PositionXY other) {
		double diffX = other.getX() - getX();
		double diffY = other.getY() - getY();

		return (diffX * diffX + diffY * diffY);
	}

	/**
	 * Convenience method to create an array of positions from an array of x/y
	 * coordinates.
	 * 
	 * @param xyArray
	 *            x/y array (even size)
	 * @return new array of PositionXY objects
	 */
	public static PositionXY[] createFromArray(int[] xyArray) {
		if (xyArray.length % 2 != 0) {
			throw new IllegalArgumentException(
					"Expecting an even-size array of x/y coordinates.");
		}
		PositionXY[] positionXYs = new PositionXY[xyArray.length / 2];
		for (int i = 0; i < xyArray.length; i = i + 2) {
			positionXYs[i / 2] = new PositionXY(xyArray[i], xyArray[i + 1]);
		}
		return positionXYs;
	}

	public Point2D toPoint2D() {
		return new Point2D(x, y);
	}

	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append('(');
		str.append(x);
		str.append(',');
		str.append(y);
		str.append(')');
		return str.toString();
	}
}
