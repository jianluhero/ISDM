/**
 * 
 */
package iamrescue.belief.provenance;

/**
 * @author Sebastian
 * 
 */
public class InferredOrigin implements IOrigin {

	public static final IOrigin INSTANCE = new InferredOrigin();

	private InferredOrigin() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj != null && obj instanceof InferredOrigin);
	}

	@Override
	public int hashCode() {
		return 13;
	}
}
