package name.hon2a.asm;

/**
 * %Error wrapper used by @link Test tests @endlink to specify details of goal
 * or test failure.
 *
 * Adds posibility to save filename and line number of error point of origin.
 *
 * @author %hon2a
 */
public class TestError extends Error {

	protected String sourcePath = null; ///< path of file where error occured
	protected int lineNumber = 0; ///< line number on which error occured

	/**
	 * Default constructor.
	 * 
	 * @param message error message
	 */
	public TestError (String message) {
		super(message);
	}

	/**
	 * Constructor for cases when file of error origin is known.
	 *
	 * @param message error message
	 * @param path path of file where error occured
	 */
	public TestError (String message, String path) {
		super(message);
		this.sourcePath = path;
	}

	/**
	 * Constructor for cases when file and line number of error origin are known.
	 *
	 * @param message error message
	 * @param path path of file where error occured
	 * @param lineNumber line number on which error occured
	 */
	public TestError (String message, String path, int lineNumber) {
		super(message);
		this.sourcePath = path;
		this.lineNumber = lineNumber;
	}

	/**
	 * Return all error info in single human-readable string.
	 *
	 * @return Human-readable error message.
	 */
	@Override
	public String toString () {
		StringBuilder builder = new StringBuilder(this.message);
		if (this.sourcePath != null) {
			builder.append(" (")
				.append(this.sourcePath);
			if (this.lineNumber > 0) {
				builder.append(" at line ")
					.append(this.lineNumber);
			}
			builder.append(")");
		}
		return builder.toString();
	}
}
