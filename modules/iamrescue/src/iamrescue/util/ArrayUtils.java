package iamrescue.util;

public class ArrayUtils {

    public static byte[] concatenate(byte[] array1, byte[] array2) {
        if (array1 == null) {
            return array2;
        }

        int length1 = array1.length;
        int length2 = array2.length;
        byte[] concat = new byte[length1 + length2];

        System.arraycopy(array1, 0, concat, 0, length1);
        System.arraycopy(array2, 0, concat, length1, length2);
        return concat;
    }

    public static short min(short[] vertices) {
        short min = Short.MAX_VALUE;

        for (int i = 0; i < vertices.length; i++) {
            if (vertices[i] < min) {
                min = vertices[i];
            }
        }

        return min;

    }

    public static short max(short[] array) {
        short max = Short.MIN_VALUE;

        for (int i = 0; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }

    public static byte max(byte[] array) {
        byte max = Byte.MIN_VALUE;

        for (int i = 0; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }

    public static double sum(double[] array) {
        double sum = 0.0;

        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }

        return sum;
    }
}
