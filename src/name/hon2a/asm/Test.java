package name.hon2a.asm;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

/**
 * Abstract test used by descendants of TesterPlugin.
 *
 * While plugins reflect problem-oriented view, tests are task-oriented. Single
 * test should perform only tasks that are grouped by common requisites (such
 * as parsing same input or using same resources).
 *
 * Similarly to plugin criteria, tests have @link Goal goals @endlink . While
 * criteria should reflect how well @ref student performed (possibly integrating weights
 * in future), test goals have no such limitation. They only mark individual
 * tasks and their completion.
 *
 * Tests feature errors as alternative to exceptions (convenience shortcut enabling
 * developers to integrate exceptions seamlessly to test flow).
 * @link TestException TestExceptions @endlink should never be thrown directly.
 * Instead developers should use provided Test::triggerError() functions.
 *
 * Every test is single-use-only. Current state of execution is saved in Test::state.
 * Goal-setting and execution stages are exclusive (all goals must be set first
 * and cannot be altered later during test execution).
 *
 * @author %hon2a
 */
public abstract class Test implements Runnable {

	/**
	 * Type of triggered error.
	 */
	protected enum ErrorType {
		CODE_ERROR,	///< developer's error - corresponds to TestCodeException
		USE_ERROR,	///< @ref teacher 's error - corresponds to TestUseException
		DATA_ERROR, ///< invalid data (@ref student 's error) - corresponds to TestDataException
		EXCEPTION,	///< generic value for any exception not descended from TestException
		ILLEGAL_ARGUMENT ///< illegal method argument, special case of EXCEPTION
	}

	private static final int STAGE_INITIAL = 0; ///< initial state on test creation
	private static final int STAGE_SET_GOALS = 1; ///< stage in which goals are set
	private static final int STAGE_DO_TEST = 2; ///< stage in which test body is executed
	private static final int STAGE_CLEANUP = 3; ///< cleanup stage

	private String name; ///< test name (should be descriptive)
	/**
	 * test sources
	 *
	 * Test sources are supplied by plugin and adressed by string id, so that test
	 * doesn't depend on specific file names.
	 */
	private Map<String, String> sources;
	private Map<String, String> params; ///< associative array of test parameters
	private File outputFolder; ///< temporary folder for test output
	
	private int stage = STAGE_INITIAL; ///< stage of execution (see detailed description of Test)
	private boolean suppressExceptions = false; ///< suppress exceptions flag
	private String lastError = null; ///< last error buffer

	private Map<String, Goal> goals; ///< test goals
	private Error error; ///< error in case whole test fails

	/**
	 * Full constructor initializing sources, parameters and output folder.
	 * 
	 * @param sources associative array of test sources
	 * @param params associative array of test parameters
	 * @param outputFolder folder for test output
	 */
	public Test (Map<String, String> sources, Map<String, String> params, File outputFolder) {
		this.init(sources, params, outputFolder);
	}

	/**
	 * Partial constructor for test without output.
	 * 
	 * @param sources associative array of test sources
	 * @param params associative array of test parameters
	 */
	public Test (Map<String, String> sources, Map<String, String> params) {
		this.init(sources, params, null);
	}

	/**
	 * Partial constructor for test without parameters.
	 *
	 * @param sources associative array of test sources
	 * @param outputFolder folder for test output
	 */
	public Test (Map<String, String> sources, File outputFolder) {
		this.init(sources, null, outputFolder);
	}

	/**
	 * Partial constructor for test with no parameters and no output.
	 *
	 * @param sources associative array of test sources
	 */
	public Test (Map<String, String> sources) {
		this.init(sources, null, null);
	}

	/**
	 * Test initialization function called by constructors.
	 *
	 * @param sources associative array of test sources
	 * @param params associative array of test parameters
	 * @param outputFolder folder for test output
	 */
	private void init (Map<String, String> sources, Map<String, String> params, File outputFolder) {
		this.name = this.getClass().getSimpleName();
		if (this.name == null) {
		}
		this.sources = (sources != null) ? sources : new HashMap<String, String>();
		this.params = (params != null) ? params : new HashMap<String, String>();
		this.outputFolder = outputFolder;

		this.goals = new HashMap<String, Goal>();
		this.stage = STAGE_SET_GOALS;
		try {
			this.setGoals();
		} catch (TestException e) {
		}
	}

	/**
	 * Getter for Test::name.
	 *
	 * @return %Test name (descriptive, to be used in reports).
	 */
	public final String getName () {
		return this.name;
	}

	/**
	 * Setter for Test::name.
	 */
	public final void setName (String name) {
		this.name = name;
	}

