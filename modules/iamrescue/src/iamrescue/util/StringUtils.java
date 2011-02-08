package iamrescue.util;

public class StringUtils {

    private static String printIndented(Object o, int indentation) {
        return org.apache.commons.lang.StringUtils.repeat("  ", indentation)
        + "" + o + "\n";
    }

    /**
     * Indents a multi-line string. Strings are assumed to be terminated with
     * "\n"
     * @param string
     * @param indentation
     *            the amount of indentation (in characters)
     * @return
     */
    public static String indent(String string, int indentation) {
        String[] split = string.split("\n");
        String[] indented = indent(split, indentation);
        return org.apache.commons.lang.StringUtils.join(indented, "\n");
    }

    private static String[] indent(String[] split, int indentation) {
        for (int i = 0; i < split.length; i++) {
            split[i] = org.apache.commons.lang.StringUtils.repeat(" ",
                    indentation)
                    + split[i];
        }
        return split;
    }
}
