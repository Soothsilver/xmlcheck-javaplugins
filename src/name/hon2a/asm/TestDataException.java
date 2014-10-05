package name.hon2a.asm;

/**
 * Wrapper exception used in case of fatal error in tested data.
 *
 * Use-case: Missing source file.
 * Doesn't add any functionality to TestException, but makes case distinction
 * and therefore should be used wherever applicable.
 *
 * @author %hon2a
 */
public class TestDataException extends TestException {

	public TestDataException (String message) {
		super(message);
	}

	public TestDataException (String message, Throwable cause) {
		super(message, cause);
	}
}
