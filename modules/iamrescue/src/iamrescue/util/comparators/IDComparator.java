/**
 * 
 */
package iamrescue.util.comparators;

import java.io.Serializable;
import java.util.Comparator;

import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 */
public class IDComparator implements Comparator<EntityID>, Serializable {

    private static final long serialVersionUID = 471268162844406662L;
    public static final IDComparator DEFAULT_INSTANCE = new IDComparator();

    public int compare(EntityID id1, EntityID id2) {
        if (id1.getValue() < id2.getValue()) {
            return -1;
        }
        else if (id1.getValue() > id2.getValue()) {
            return 1;
        }
        else {
            return 0;
        }
    }
}
