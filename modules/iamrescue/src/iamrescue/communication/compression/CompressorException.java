package iamrescue.communication.compression;


public class CompressorException extends Exception {

	public CompressorException(Exception e) {
		super(e);
	}
	
	public CompressorException(String reason) {
		super(reason);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 75777477076047476L;

}
