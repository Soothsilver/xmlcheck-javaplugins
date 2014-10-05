/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.hon2a.asm;

/**
 *
 * @author hon2a
 */
public abstract class SingleTestPlugin extends Plugin {

	protected final class TestGoalCriterion extends Criterion {

		private String goalName;

		/**
		 * Default and only constructor.
		 *
		 * @param goalName goal name (unique identifier within test)
		 */
		public TestGoalCriterion (String goalName) {
			this.goalName = goalName;
		}

		/**
		 * Check the goal referenced by goal name and returns adequate result.
		 *
		 * Criterion is met when the goal is reached and vice versa.
		 *
		 * @return Criterion status packed into Test.Results wrapper.
		 * @throws PluginException in case of invalid goal name or in case of test error
		 */
		@Override
		protected Results check() throws PluginException {
			Test test = SingleTestPlugin.this.test;
			if (test == null) {
				throw new PluginCodeException("No test is set to be executed");
			}

			Error error = test.getError();
			if (error != null) {
				throw new PluginException(error.toString());
			}

			Goal goal = test.getResults().get(this.goalName);
			if (goal == null) {
				throw new PluginCodeException("Invalid goal name passed");
			}

			return new Results(goal.reached, goal.reached ? 100 : 0,
					goal.reached ? "" : goal.error.toString());
		}
	}

	private Test test = null; ///< used test

	/**
	 * Run the single test.
	 *
	 * @throws PluginException in case no test is set to be executed
	 */
	@Override
	protected final void execute() throws PluginCodeException {
		if (this.test == null) {
			throw new PluginCodeException("No test is set to be executed");
		}

		this.test.run();
	}

	/**
	 * Set test to be executed by this plugin.
	 *
	 * @param test test to be set
	 */
	protected void setTest (Test test) throws PluginCodeException {
		this.test = test;
		for (String goalName : this.test.getResults().keySet()) {
			this.addCriterion(goalName, new TestGoalCriterion(goalName));
		}
	}
}
