/**
 * 
 */
package iamrescue.util;

import java.io.Serializable;
import java.util.Comparator;

import rescuecore2.standard.entities.StandardEntity;

/**
 * @author Sebastian
 */
public class EntityComparator implements Comparator<StandardEntity>,
		Serializable {

	private static final long serialVersionUID = 4000963822538402883L;

	public int compare(StandardEntity se1, StandardEntity se2) {
		if (se1.getID().getValue() < se2.getID().getValue()) {
			return -1;
		} else if (se1.getID().getValue() > se2.getID().getValue()) {
			return 1;
		} else {
			return 0;
		}
	}
}
