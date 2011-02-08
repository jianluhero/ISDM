package iamrescue.util;

import java.util.Arrays;

public class BaseConverter {
	public static int[] convertBase(int[] original, int originalBase,
			int returnBase) {
		return convertBase(convertToDecimal(original, originalBase), returnBase);
	}

	public static int convertToDecimal(int[] original, int originalBase) {
		int number = 0;
		int currentValue = 1;
		for (int i = original.length - 1; i >= 0; i--) {
			number += original[i] * currentValue;
			currentValue *= originalBase;
		}
		return number;
	}

	public static boolean[] convertToBinary(int decimal) {
		int[] binary = convertBase(decimal, 2);
		boolean[] returnArray = new boolean[binary.length];
		for (int i = 0; i < binary.length; i++) {
			returnArray[i] = binary[i] > 0;
		}
		return returnArray;
	}

	public static boolean[] convertToBinary(int[] original, int originalBase) {
		return convertToBinary(convertToDecimal(original, originalBase));
	}

	public static int[] convertFromBinary(boolean[] binary, int returnBase) {
		return convertBase(convertToDecimal(binary), returnBase);
	}

	public static int convertToDecimal(boolean[] binary) {
		int number = 0;
		int currentValue = 1;
		for (int i = binary.length - 1; i >= 0; i--) {
			if (binary[i]) {
				number += currentValue;
			}
			currentValue *= 2;
		}
		return number;
	}

	public static int[] convertBase(int number, int returnBase) {
		if (returnBase < 2) {
			throw new IllegalArgumentException("Return base must be > 1");
		}
		if (number == 0) {
			return new int[0];
		}
		int places = 1 + (int) (Math.log(number) / Math.log(returnBase));
		if (places < 0) {
			throw new IllegalArgumentException("Could not convert " + number
					+ " to " + returnBase);
		}
		int[] returnNumber = new int[places];
		int currentValue = 1;// (int) Math.pow(returnBase, places - 1);
		for (int i = 1; i <= places - 1; i++) {
			currentValue *= returnBase;
		}
		int remaining = number;
		for (int i = 0; i < places; i++) {
			returnNumber[i] = remaining / currentValue;
			remaining %= currentValue;
			currentValue /= returnBase;
		}
		return returnNumber;
	}

	public static int[] padToLength(int[] array, int length) {
		if (length < array.length) {
			throw new IllegalArgumentException("Array "
					+ Arrays.toString(array) + " is already longer than "
					+ length);
		} else if (length == array.length) {
			return array;
		} else {
			int[] newArray = new int[length];
			int offset = length - array.length;
			for (int i = 0; i < length; i++) {
				if (i < offset) {
					newArray[i] = 0;
				} else {
					newArray[i] = array[i - offset];
				}
			}
			return newArray;
		}
	}

	public static boolean[] padToLength(boolean[] array, int length) {
		if (length < array.length) {
			throw new IllegalArgumentException("Array "
					+ Arrays.toString(array) + " is already longer than "
					+ length);
		} else if (length == array.length) {
			return array;
		} else {
			boolean[] newArray = new boolean[length];
			int offset = length - array.length;
			for (int i = 0; i < length; i++) {
				if (i < offset) {
					newArray[i] = false;
				} else {
					newArray[i] = array[i - offset];
				}
			}
			return newArray;
		}
	}

	public static void main(String[] args) {
		System.out.println("123 in binary: "
				+ Arrays.toString(padToLength(convertBase(123, 2), 10)));

		System.out.println("And back: "
				+ Arrays.toString(padToLength(convertBase(convertBase(123, 2),
						2, 10), 10)));

		System.out.println("123 in oct: "
				+ Arrays.toString(padToLength(convertBase(123, 8), 10)));

		System.out.println("And back: "
				+ Arrays.toString(padToLength(convertBase(convertBase(123, 8),
						8, 10), 10)));

		System.out.println("123 in base 5: "
				+ Arrays.toString(padToLength(convertBase(123, 5), 5)));

		System.out.println("And back: "
				+ Arrays.toString(padToLength(convertBase(convertBase(123, 5),
						5, 10), 10)));

		int[] status = { 2, 2 };

		System.out.println("2,2 to decimal: " + convertToDecimal(status, 3));
		System.out.println("2,2 to binary: "
				+ Arrays.toString(convertToBinary(status, 3)));
		System.out.println("2,2 to binary: "
				+ Arrays.toString(convertBase(status, 3, 2)));

	}
}
