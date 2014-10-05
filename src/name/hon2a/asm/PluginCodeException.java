package name.hon2a.asm;

/**
 * Wrapper exception used in case of misuse of library plugin methods by 
 * @link Plugin plugin @endlink developers.
 *
 * Use-case: Calling methods with illegal arguments.
 * Constructors prepend supplied error messages with generic message and apply
 * Utils::indentError formatting.
 *
 * @author %hon2a
 */
public class PluginCodeException extends PluginException {

	public PluginCodeException (String message) {
		super(Utils.indentError("Plugin is not written properly", message));
	}

	public PluginCodeException (String message, Throwable cause) {
		super(Utils.indentError("Plugin is not written properly", message), cause);
	}
}