	/**
	 * Retrieve source descriptor for supplied id.
	 *
	 * @return File descriptor of source.
	 */
	protected final File getSourceFile (String id) {
		return new File(this.getSourcePath(id));
	}

	/**
	 * Retrieve source descriptor for supplied id.
	 *
	 * @return String source path.
	 */
	protected final String getSourcePath (String id) {
		return this.sources.get(id);
	}

	/**
	 * Retrieve parameter for supplied id.
	 *
	 * @return String value of parameter.
	 */
	protected final String getParam (String key) {
		return this.params.get(key);
	}

	/**
	 * Translate relative path to output file descriptor.
	 *
	 * @return File descriptor of output file.
	 */
	protected final File getOutputFile (String path) {
		return new File(this.outputFolder, path);
	}

	/**
	 * Translate relative path absolute path of output file.
	 *
	 * @return Path of output file.
	 */
	protected final String getOutputPath (String path) {
		return this.getOutputFile(path).getAbsolutePath();
	}

	/**
	 * Getter for Test::goals (these are interpreted as test results).
	 *
	 * @return Associative array of this tests' goals.
	 */
	public final Map<String, Goal> getResults () {
		return this.goals;
	}

	/**
	 * Getter for Test::error (if error is not null, then whole test failed)
	 * 
	 * @return Error message string.
	 */
	public final Error getError () {
		return this.error;
	}

	/**
	 * @return True if test has failed, false otherwise.
	 */
	public final boolean hasError () {
		return (this.getError() == null);
	}

	/**
	 * Set test goals.
	 *
	 * Descendants of Test need to override this method and set test goals inside
	 * its body. It is called in Test constructor.
	 *
	 * @throws TestException only if used incorrectly (Test::addGoal() throws
	 * exception only if used outside of this method)
	 */
	protected abstract void setGoals () throws TestException;

	/**
	 * Test body.
	 *
	 * Descendants of Test need to override this method and perform all test tasks
	 * inside its body.
	 *
	 * @throws TestException
	 * @see Test::run()
	 */
	protected abstract void doTest () throws TestException;

	/**
	 * Run test.
	 *
	 * Public test access point. Can be called only once or test fails implicitly.
	 *
	 * @see Test::doTest()
	 */
	public final void run () {
		try {
			if (this.stage != STAGE_SET_GOALS) {
				this.triggerError("Test can be run only once.", ErrorType.USE_ERROR);
			}
			this.stage = STAGE_DO_TEST;
			this.doTest();
			this.activateExceptions();
		} catch (TestException e) {
			this.error = new Error(Utils.getMessageTrace(e));
		} catch (Exception e) {
			this.error = new Error(new StringBuilder("Runtime error")
					  .append(Utils.indent(Utils.getMessageTrace(e, true)))
					  .toString());
		}
		this.stage = STAGE_CLEANUP;
	}

	/**
	 * Start error-driven section (as opposed to exception-driven).

	 * @see Test::activateExceptions()
	 */
	protected final void suppressExceptions () {
		this.suppressExceptions = true;
	}

	/**
	 * Close error-driven section (and start exception-driven flow again).
	 * 
	 * @see Test::suppressExceptions()
	 */
	protected final void activateExceptions () {
		this.suppressExceptions = false;
	}

	/**
	 * Trigger error (use instead of throwing exceptions directly).
	 *
	 * Using this function instead of throwing exceptions provides syntactic sugar
	 * for test developers. It checks suppressExceptions flag and if it is
	 * set, saves error to internal buffer (it can be retrieved by Test::getLastError()).
	 * Otherwise it throws appropriate descendant of TestException. Follows example
	 * (typically, method definition is in plugin library and test developer writes
	 * only second part of the code).
	 *
	 * Old model (method definition and use):
	 * @code
	 * protected final void foo () throws TestException {
	 *		try {
	 *			somethingProblematic();
	 *		} catch (SomeException e) {
	 *			throw new TestException("Custom message.", e);
	 *		}
	 * }
	 * @endcode
	 * @code
	 * String error = null;
	 * try {
	 *		this.foo()
	 * } catch (TestException e) {
	 *		error = Utils.indentError(e.getMessage(), Utils.getMessageTrace(e.getCause()));
	 * }
	 * this.getGoal("someGoal").reachOnNoError(error);
	 * @endcode
	 *
	 * New model (method definition and use):
	 * @code
	 * protected final void foo () throws TestException {
	 *		try {
	 *			somethingProblematic();
	 *		} catch (SomeException e) {
	 *			this.triggerError("Custom message.", e);
	 *			return; // only in case of fatal error, non-fatal errors can be triggered too
	 *		}
	 * }
	 * @endcode
	 * @code
	 * this.supressExceptions(); // only use once to open error-driven section
	 * this.foo();
	 * this.getGoal("someGoal").reachOnNoError(this.getLastError());
	 * this.activateExceptions(); // only use once after whole error-driven section
	 * @endcode
	 *
	 * @param message message describing error in terms of test
	 * @param type error type (see @ref ErrorType)
	 * @param cause exception that caused this error
	 * @throws TestException as error wrapper in case of Test::suppressExceptions set to false
	 * @see Test::suppressExceptions()
	 */
	protected final void triggerError (String message, ErrorType type, Throwable cause)
			  throws TestException {
		if (this.suppressExceptions) {
			this.lastError = Utils.indentError(message, Utils.getMessageTrace(cause));
		} else {
			switch (type) {
				case CODE_ERROR:
					throw new TestCodeException(message, cause);
				case USE_ERROR: case ILLEGAL_ARGUMENT:
					throw new TestUseException(message, cause);
				case DATA_ERROR:
					throw new TestDataException(message, cause);
				case EXCEPTION:
					throw new TestException(message, cause);
			}
		}
	}

