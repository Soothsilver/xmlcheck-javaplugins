package name.hon2a.asmp.regex;

import name.hon2a.asm.Plugin;
import name.hon2a.asm.PluginException;
import name.hon2a.asm.TesterPlugin;
import name.hon2a.asm.Utils;

/**
 * Regular expression matching plugin for demonstration purposes.
 *
 * Sets up single criterion dependent on completion of RegexTest, that is
 * configured to be successful only if file "demo.txt" provided by @ref student
 * is formatted like Ant build file.
 *
 * @author hon2a
 */
public class Main extends TesterPlugin {

	/**
	 * Run plugin.
	 *
	 * @param args command line arguments
	 */
	public static void main (String[] args) {
		Plugin self = new Main();
		System.out.println(self.run(args));
	}

	/**
	 * Set up single test-criterion, that is met only if input file "demo.txt"
	 * is formatted similarly to Ant build file.
	 *
	 * @param params unused
	 * @throws PluginException
	 */
	@Override
	protected void setUp(String[] params) throws PluginException {
		String regexId = DemoRegexTest.regexParamPrefix + 0;
		this.addTestAsCriterion(new DemoRegexTest(
			Utils.createStringMap(DemoRegexTest.regexSourceId, this.getSourcePath("demo.txt")),
			Utils.createStringMap(
				regexId, "(^[a-zA-Z.]+:.+$)|(^#.*$)|(^$)",
				regexId + DemoRegexTest.regexDescriptionSuffix, "Match sample regex"
		)));
	}

}
