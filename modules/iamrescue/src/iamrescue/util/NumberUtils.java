package iamrescue.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.commons.lang.Validate;

public class NumberUtils {

    private static int byteRange = Byte.MAX_VALUE - Byte.MIN_VALUE;
    private static int shortRange = Short.MAX_VALUE - Short.MIN_VALUE;

    /**
     * Converts a byte to a percentage (between 0.0 and 1.0), where
     * Byte.MIN_VALUE is mapped to 0.0 and Byte.MAX_VALUE is mapped to 1.0
     * @param percentage
     * @return
     */
    public static double convertByteToPercentage(byte b) {
        return (b - Byte.MIN_VALUE) / (double) byteRange;
    }

    /**
     * Converts a percentage (between 0.0 and 1.0) to a byte, where 0.0 is
     * mapped to Byte.MIN_VALUE and 1.0 is mapped to Byte.MAX_VALUE
     * @param percentage
     * @return
     */
    public static byte convertPercentageToByte(double percentage) {
        if (percentage > 1 || percentage < 0) {
            throw new IllegalArgumentException("Value out of range "
                    + percentage);
        }
        return (byte) (Math.round(percentage * byteRange) + Byte.MIN_VALUE);
    }

    public static boolean equals(Number object1, Number object2, double delta) {
        double d1 = object1.doubleValue();
        double d2 = object2.doubleValue();

        if (d1 == d2) {
            return true;
        }

        return (Math.max(d1, d2) / Math.min(d1, d2) - 1) < delta;
    }

    public static boolean almostEquals(Number number, Number number2, double d,
            int i) {
        if (!equals(number, number2, d)) {
            return (Math.abs(number.doubleValue() - number2.doubleValue()) < i);
        }
        else {
            return true;
        }
    }

    /**
     * Converts a byte to a percentage (between 0.0 and 1.0), where
     * Byte.MIN_VALUE is mapped to 0.0 and Byte.MAX_VALUE is mapped to 1.0
     * @param percentage
     * @return
     */
    public static double convertShortToPercentage(short b) {
        return (b - Short.MIN_VALUE) / (double) shortRange;
    }

    /**
     * Converts a percentage (between 0.0 and 1.0) to a byte, where 0.0 is
     * mapped to Byte.MIN_VALUE and 1.0 is mapped to Byte.MAX_VALUE
     * @param percentage
     * @return
     */
    public static short convertPercentageToShort(double percentage) {
        if (percentage > 1 || percentage < 0) {
            throw new IllegalArgumentException("Value out of range "
                    + percentage);
        }
        return (short) (Math.round(percentage * shortRange) + Short.MIN_VALUE);
    }

    public static double computePercentage(double value, double min, double max) {
        Validate.isTrue(value >= min);
        Validate.isTrue(value <= max);

        return (value - min) / (max - min);
    }

    public static double computeValue(double percentage, double min, double max) {
        return percentage * (max - min) + min;
    }

    /**
     * Convert the given number into an instance of the given target class.
     * Copied from org.springframework.util.NumberUtils
     * @param number
     *            the number to convert
     * @param targetClass
     *            the target class to convert to
     * @return the converted number
     * @throws IllegalArgumentException
     *             if the target class is not supported (i.e. not a standard
     *             Number subclass as included in the JDK)
     * @see java.lang.Byte
     * @see java.lang.Short
     * @see java.lang.Integer
     * @see java.lang.Long
     * @see java.math.BigInteger
     * @see java.lang.Float
     * @see java.lang.Double
     * @see java.math.BigDecimal
     */
    public static Number convertNumberToTargetClass(Number number,
            Class targetClass) throws IllegalArgumentException {

        assert number != null : "Number must not be null";
        assert targetClass != null : "Target class must not be null";

        if (targetClass.isInstance(number)) {
            return number;
        }
        else if (targetClass.equals(Byte.class)) {
            long value = number.longValue();
            if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
                raiseOverflowException(number, targetClass);
            }
            return new Byte(number.byteValue());
        }
        else if (targetClass.equals(Short.class)) {
            long value = number.longValue();
            if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                raiseOverflowException(number, targetClass);
            }
            return new Short(number.shortValue());
        }
        else if (targetClass.equals(Integer.class)) {
            long value = number.longValue();
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                raiseOverflowException(number, targetClass);
            }
            return Integer.valueOf(number.intValue());
        }
        else if (targetClass.equals(Long.class)) {
            return Long.valueOf(number.longValue());
        }
        else if (targetClass.equals(Float.class)) {
            return Float.valueOf(number.floatValue());
        }
        else if (targetClass.equals(Double.class)) {
            return Double.valueOf(number.doubleValue());
        }
        else if (targetClass.equals(BigInteger.class)) {
            return BigInteger.valueOf(number.longValue());
        }
        else if (targetClass.equals(BigDecimal.class)) {
            // using BigDecimal(String) here, to avoid unpredictability of
            // BigDecimal(double)
            // (see BigDecimal javadoc for details)
            return new BigDecimal(number.toString());
        }
        else {
            throw new IllegalArgumentException("Could not convert number ["
                    + number + "] of type [" + number.getClass().getName()
                    + "] to unknown target class [" + targetClass.getName()
                    + "]");
        }
    }

    /**
     * Raise an overflow exception for the given number and target class.
     * @param number
     *            the number we tried to convert
     * @param targetClass
     *            the target class we tried to convert to
     */
    private static void raiseOverflowException(Number number, Class targetClass) {
        throw new IllegalArgumentException("Could not convert number ["
                + number + "] of type [" + number.getClass().getName()
                + "] to target class [" + targetClass.getName() + "]: overflow");
    }

}