	/**
	 * Partial trigger for errors without exception cause.
	 *
	 * @param message message describing error in terms of test
	 * @param type error type (see @ref ErrorType)
	 * @throws TestException as error wrapper in case of Test::suppressExceptions set to false
	 * @see Test::suppressExceptions()
	 * @see Test::triggerError()
	 */
	protected final void triggerError (String message, ErrorType type) throws TestException {
		this.triggerError(message, type, null);
	}

	/**
	 * Partial trigger wrapping other exceptions in custom message.
	 *
	 * @param message message describing error in terms of test
	 * @param cause exception that caused this error
	 * @throws TestException as error wrapper in case of Test::suppressExceptions set to false
	 * @see Test::suppressExceptions()
	 * @see Test::triggerError()
	 */
	protected final void triggerError (String message, Throwable cause) throws TestException {
		this.triggerError(message, ErrorType.EXCEPTION, cause);
	}

	/**
	 * Partial trigger wrapping other exceptions in generic message.
	 *
	 * @param cause exception that caused this error
	 * @throws TestException as error wrapper in case of Test::suppressExceptions set to false
	 * @see Test::suppressExceptions()
	 * @see Test::triggerError()
	 */
	protected final void triggerError (Throwable cause) throws TestException {
		this.triggerError("Runtime error", cause);
	}

	/**
	 *	Retrieve last error and clear buffer.
	 *
	 * Use only after switching to error-driven flow with Test::supressExceptions().
	 *
	 * @return Last error or null in case of no error or Test::suppressExceptions not being set.
	 * @see Test::suppressExceptions()
	 * @see Test::triggerError()
	 */
	protected final String getLastError () {
		String e = this.lastError;
		this.lastError = null;
		return e;
	}

	/**
	 * Add new goal to this test.
	 *
	 * This method can only be called from Test::setGoals(). Goals can only be
	 * added, never removed. They are implicitly marked as failed and need to be
	 * marked as reached using one of methods provided by Goal class.
	 *
	 * @param id goal ID. Goals need to be retrieved later by passing this ID to
	 *		Test::getGoal()
	 * @param description goal description to be used in reports
	 * @throws TestException in case this method is called outside of Test::setGoals()
	 * @see Test::getGoal()
	 */
	protected final void addGoal (String id, String description) throws TestException {
		if (this.stage != STAGE_SET_GOALS) {
			this.triggerError("Goals cannot be added outside setGoals() method", ErrorType.CODE_ERROR);
			return;
		}
		this.goals.put(id, new Goal(description));
	}

	/**
	 * Get goal by ID.
	 *
	 * @param id ID of goal to be retrieved
	 * @return Goal if ID exists, or null.
	 * @see Test::addGoal()
	 */
	protected final Goal getGoal (String id) {
		return this.goals.get(id);
	}

	/**
	 * Trigger error if one of required test sources hasn't been supplied.
	 *
	 * Convenience method for test developers.
	 *
	 * @param sources IDs of required sources
	 * @throws TestException in case one or more IDs don't exist or if associated
	 *		file descriptor is invalid
	 */
	protected final void requireSources (String ... sources) throws TestException {
		for (String id : sources) {
			String path = this.sources.get(id);
			if (path == null) {
				this.triggerError("Source '" + id + "' has not been specified", ErrorType.USE_ERROR);
				return;
			}
			File file = new File(path);
			if (!file.canRead()) {
				this.triggerError("File/folder " + file.getName()
						  + " doesn't exist or cannot be opened", ErrorType.DATA_ERROR);
				return;
			}
		}
	}

