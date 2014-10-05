package name.hon2a.asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract plugin that separates data processing from result assessment by using
 * @link Test tests @endlink for performing all processing.
 *
 * Class implements and finalizes Plugin::execute() method, leaving only Plugin::setUp()
 * customizable for plugin developers. All data processing is delegated to descendants
 * of Test class and assessed automatically as part of TesterCriterion::check()
 * method.
 *
 * Tests can either be added directly as plugin criteria (in that case all test
 * goals must be reached for criterion to be met) or separately from them. Separating
 * tests and criteria offers oportunity to uncouple task-oriented tests from
 * problem-oriented criteria, hiding task coupling from data-submitters in the
 * process. Only parts of tests can than be used as criteria, combined or divided
 * as necessary.
 *
 * @author %hon2a
 */
public abstract class TesterPlugin extends Plugin {

	/**
	 * Test-oriented implementation of plugin criterion.
	 *
	 * Stores associative array of \<test id, goal name\> pairs and checks all respective
	 * goals upon TesterCriterion::check() call. All goals need to be reached
	 * for criterion to be met.
	 */
	protected final class TesterCriterion extends Criterion {

		private final Map<Integer, String> goalPairs; ///< associative array of \<test id, goal name\> pairs

		/**
		 * Default and only constructor.
		 *
		 * Map of \<test id, goal name\> pairs cannot be altered later.
		 *
		 * @param goalPairs associative array of \<test id, goal name\> pairs
		 */
		public TesterCriterion (Map<Integer, String> goalPairs) {
			this.goalPairs = new HashMap<Integer, String>(goalPairs);
		}

		/**
		 * Check all goals referenced in TestCriterion::goalPairs and return
		 * combined results.
		 *
		 * For criterion to be met, all goals need to be reached. For goal to be
		 * reached, its test must complete successfully. If it doesn't, all goals
		 * of that test are automatically failed, even if not marked as such. Error
		 * details of all failed goals are combined together into single error
		 * details string. Criterion fulfillment percentage is based on number of
		 * reached goals divided by all goals assigned to this criterion.
		 *
		 * @return Criterion status packed into Test.Results wrapper.
		 * @throws PluginException in case of invalid goal reference
		 */
		@Override
		protected Results check() throws PluginException {
			int goalsFailed = 0;
			StringBuilder details = new StringBuilder();

			boolean[] failedTests = new boolean[TesterPlugin.this.tests.size()];

			for (Map.Entry<Integer, String> goalPair : this.goalPairs.entrySet()) {
				int testId = goalPair.getKey();
				if (testId >= TesterPlugin.this.testResults.length) {
					throw new PluginCodeException("Invalid test id passed");
				}

				String goalName = goalPair.getValue();
				Test test = TesterPlugin.this.tests.get(testId);
				Goal goal = test.getResults().get(goalName);
				if (goal == null) {
					throw new PluginCodeException("Invalid goal name passed");
				}

				Error error = test.getError();
				if (error != null) {
					++goalsFailed;
					if (!failedTests[testId]) {
						failedTests[testId] = true;
						details.append(TesterPlugin.this.tests.get(testId).getName())
							.append(" didn't successfully finish")
							.append(Utils.EOL_STRING)
							.append(Utils.indent(error.toString()));
					}
				} else if (!goal.reached) {
					++goalsFailed;
					details.append(goal.name)
						.append(" (")
						.append(TesterPlugin.this.tests.get(testId).getName())
						.append(")")
						.append(" failed")
						.append(Utils.EOL_STRING)
						.append(Utils.indent(goal.error.toString()));
				}
			}
			int goalCount = this.goalPairs.size();
			int fulfillment = (goalCount > 0)
				? ((goalCount - goalsFailed) * 100 / goalCount)
				: 100;
			return new Results((goalsFailed == 0), fulfillment, details.toString());
		}
	}

	private List<Test> tests = new ArrayList<Test>(); ///< list of used tests
	private Map[] testResults; ///< array of results maps of all tests

	/**
	 * Run all tests added to this plugin in TesterPlugin::setUp() method override.
	 *
	 * Tests are run in parallel threads and once finished, their results are saved.
	 * It is recommended to assign exclusive output folders to individual tests
	 * to avoid unwanted test output clashes.
	 *
	 * @throws PluginException in case plugin is interrupted while waiting for tests to finish
	 */
	@Override
	protected final void execute() throws PluginException {
		int testCount = this.tests.size();
		Thread[] threads = new Thread[testCount];
		this.testResults = new HashMap[testCount];

		for (int i = 0; i < testCount; ++i) {
			Test test = this.tests.get(i);
			threads[i] = new Thread(test);
			threads[i].start();
		}
		for (int i = 0; i < threads.length; ++i) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				throw new PluginException("Plugin was interrupted before all tests were finished", e);
			}
			this.testResults[i] = this.tests.get(i).getResults();
		}
	}

	/**
	 * Add test to be used by this plugin.
	 * 
	 * @param test test to be added
	 * @return Test ID to be used to refer test to plugin criteria.
	 * @see TesterPlugin::addTestAsCriterion()
	 */
	protected int addTest (Test test) {
		this.tests.add(test);
		return this.tests.size() - 1;
	}

	/**
	 * Add test to used by plugin and bound to new plugin criterion.
	 *
	 * All of test's goals must be reached for criterion to be met. If no description
	 * is supplied, generic description is created.
	 *
	 * @param test test to be added
	 * @param description criterion description
	 * @return Test ID to be used to refer test to plugin criteria.
	 * @throws PluginCodeException
	 * @see TesterPlugin::addTestAsCriterion(Test)
	 * @see TesterPlugin::addTest()
	 */
	protected int addTestAsCriterion (Test test, String description) throws PluginCodeException {
		int testId = this.addTest(test);
		Map<Integer, String> goals = new HashMap<Integer, String>();
		for (String goalName : test.getResults().keySet()) {
			goals.put(testId, goalName);
		}
		if (description == null) {
			description = (new StringBuilder("Complete test ")
					.append(test.getName())
					.toString());
		}
		this.addCriterion(description, new TesterCriterion(goals));
		return testId;
	}

	/**
	 * Add test to used by plugin and bound to new plugin criterion (without criterion
	 * description).
	 *
	 * @param test test to be added
	 * @return Test ID to be used to refer test to plugin criteria.
	 * @throws PluginCodeException
	 * @see TesterPlugin::addTestAsCriterion(Test, String)
	 * @see TesterPlugin::addTest()
	 */
	protected int addTestAsCriterion (Test test) throws PluginCodeException {
		return this.addTestAsCriterion(test, null);
	}
}
