package name.hon2a.asm;

/**
 * Wrapper exception used in case of misuse of @link Test test @endlink library
 * methods by test developers.
 *
 * Use-case: Calling methods with illegal arguments.
 * Constructors prepend supplied error messages with generic message and apply
 * Utils::indentError formatting.
 *
 * @author %hon2a
 */
public class TestCodeException extends TestException {

	public TestCodeException (String message) {
		super(Utils.indentError("Test is not written properly", message));
	}

	public TestCodeException (String message, Throwable cause) {
		super(Utils.indentError("Test is not written properly", message), cause);
	}
}
