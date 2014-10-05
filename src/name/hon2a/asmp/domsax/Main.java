package name.hon2a.asmp.domsax;

import name.hon2a.asm.Plugin;
import name.hon2a.asm.PluginException;
import name.hon2a.asm.TesterPlugin;
import name.hon2a.asm.Utils;

/**
 * Assignment Manager plugin for automating DOM-SAX assignment correction.
 *
 * Compiles Java sources provided by @ref student and runs compiled scripts on
 * XML input also provided. Saves output for non-automated part of correction
 * (input is different for every @ref student as are details of his assignment).
 *
 * In DOM-SAX assignment, students are required to write their own Java classes
 * that facilitate DOM transformations / SAX event handling. DOM transformation
 * class needs to be named @c user.MyDomTransformer and implement DomTransformer
 * interface, while SAX event handler needs to be named @c user.MySaxHandler
 * and extend org.xml.sax.helpers.DefaultHandler.
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
        //System.out.println(self.run(new String[] { "D:\\Skydrive\\Projekty Java\\smallTest\\src\\warplan_java7.zip" }));
		System.out.println(self.run(args));
		//System.out.println(self.run(new String[] { "C:\\Users\\hon2a\\Documents\\school\\Java\\zapoctak\\implementace_built\\dataSamples\\domSax_correct_UTF8.zip" }));
	}

	/**
	 * Set up two criteria, one for DOM transformation and one for SAX parsing,
	 * using DomJavaTest and SaxJavaTest respectively.
	 *
	 * @param params unused (this plugin does not take any parameters at this time)
	 * @throws PluginException
	 */
	@Override
	protected void setUp(String[] params) throws PluginException {

		String sourceXmlPath = this.getSourcePath("data.xml");

		this.addTestAsCriterion(new DomJavaTest(
			Utils.createStringMap(
				DomJavaTest.sourceJava, this.getSourcePath("dom"),
				DomJavaTest.sourceXml, sourceXmlPath
			),
			Utils.createStringMap(
				DomJavaTest.paramDomScript, "user.MyDomTransformer",
				DomJavaTest.paramOutputFile, "data.transformed.xml"
			),
			this.getOutputFile(".")
		), "XML DOM transformation using supplied script");

		this.addTestAsCriterion(new SaxJavaTest(
			Utils.createStringMap(
				SaxJavaTest.sourceJava, this.getSourcePath("sax"),
				SaxJavaTest.sourceXml, sourceXmlPath
			),
			Utils.createStringMap(
				SaxJavaTest.paramSaxScript, "user.MySaxHandler",
				SaxJavaTest.paramOutputFile, "sax.output.txt"
			),
			this.getOutputFile(".")
		), "XML SAX parsing using supplied handler");
	}

}
