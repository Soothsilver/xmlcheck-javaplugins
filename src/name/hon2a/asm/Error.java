package name.hon2a.asm;

/**
 * %Error wrapper class. Stores information pertinent to error and outputs them
 * in human-readable form through its toString() method.
 *
 * @author %hon2a
 */
public class Error {

	/// plain message string
	protected final String message;

	/**
	 * Sole default constructor. Sets plain error message string.
	 * 
	 * @param message error message
	 */
	public Error (String message) {
		this.message = message;
	}

	/**
	 * Retrieve error message in human-readable format.
	 *
	 * Descentants must override this function or risk that their additional data
	 * will not be used.
	 * 
	 * @return %Error message in readable format.
	 */
	@Override
	public String toString () {
		return this.message;
	}
}
