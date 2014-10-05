package name.hon2a.asmp.listing;

import name.hon2a.asm.Plugin;
import name.hon2a.asm.PluginException;
import name.hon2a.asm.TesterPlugin;
import name.hon2a.asm.Utils;

/**
 * Wrapper plugin for ListingTest.
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
	 * Set up single listing criterion wrapping ListingTest.
	 * 
	 * Test should check whether or not @ref student supplied any input and print
	 * input listing in listing.txt file.
	 *
	 * @param args first member is used as filename of output file (optional)
	 * @throws PluginException
	 */
	@Override
	protected void setUp(String[] args) throws PluginException {
		String outputFileName = (args.length > 0) ? args[0] : "listing.txt";
		this.addTestAsCriterion(new ListingTest(
				  Utils.createStringMap(ListingTest.baseDirSourceId, this.getSourcePath(".")),
				  Utils.createStringMap(ListingTest.outputFileParamId, outputFileName),
				  this.getOutputFile(".")));
	}

}
