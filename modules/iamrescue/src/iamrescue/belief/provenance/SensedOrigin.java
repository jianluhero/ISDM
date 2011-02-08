/**
 * 
 */
package iamrescue.belief.provenance;

/**
 * @author Sebastian
 * 
 */
public class SensedOrigin implements IOrigin {

	public static final IOrigin INSTANCE = new SensedOrigin();

	private SensedOrigin() {

	}

	@Override
	public boolean equals(Object obj) {
		return (obj != null && obj instanceof SensedOrigin);
	}

	@Override
	public int hashCode() {
		return 29;
	}
}
