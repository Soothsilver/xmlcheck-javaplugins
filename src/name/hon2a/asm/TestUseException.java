package name.hon2a.asm;

/**
 * Wrapper exception used in case of @link Test test @endlink misuse.
 *
 * Use-case: Test is running correctly, but has received incorrect input.
 * Constructors prepend supplied error messages with generic test misuse message
 * and apply Utils::indentError formatting.
 *
 * @author %hon2a
 */
public class TestUseException extends TestException {

	public TestUseException (String message) {
		super(Utils.indentError("Test is not being used properly", message));
	}

	public TestUseException (String message, Throwable cause) {
		super(Utils.indentError("Test is not being used properly", message), cause);
	}
}
