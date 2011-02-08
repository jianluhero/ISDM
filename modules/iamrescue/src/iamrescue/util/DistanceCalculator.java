/**
 * 
 */
package iamrescue.util;

import rescuecore2.misc.Pair;

/**
 * @author Sebastian
 */
public class DistanceCalculator {
    public static double getDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(getSquaredDistance(x1, y1, x2, y2));
    }

    public static double getSquaredDistance(double x1, double y1, double x2,
            double y2) {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }

    public static double getDistance(Pair<Integer, Integer> location1,
            Pair<Integer, Integer> location2) {
        return Math.sqrt(getSquaredDistance(location1, location2));
    }

    public static double getSquaredDistance(Pair<Integer, Integer> location1,
            Pair<Integer, Integer> location2) {
        return getSquaredDistance(location1.first(), location1.second(),
                location2.first(), location2.second());
    }
}
