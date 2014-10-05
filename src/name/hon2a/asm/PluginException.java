package name.hon2a.asm;

/**
 * Wrapper exception to be used by @link Plugin plugins @endlink .
 *
 * @author %hon2a
 */
public class PluginException extends Exception {

	public PluginException (String message) {
		super(message);
	}

	public PluginException (String message, Throwable cause) {
		super(message, cause);
	}
}
