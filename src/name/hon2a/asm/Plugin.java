package name.hon2a.asm;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.ecs.xml.*;

/**
 * Abstract plugin for Assignment Manager.
 *
 * Provides layer of convenience between base structure of plugins and their
 * implementations. Descendants need only to implement their own criteria,
 * script body, and setUp() and execute() methods. Plugin response is generated
 * automatically using Criterion::check() for assessing final status of plugin
 * criteria.
 *
 * Plugins are standalone scripts to be run from console or from other plugins.
 * Sole access point, the run() method, accepts array of strings, so it can be
 * called directly from main application method.
 *
 * @author %hon2a
 */
public abstract class Plugin {

	/**
	 * %Criterion results wrapper.
	 *
	 * Criteria must implement Criterion::check method, which returns criterion
	 * status in this struct. Includes @ref passed flag, @ref fulfillment counter
	 * and error @ref details in case of failure.
	 */
	protected final class Results {

		protected boolean passed; ///< passed flag (true if criterion is met)
		protected int fulfillment; ///< fulfillment percentage (value from 0 to 100)
		protected String details; ///< error details (if criterion is not met)

		/**
		 * Simple constructor in case criterion is met.
		 */
		public Results () {
			this.passed = true;
			this.fulfillment = 100;
			this.details = "";
		}

		/**
		 * Complex constructor setting all members.
		 * 
		 * @param passed is criterion met
		 * @param fulfillment fulfillment percentage
		 * @param details error details
		 */
		public Results (boolean passed, int fulfillment, String details) {
			this.passed = passed;
			this.fulfillment = (fulfillment < 0) ? 0 : ((fulfillment > 100) ? 100 : fulfillment);
			this.details = details;
		}
	}

	/**
	 * Abstract plugin criterion.
	 *
	 * Descendants must implement Criterion::check method that returns criterion status.
	 */
	protected abstract class Criterion {

		/**
		 * Check whether criterion is met.
		 *
		 * @return %Criterion status as Results.
		 * @throws PluginException in case of inconsistent plugin state.
		 * @todo Future versions should implement also criterion weights.
		 */
		protected abstract Results check () throws PluginException;
	}

	/**
	 * minimum number of plugin arguments
	 *
	 * Every plugin must receive path to submission file as its first argument.
	 * That arguments is saved and removed before arguments are passed to Plugin::setUp.
	 */
	private static int MIN_ARGUMENTS = 1;

	
	private File dataFolder; ///< temporary folder with unpacked submission files
	private File outputFolder; ///< temporary folder for plugin output
	private Map<String, Criterion> criteria = new HashMap<String, Criterion>(); ///< plugin criteria

	protected Map<String, String> config; ///< plugin config

	/**
	 * Constructor for configurable plugins.
	 *
	 * @param config plugin configuration
	 */
	public Plugin (Map<String, String> config) {
		this.config = config;
	}

	/**
	 * Default constructor.
	 */
	public Plugin () {
	}

	/**
	 * Plugin initialization method.
	 *
	 * Should be used for setting up plugin criteria and such.
	 *
	 * @param params params passed to plugin on run
	 * @throws PluginException
	 */
	protected abstract void setUp (String[] params) throws PluginException;

	/**
	 * Plugin execution method.
	 *
	 * Should be used for executing tests, computing, ... anything that needs to
	 * be done before criteria can be checked.
	 * 
	 * @throws PluginException
	 */
	protected abstract void execute () throws PluginException;

	/**
	 * Run plugin (sole public access point).
	 *
	 * Plugins must be launched using this method. If plugin is standalone executable,
	 * all command-line arguments should be passed to this function.
	 * 
	 * @param args command-line arguments.
	 * First argument must be submission zip archive path. All other arguments are
	 * passed to Plugin::setUp method.
	 * @return XML string conform to Assignment Manager specs
	 */
	public final String run (String [] args) {
		try {
			if ((args == null) || (args.length < MIN_ARGUMENTS)) {
				throw new PluginUseException("Data file argument missing");
			}

			this.dataFolder = Utils.createTempDirectory();
			File dataFile = new File(args[0]);
			Utils.unzip(dataFile, this.dataFolder);
			this.outputFolder = Utils.createTempDirectory();

			String[] params = new String[args.length - 1];
			System.arraycopy(args, 1, params, 0, params.length);
			this.setUp(params);

			this.execute();

			return this.makeReplyXml();
		} catch (PluginException e) {
			return this.makeErrorXml(e.getMessage());
		} catch (Exception e) {
			return this.makeErrorXml(new StringBuilder("Java Exception: ")
					  .append(e.getMessage())
                      .append(e.getClass().toString())
                      .append(e.toString())
                      .append(Utils.EOL_STRING)
					  .toString());
		} finally {
			if (this.dataFolder != null) {
				Utils.removeDirectoryAndContents(this.dataFolder);
			}
			if (this.outputFolder != null) {
				Utils.removeDirectoryAndContents(this.outputFolder);
			}
		}
	}

	/**
	 * Translate relative path of source file to file descriptor.
	 *
	 * @param path relative path of source file
	 * @return Source file descriptor.
	 */
	protected final File getSourceFile (String path) {
		return new File(this.dataFolder, path);
	}

	/**
	 * Translate relative path of source file to absolute path.
	 *
	 * @param path relative path of source file
	 * @return Absolute path of source file.
	 */
	protected final String getSourcePath (String path) {
		return this.getSourceFile(path).getAbsolutePath();
	}

