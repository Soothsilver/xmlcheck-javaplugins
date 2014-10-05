package name.hon2a.asmpt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Wrapper module for various utility functions.
 *
 * @author hon2a
 */
public class Utils {

	private static final int BUFFER_SIZE = 2048;

	/**
	 * Cut beginning of absolute path to only show volume label set number
	 * of last folders.
	 *
	 * @param path path to be shortened
	 * @param folderCount number of parent folders to show
	 * @return Shortened path.
	 */
	public static String makeReadablePath (String path, int folderCount) {
		String[] parts = path.split(Pattern.quote(File.separator));
		int partCount = folderCount + 2;
		if (parts.length < partCount) {
			return path;
		}
		StringBuilder shortPath = new StringBuilder(parts[0]);
		int startFrom = (parts.length - partCount > 0) ? parts.length - partCount : 1;
		parts[startFrom] = "...";
		for (int i = startFrom; i < parts.length; ++i) {
			shortPath.append(File.separator);
			shortPath.append(parts[i]);
		}
		return shortPath.toString();
	}

	/**
	 * Copy file to new location.
	 *
	 * @param sourceFile descriptor of file to copy
	 * @param destFile descriptor of copy destination
	 * @throws IOException
	 */
	public static void copyFile(File sourceFile, File destFile) throws IOException {
		if(!destFile.exists()) {
			destFile.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;
		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		}
		finally {
			if(source != null) {
				source.close();
			}
			if(destination != null) {
				destination.close();
			}
		}
	}

	/**
	 * Run shell commnd and return result.
	 *
	 * @param command command to be run
	 * @param directory base directory from it should be run
	 * @return Result in a string.
	 * @throws IOException
	 */
	public static String runShellCommand (String command, File directory) throws IOException {
		StringBuilder response = new StringBuilder();
		Process proc = Runtime.getRuntime().exec(command.toString(), null, directory);
		InputStream is = proc.getInputStream();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is), BUFFER_SIZE);
			char[] data = new char[BUFFER_SIZE];
			int charsRead = 0;
			while ((charsRead = reader.read(data, 0, BUFFER_SIZE)) != -1) {
				response.append(data, 0, charsRead);
			}
		} finally {
			is.close();
		}
		return response.toString();
	}

	/**
	 * Parse XML string and return DOM document.
	 *
	 * @param xmlString string to be parsed
	 * @return DOM document parsed from string.
	 * @throws IOException in case of internal error
	 * @throws ParserConfigurationException in case of parser misconfiguration
	 * @throws SAXException in case of parser error
	 */
	public static Document parseXmlString (String xmlString)
			  throws IOException, ParserConfigurationException, SAXException {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		InputSource is = new InputSource();
		is.setCharacterStream(new StringReader(xmlString));
		return builder.parse(is);
	}

	/**
	 * Get children of element of DOM node.
	 *
	 * @param node node to get children of
	 * @return List of children (child elements), or null if node is not element.
	 */
	public static List<Node> getChildElements (Node node) {
		if (node.getNodeType() != Node.ELEMENT_NODE) {
			return null;
		}
		NodeList children = node.getChildNodes();
		List<Node> childElements = new ArrayList<Node>(children.getLength());
		for (int i = 0; i < children.getLength(); ++i) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				childElements.add(child);
			}
		}
		return childElements;
	}
}
