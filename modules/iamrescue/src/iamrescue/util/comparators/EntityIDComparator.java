/**
 * 
 */
package iamrescue.util.comparators;

import java.io.Serializable;
import java.util.Comparator;

import rescuecore2.standard.entities.StandardEntity;

/**
 * @author Sebastian
 */
public class EntityIDComparator implements Comparator<StandardEntity>,
		Serializable {

	private static final long serialVersionUID = 8847528572477242112L;
	public static final EntityIDComparator DEFAULT_INSTANCE = new EntityIDComparator();

	public int compare(StandardEntity se1, StandardEntity se2) {
		return IDComparator.DEFAULT_INSTANCE.compare(se1.getID(), se2.getID());
	}
}