	/**
	 * Translate relative path of output file to file descriptor.
	 *
	 * @param path relative path of output file
	 * @return Output file descriptor.
	 */
	protected final File getOutputFile (String path) {
		return new File(this.outputFolder, path);
	}

	/**
	 * Translate relative path of output file to absolute path.
	 *
	 * @param path relative path of output file
	 * @return Absolute path of output file.
	 */
	protected final String getOutputPath (String path) {
		return this.getOutputFile(path).getAbsolutePath();
	}

	/**
	 * Check all plugin criteria and return results.
	 * 
	 * @return Map of results identified by criteria names.
	 * @throws PluginException
	 */
	private Map<String, Results> assessResults () throws PluginException {
		Map<String, Results> results = new HashMap<String, Results>(this.criteria.size());
		for (Map.Entry<String, Criterion> criterionPair : this.criteria.entrySet()) {
			results.put(criterionPair.getKey(), criterionPair.getValue().check());
		}
		return results;
	}

	/**
	 * Compress all contents of output folder into one zip archive.
	 *
	 * @return File descriptor of output archive.
	 * @throws PluginException
	 * @throws IOException
	 */
	private File packOutput () throws PluginException, IOException {
		File outputFile = null;
		if ((this.outputFolder != null) && (this.outputFolder.isDirectory())
				&& (this.outputFolder.list().length > 0)) {
			outputFile = Utils.createTempFile("zip");
			Utils.zip(this.outputFolder, outputFile);
		}
		return outputFile;
	}

	private String prepareErrorDetails (String str) {
		if (str == null) {
			return "";
		}
		str = Utils.escapeXml(str);
		if (this.dataFolder != null) {
			try {
				str = str.replaceAll("(?i)" + Pattern.quote(this.dataFolder.getCanonicalPath()), ".");
			} catch (IOException e) {
			}
		}
		return str;
	}

	/**
	 * Create and return regular plugin response XML conform to Assignment Manager specs.
	 *
	 * Response structure:
	 * @code
	 * <plugin-reply>
	 *		<output>
	 *			<file>OUTPUT_ARCHIVE_PATH</file>
	 *		</output>
	 *		<criterion name="CRITERION_NAME">
	 *			<success>CRITERION_SUCCESS</success>
	 *			<fulfillment>CRITERION_FULFILLMENT_PERCENTAGE</fulfillment>
	 *			<details>CRITERION_ERROR_DETAILS</details>
	 *		</criterion>
	 *		...
	 * </plugin-reply>
	 * @endcode
	 * 
	 * Output is optional. Number of criterion tags is not limited.
	 *
	 * CRITERION_SUCCESS is string representation of boolean
	 * 
	 * CRITERION_FULFILLMENT_PERCENTAGE is integer between 0 and 100 (inclusive)
	 *
	 * @return Plugin response XML string.
	 * @throws PluginException
	 * @throws IOException
	 */
	private String makeReplyXml () throws PluginException, IOException {
		Map<String, Results> results = this.assessResults();
		File outputFile = this.packOutput();

		XML reply = new XML("plugin-reply");
		if (outputFile != null) {
			reply.addElement(new XML("output")
				.addElement(new XML("file").addElement(outputFile.getAbsolutePath())));
		}
		for (Map.Entry<String, Results> resultPair : results.entrySet()) {
			Results r = resultPair.getValue();
			reply.addElement(new XML("criterion")
				.addXMLAttribute("name", resultPair.getKey())
				.addElement(new XML("passed").addElement(Boolean.toString(r.passed)))
				.addElement(new XML("fulfillment").addElement(Integer.toString(r.fulfillment)))
				.addElement(new XML("details").addElement(this.prepareErrorDetails(r.details))));
		}
		return new XMLDocument().addElement(reply).toString();
	}

	/**
	 * Create and return plugin error response XML conform to Assignment Manager specs.
	 * 
	 * Response structure:
	 * @code
	 * <plugin-reply>
	 *		<error>ERROR_DETAILS</error>
	 * </plugin-reply>
	 * @endcode

	 * @param error error details
	 * @return Plugin response XML string.
	 */
	private String makeErrorXml (String error) {
		XMLDocument xml = new XMLDocument()
			.addElement(new XML("plugin-reply")
				.addElement(new XML("error").addElement(this.prepareErrorDetails(error))));
		return xml.toString();
	}

	/**
	 * Add new plugin criterion.
	 * 
	 * @param name criterion name (should be descriptive)
	 * @param criterion criterion instance
	 * @throws PluginCodeException
	 */
	protected final void addCriterion (String name, Criterion criterion) throws PluginCodeException {
		if (this.criteria.get(name) != null) {
			throw new PluginCodeException(new StringBuilder("Cannot add criterion with same name twice (")
					.append(name)
					.append(")")
					.toString());
		}
		this.criteria.put(name, criterion);
	}

	/**
	 * Throw appropriate exception if some mandatory plugin arguments are missing.
	 * 
	 * @param params array of supplied arguments
	 * @param descriptions descriptions of expected arguments
	 * @throws PluginException
	 */
	protected final void requireParams (String[] params, String[] descriptions)
			  throws PluginException {
		if ((descriptions == null) || (descriptions.length == 0)) {
			return;
		}
		if ((params == null) || (params.length < descriptions.length)) {
			StringBuilder message = new StringBuilder("Plugin takes ");
			message.append(Integer.toString(descriptions.length))
				.append(" mandatory arguments: ")
				.append(Utils.join(descriptions, ", "));
			throw new PluginUseException(message.toString());
		}
	}
}
