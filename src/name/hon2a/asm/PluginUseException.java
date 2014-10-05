package name.hon2a.asm;

/**
 * Wrapper exception used in case of @link Plugin plugin @endlink misuse.
 * 
 * Use-case: Plugin is running correctly, but has received incorrect input.
 * Constructors prepend supplied error messages with generic plugin misuse message
 * and apply Utils::indentError formatting.
 *
 * @author %hon2a
 */
public class PluginUseException extends PluginException {

	public PluginUseException (String message) {
		super(Utils.indentError("Plugin is not being used properly", message));
	}

	public PluginUseException (String message, Throwable cause) {
		super(Utils.indentError("Plugin is not being used properly", message), cause);
	}
}
