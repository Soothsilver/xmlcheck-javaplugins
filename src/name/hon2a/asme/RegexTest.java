package name.hon2a.asme;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import name.hon2a.asm.Test;
import name.hon2a.asm.TestException;

/**
 * Abstract test providing support for matching strings to regular expressions.
 *
 * @author %hon2a
 */
public abstract class RegexTest extends Test {

	public static final int MATCHING_ERROR = 0;
	public static final int MATCHING_SUCCESS = -1;

	public RegexTest (Map<String, String> sources, Map<String, String> params, File outputFolder) {
		super(sources, params, outputFolder);
	}

	public RegexTest (Map<String, String> sources, Map<String, String> params) {
		super(sources, params, null);
	}

	public RegexTest (Map<String, String> sources, File outputFolder) {
		super(sources, null, outputFolder);
	}

	public RegexTest (Map<String, String> sources) {
		super(sources, null, null);
	}

	/**
	 * Match string to regular expression.
	 *
	 * @param regex regular expression
	 * @param string string to be matched
	 * @return True if string matches regular expression, false otherwise.
	 * @throws TestException if supplied regular expression is invalid
	 * @see RegexTest::matchFile()
	 */
	protected final boolean match (String regex, String string) throws TestException {
		Pattern pattern;
		try {
			pattern = Pattern.compile(regex, Pattern.MULTILINE);
		} catch (PatternSyntaxException e) {
			this.triggerError("Supplied string is not valid regular expression", ErrorType.USE_ERROR, e);
			return false;
		}
		return pattern.matcher(string).matches();
	}

	protected final int matchLines (String regex, String string) throws TestException {
		Pattern pattern;
		try {
			pattern = Pattern.compile(regex);
		} catch (PatternSyntaxException e) {
			this.triggerError("Supplied string is not valid regular expression", ErrorType.USE_ERROR, e);
			return MATCHING_ERROR;
		}
		BufferedReader reader = new BufferedReader(new StringReader(string));
		String line;
		try {
			int lineNumber = 1;
			while ((line = reader.readLine()) != null) {
				if (!pattern.matcher(line).matches()) {
					return lineNumber;
				}
				++lineNumber;
			}
		} catch (IOException e) {
			this.triggerError(e);
			return MATCHING_ERROR;
		}
		return MATCHING_SUCCESS;
	}

	/**
	 * Match contents of text file to regular expression.
	 *
	 * @param regex regular expression
	 * @param file text file to be matched
	 * @return True if file contents match regular expression, false otherwise.
	 * @throws TestException if supplied regular expression is invalid
	 * @see RegexTest::match()
	 */
	protected final boolean matchFileLines (String regex, File file) throws TestException {
		String fileContents = this.loadTextFile(file);
		return this.match(regex, fileContents);
	}
}
