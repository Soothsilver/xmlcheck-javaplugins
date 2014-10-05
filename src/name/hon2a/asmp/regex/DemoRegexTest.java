package name.hon2a.asmp.regex;

import java.io.File;
import java.util.Map;
import name.hon2a.asm.TestException;
import name.hon2a.asme.RegexTest;

/**
 * Test used for checking whether file contents match supplied regular expressions.
 *
 * Developers using this test need to provide it with specific sets of parameters
 * as well as source with ID RegexTest::regexSourceId. Parameters required for
 * every regex are _regex_NUM and _regex_NUM_description_, where _regex_ stands
 * for value of RegexTest::regexParamPrefix and _description_ for
 * RegexTest::regexDescriptionSuffix.
 *
 * Goals are similarly named _prefix_NUM, where _prefix_ is RegexTest::regexGoalPrefix.
 *
 * @author %hon2a
 */
public class DemoRegexTest extends RegexTest {

	public static final String regexParamPrefix = "regex"; ///< regex parameter prefix
	public static final String regexGoalPrefix = "matchRegex"; ///< regex goal prefix
	public static final String regexDescriptionSuffix = "description"; ///< regex descrption parameter suffix
	public static final String regexSourceId = "regexSource"; ///< source ID

	protected int regexGoalCount; ///< number of regular expression parameters supplied

	/**
	 * Required source: RegexTest::regexSourceId, required parameters:
	 * see detailed description of RegexTest .
	 */
	public DemoRegexTest (Map<String, String> sources, Map<String, String> params, File outputFolder) {
		super(sources, params, outputFolder);
	}

	/**
	 * Required source: RegexTest::regexSourceId, required parameters:
	 * see detailed description of RegexTest .
	 */
	public DemoRegexTest (Map<String, String> sources, Map<String, String> params) {
		super(sources, params, null);
	}

	/**
	 * Set a goal for each supplied regular expression.
	 *
	 * Goal description can be also supplied as test parameter (see detailed
	 * description of RegexTest ). Otherwise a generic description is created.
	 * Goal is reached if whole contents of input file match supplied regular
	 * expression.
	 *
	 * @throws TestException
	 */
	@Override
	protected void setGoals () throws TestException {
		String regex;
		String regexLabel;
		while (true) {
			String regexIndex = Integer.toString(this.regexGoalCount++);
			StringBuilder builder = new StringBuilder(regexParamPrefix);
			builder.append(regexIndex);
			regex = this.getParam(builder.toString());
			if (regex == null) {
				break;
			}
			builder.append(regexDescriptionSuffix);
			regexLabel = this.getParam(builder.toString());
			String description = (regexLabel != null) ? regexLabel
				: "Match " + regex;
			this.addGoal(regexGoalPrefix + regexIndex, description);
		}
		this.regexGoalCount--;
	}

	/**
	 * Check whether supplied input file matches all supplied regular expressions.
	 *
	 * @throws TestException
	 */
	@Override
	protected void doTest () throws TestException {
		this.requireSources(regexSourceId);

		String sourceText = this.loadTextFile(this.getSourceFile(regexSourceId));

		for (int i = 0; i < this.regexGoalCount; ++i) {
			int lineNumber = this.matchLines(this.getParam(DemoRegexTest.regexParamPrefix + i), sourceText);
			this.getGoal(regexGoalPrefix + i).reachOnCondition((lineNumber == MATCHING_SUCCESS),
					"Source file contents do not match pattern",
					this.getSourceFile(regexSourceId).getName(), lineNumber);
		}
	}
	
}