	/**
	 * Trigger error if one of required test parameters hasn't been supplied.
	 *
	 * Convenience method for test developers.
	 *
	 * @param params keys of required parameters
	 * @throws TestException in case one or more IDs don't exist
	 */
	protected final void requireParams (String ... params) throws TestException {
		for (String id : params) {
			if (!this.params.containsKey(id)) {
				this.triggerError("Parameter '" + id + "' has not been specified", ErrorType.USE_ERROR);
			}
		}
	}

	/**
	 * Load text file to string.
	 *
	 * @param source file descriptor of source to be loaded
	 * @return Text contents of file in a string.
	 * @throws TestException with generic message if file could not be loaded
	 */
	protected final String loadTextFile (File source) throws TestException {
		try {
			return Utils.loadTextFile(source);
		} catch (Exception e) {
			this.triggerError("Cannot load file", e);
			return null;
		}
	}

	/**
	 * Load and parse XML file.
	 *
	 * @param source file descriptor of source to be loaded
	 * @param validate whether to validate XML on parsing
	 * @return Document contents of file as a XML Document.
	 * @throws TestException with generic message if file could not be loaded
	 */
	protected final Document loadXmlFile (File source, boolean validate) throws TestException {
		String xmlString = this.loadTextFile(source);

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(validate);
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			this.triggerError("XML parser cannot be initialized", e);
			return null;
		}

		Document xmlDocument;
		try {
			xmlDocument = builder.parse(new ByteArrayInputStream(xmlString.getBytes()), source.getAbsolutePath());
		} catch (Exception e) {
			this.triggerError("XML cannot be parsed", e);
			return null;
		}

		return xmlDocument;
	}

	/**
	 * Load and parse XML file without validation.
	 *
	 * @param source file descriptor of source to be loaded
	 * @return Document contents of file as a XML Document.
	 * @throws TestException with generic message if file could not be loaded
	 */
	protected final Document loadXmlFile (File source) throws TestException {
		return this.loadXmlFile(source, false);
	}

	/**
	 * Save data from input stream to output file with supplied relative path.
	 * 
	 * @param path path relative to base output folder
	 * @param contents data to be saved
	 * @param binary true if file is to be saved as binary (otherwise it's saved as text)
	 * @param charsetName name of charset for saving as text
	 * @throws TestException in case file could not be saved
	 * @see Test::saveTextFile()
	 * @see Test::saveBinaryFile()
	 */
	private final void saveFile (String path, InputStream contents, boolean binary,
			  String charsetName) throws TestException{
		if ((path == null) || (path.equals(""))) {
			this.triggerError("Cannot save file (empty path)", ErrorType.ILLEGAL_ARGUMENT);
		}
		if (this.outputFolder == null) {
			this.triggerError("Cannot save file (output folder is not set)", ErrorType.CODE_ERROR);
			return;
		}

		try {
			if (binary) {
				Utils.saveBinaryFile(this.getOutputFile(path), contents);
			} else {
				Utils.saveTextFile(this.getOutputFile(path), contents, charsetName);
			}
		} catch (IOException e) {
			this.triggerError("Cannot save file (" + path + ")", e);
		}
	}

	/**
	 * Save data from input stream as text file in specified encoding.
	 * 
	 * @param path path relative to base output folder
	 * @param contents data to be saved
	 * @param charsetName name of charset for saving as text
	 * @throws TestException in case file could not be saved
	 * @see Test::saveBinaryFile()
	 */
	protected final void saveTextFile (String path, InputStream contents,
			  String charsetName) throws TestException {
		this.saveFile(path, contents, false, charsetName);
	}

	/**
	 * Save data from input stream as text file in default encoding.
	 *
	 * @param path path relative to base output folder
	 * @param contents data to be saved
	 * @throws TestException in case file could not be saved
	 * @see Test::saveTextFile(String, InputStream, String)
	 * @see Test::saveBinaryFile()
	 */
	protected final void saveTextFile (String path, InputStream contents)
			  throws TestException {
		this.saveTextFile(path, contents, null);
	}

	/**
	 * Save data from input stream in binary file.
	 *
	 * @param path path relative to base output folder
	 * @param contents data to be saved
	 * @throws TestException in case file could not be saved
	 * @see Test::saveTextFile()
	 */
	protected final void saveBinaryFile (String path, InputStream contents)
			  throws TestException {
		this.saveFile(path, contents, true, null);
	}
}