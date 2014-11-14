package name.hon2a.asmp.domsax;

import name.hon2a.asm.TestCodeException;
import name.hon2a.asm.TestDataException;
import name.hon2a.asm.TestException;
import name.hon2a.asme.JavaTest;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.Map;

/**
 * @author hon2a
 */
public class SaxJavaTest extends JavaTest {

	public static final String sourceJava = "javaFiles"; ///< source ID of Java files
	public static final String sourceXml = "xmlDocument"; ///< source ID of xml document
	/// param ID of main class of user script
	public static final String paramSaxScript = "userClass";
	public static final String paramOutputFile = "outputFile"; ///< param ID of output file path
	public static final String goalParseXmlUsingUserHandler = "parse"; ///< goal ID of parse xml goal

	/// name of main method (access point) of user DOM script
	protected static final String domScriptMainMethod = "transform";

	/**
	 * Required source: SaxJavaTest::sourceJava, SaxJavaTest::sourceXml;
	 * required parameters: SaxJavaTest::paramSaxScript, SaxJavaTest::paramOutputFile.
	 */
	public SaxJavaTest (Map<String, String> sources, Map<String, String> params, File outputFolder) {
		super(sources, params, outputFolder);
	}

	/**
	 * Set goals of this test.
	 *
	 * This test is linear - every next step needs the previous one successfully
	 * completed. At the same time, without the last step, there are no meaningful
	 * results. Therefore this test has only one goal.
	 *
	 * @throws TestException
	 */
	@Override
	protected void setGoals () throws TestException {
		this.addGoal(SaxJavaTest.goalParseXmlUsingUserHandler, "SAX parsing of XML using supplied content handler");
	}

	/**
	 * Execute test body.
	 *
	 * This test tries to parse XML file using SAX parser with user-supplied handler,
	 * and save output to file. User-supplied handler needs to extend
	 * org.xml.sax.helpers.DefaultHandler class.
	 *
	 * @throws TestException
	 */
	@Override
	protected void doTest () throws TestException {
		this.requireSources(SaxJavaTest.sourceJava, SaxJavaTest.sourceXml);
		this.requireParams(SaxJavaTest.paramSaxScript, SaxJavaTest.paramOutputFile);
		PrintStream systemOutStream = System.out;
		PrintStream systemErrStream = System.err;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		System.setOut(new PrintStream(baos));
		System.setErr(new PrintStream(new NullOutputStream()));

		File sourcePath = this.getSourceFile(SaxJavaTest.sourceJava);
		this.compileJavaSources(sourcePath);

		DefaultHandler userHandler;
		Object obj = this.loadJavaSource(sourcePath, this.getParam(SaxJavaTest.paramSaxScript));
		try {
			userHandler = (DefaultHandler) obj;
		} catch (ClassCastException e) {
			throw new TestDataException("User handler does not extend org.xml.sax.helpers.DefaultHandler.", e);
		}

		SAXParser saxParser;
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		try {
			saxParser = factory.newSAXParser();
		} catch (Exception e) {
			throw new TestCodeException("Cannot create SAX parser", e);
		}

		File inputFile = this.getSourceFile(SaxJavaTest.sourceXml);
		String xmlInputString = this.loadTextFile(inputFile);


		try {
			saxParser.parse(new ByteArrayInputStream(xmlInputString.getBytes()), userHandler,
					inputFile.getAbsolutePath());
		} catch (SAXException e) {
			throw new TestDataException("Cannot parse xml using supplied handler", e);
		} catch (IOException e) {
			throw new TestCodeException("Error while reading input", e);
		} finally {
			System.setOut(systemOutStream);
            System.setErr(systemErrStream);
		}

		this.saveTextFile(this.getParam(SaxJavaTest.paramOutputFile),
				new ByteArrayInputStream(baos.toByteArray()));

		this.getGoal(SaxJavaTest.goalParseXmlUsingUserHandler).reach();
	}

}
