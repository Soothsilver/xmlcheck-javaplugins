package name.hon2a.asm;

/**
 * Wrapper exception used to be used by @link Test tests @endlink .
 *
 * Descendants of this class should be used whenever possible.
 *
 * @author %hon2a
 */
public class TestException extends Exception {

	public TestException (String message) {
		super(message);
	}

	public TestException (String message, Throwable cause) {
		super(message, cause);
	}
}
