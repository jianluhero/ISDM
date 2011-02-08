package iamrescue.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

public class GeneralUtils {

    public static <T extends Object> T getRandomElement(Collection<T> collection) {
        int index = new Random().nextInt(collection.size());
        Iterator<T> iterator = collection.iterator();

        for (int i = 0; i <= index - 1; i++) {
            iterator.next();
        }

        return iterator.next();
    }

    public static <T extends Object> T getRandomElement(T[] edges) {
        int index = new Random().nextInt(edges.length);
        return edges[index];
    }
}
