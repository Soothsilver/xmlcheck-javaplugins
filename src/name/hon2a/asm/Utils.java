package name.hon2a.asm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Wrapper module for various utility functions.
 *
 * @author %hon2a
 */
public class Utils {

	/// OS-specific line separator
	public static final String EOL_STRING = System.getProperty("line.separator");
	public static final String INDENT_STRING = "   "; ///< default indentation string

	private static final int BUFFER_SIZE = 2048; ///< default buffer size

	/**
	 * Create temporary file with given extension.
	 *
	 * @param extension file extension
	 * @return File descriptor of created file.
	 * @throws IOException if file cannot be created
	 * @see Utils::createTempFile()
	 */
	public static File createTempFile (String extension) throws IOException {
		final File tempFile;
		tempFile = File.createTempFile("asmTempFile_" + Long.toString(System.nanoTime()),
				  ((extension == null) ? "" : "." + extension));

		if(!(tempFile.delete()))
		{
			throw new IOException("Could not delete temp file: " + tempFile.getName());
		}
		if(!(tempFile.createNewFile()))
		{
			 throw new IOException("Could not create temp directory: " + tempFile.getName());
		}

		return tempFile;
	}

	/**
	 * Create temporary file.
	 *
	 * @return File descriptor of created file.
	 * @throws IOException if file cannot be created
	 * @see Utils::createTempFile(String)
	 */
	public static File createTempFile () throws IOException {
		return createTempFile(null);
	}

	/**
	 * Create temporary folder.
	 *
	 * @return File descriptor of created folder.
	 * @throws IOException if folder cannot be created
	 */
	public static File createTempDirectory () throws IOException {
		final File tempDir;
		tempDir = File.createTempFile("asmTempFolder_", Long.toString(System.nanoTime()));

		if(!(tempDir.delete()))
		{
			throw new IOException("Could not delete temp file: " + tempDir.getName());
		}
		if(!(tempDir.mkdir()))
		{
			 throw new IOException("Could not create temp directory: " + tempDir.getName());
		}

		return tempDir;
	}

	/**
	 * Load text file to string.
	 *
	 * @param source file descriptor of source file
	 * @return Text contents of source file in a string.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @see Utils::saveTextFile()
	 */
	public static String loadTextFile (File source) throws FileNotFoundException, IOException {
		StringBuilder textContent = new StringBuilder(BUFFER_SIZE);
		FileInputStream fi = new FileInputStream(source);
		UnicodeBOMInputStream ubis = new UnicodeBOMInputStream(fi).skipBOM();
		InputStreamReader ir;
		try {
			ir = new InputStreamReader(ubis, "UTF-8");
		} catch (Exception e) {
			ir = new InputStreamReader(ubis);
		}

		Reader reader = new BufferedReader(ir, BUFFER_SIZE);
		char[] buf = new char[BUFFER_SIZE];
		int numRead = 0;

		try {
			while ((numRead = reader.read(buf)) != -1) {
				textContent.append(String.valueOf(buf, 0, numRead));
			}
		} finally {
			reader.close();
		}

		return textContent.toString();
	}

	/**
	 * Recode file to default system encoding.
	 *
	 * @param source file descriptor of source file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void recodeFileToDefaultEncoding (File source)
			throws FileNotFoundException, IOException {
		String contents = Utils.loadTextFile(source);
		Writer output = new BufferedWriter(new FileWriter(source));
		try {
			output.write(contents);
		} finally {
			output.close();
		}
	}

	/**
	 * Save data from input stream to text file.
	 *
	 * @param dest destination file
	 * @param is input stream
	 * @param charsetName name of charset which should be used (default if null)
	 * @throws IOException
	 * @see Utils::saveBinaryFile()
	 */
	public static void saveTextFile (File dest, InputStream is, String charsetName) throws IOException {
		BufferedWriter writer = null;
		BufferedReader reader = null;
		dest.createNewFile();
		try {
			writer = new BufferedWriter(new FileWriter(dest), BUFFER_SIZE);
			reader = (charsetName == null)
				? new BufferedReader(new InputStreamReader(is), BUFFER_SIZE)
				: new BufferedReader(new InputStreamReader(is, charsetName), BUFFER_SIZE);
			int count = 0;
			char[] charBuffer = new char[BUFFER_SIZE];
			while ((count = reader.read(charBuffer, 0, BUFFER_SIZE)) != -1) {
				writer.write(charBuffer, 0, count);
			}
		} finally {
			if (writer != null) {
				writer.close();
			}
			if (reader != null) {
				reader.close();
			}
		}
	}

	/**
	 * Save data from input stream to binary file.
	 *
	 * @param dest destination file
	 * @param contents input stream
	 * @throws IOException
	 * @see Utils::saveTextFile()
	 */
	public static void saveBinaryFile (File dest, InputStream contents) throws IOException {
		BufferedOutputStream os = null;
		BufferedInputStream is = null;
		dest.createNewFile();
		try {
			is = new BufferedInputStream(contents, BUFFER_SIZE);
			os = new BufferedOutputStream(new FileOutputStream(dest), BUFFER_SIZE);
			int count = 0;
			byte[] byteBuffer = new byte[BUFFER_SIZE];
			while ((count = is.read(byteBuffer, 0, BUFFER_SIZE)) != -1) {
				os.write(byteBuffer, 0, count);
			}
		} finally {
			if (is != null) {
				is.close();
			}
			if (os != null) {
				os.close();
			}
		}
	}

	/**
	 * Indent text.
	 *
	 * @param text text to be indented
	 * @param count indentation depth
	 * @param filler indentation string
	 * @return Indented text.
	 * @see Utils::indent(String, int)
	 * @see Utils::indent(String)
	 */
	public static String indent (String text, int count, String filler) {
		if ((text == null) || (count < 0) || (filler == null)) {
			return null;
		}

		StringBuilder fillerBuilder = new StringBuilder();
		for (int i = 0; i < count; ++i) {
			fillerBuilder.append(filler);
		}
		filler = fillerBuilder.toString();

		StringWriter buffer = new StringWriter();
		BufferedWriter writer = new BufferedWriter(buffer);
		BufferedReader reader = new BufferedReader(new StringReader(text));
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				writer.write(new StringBuilder(filler).append(line).toString());
				writer.newLine();
			}
			reader.close();
			writer.close();
		} catch (IOException e) {
		}

		return buffer.toString();
	}

	/**
	 * Indent text with default indentation string.
	 *
	 * @param text text to be indented
	 * @param count indentation depth
	 * @return Indented text.
	 * @see Utils::indent(String, int, String)
	 * @see Utils::indent(String)
	 */
	public static String indent (String text, int count) {
		return indent(text, count, INDENT_STRING);
	}

	/**
	 * Indent text by one with default indentation string.
	 *
	 * @param text text to be indented
	 * @return Indented text.
	 * @see Utils::indent(String, int, String)
	 * @see Utils::indent(String, int)
	 */
	public static String indent (String text) {
		return indent(text, 1);
	}

	/**
	 * Format error so that error details are indented and appended on next line
	 * after error message.
	 *
	 * @param message error message
	 * @param details error details
	 * @return Formatted error message.
	 * @see Utils::indent()
	 */
	public static String indentError (String message, String details) {
		return (new StringBuilder(message)
				.append(EOL_STRING)
				.append(indent(details))
				.toString());
	}

	/**
	 * Add folder contents to given zip output stream.
	 *
	 * @param sourceFolder source folder
	 * @param zos output stream
	 * @param pathBase path prefix for added entries
	 * @throws IOException
	 */
	private static void zipDirectory (File sourceFolder, ZipOutputStream zos, String pathBase) throws IOException {
		String[] folderContents = sourceFolder.list();
		byte[] byteBuffer = new byte[BUFFER_SIZE];
		int count;
		for (int i = 0; i < folderContents.length; ++i) {
			File file = new File(sourceFolder, folderContents[i]);
			StringBuilder entryBuilder = new StringBuilder(pathBase);
			if (!pathBase.equals("")) {
				entryBuilder.append(File.separator);
			}
			entryBuilder.append(file.getName());
			if (file.isDirectory()) {
				zipDirectory(sourceFolder, zos, entryBuilder.toString());
			} else {
				BufferedInputStream is = null;
				try {
					is = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);
					zos.putNextEntry(new ZipEntry(entryBuilder.toString()));
					while ((count = is.read(byteBuffer)) != -1) {
						zos.write(byteBuffer, 0, count);
					}
				} finally {
					if (is != null) {
						is.close();
					}
				}
			}
		}
	}

	/**
	 * Pack folder contents into single ZIP archive.
	 *
	 * @param sourceFolder source folder
	 * @param archive destination file
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @see Utils::unzip()
	 */
	public static void zip (File sourceFolder, File archive)
			  throws FileNotFoundException, IOException {
		ZipOutputStream zos = null;
		try {
			zos = new ZipOutputStream(new FileOutputStream(archive));
			zipDirectory(sourceFolder, zos, "");
		} finally {
			if (zos != null) {
				zos.close();
			}
		}
	}

	/**
	 * Unpack contents of ZIP archive to given folder.
	 *
	 * @param archive ZIP archive
	 * @param destFolder destination folder
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @see Utils::zip()
	 */
	public static void unzip (File archive, File destFolder)
			  throws FileNotFoundException, IOException  {
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(archive)));
		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null) {
			File destFile = new File(destFolder, entry.getName());
			if (entry.isDirectory()) {
				if (destFile.exists() && !destFile.isDirectory()) {
					destFile.delete();
				}
				destFile.mkdirs();
			} else {
				if (!destFile.getParentFile().isDirectory()) {
					destFile.getParentFile().mkdirs();
				}

				int count;
				byte data[] = new byte[BUFFER_SIZE];
				BufferedOutputStream dest = null;
				try {
					dest = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE);
					while ((count = zis.read(data, 0, BUFFER_SIZE)) != -1) {
						dest.write(data, 0, count);
					}
					dest.flush();
				} finally {
					if (dest != null) {
						dest.close();
					}
				}
			}
		}
		zis.close();
	}

	/**
	 * Remove folder and its contents.
	 *
	 * @param directory folder to be removed
	 * @return True if removal was successful, false otherwise.
	 * @see Utils::removeAnyFile()
	 */
	public static boolean removeDirectoryAndContents (File directory) {
		if (directory == null) {
			return false;
		}
		if (!directory.isDirectory()) {
			return false;
		}

		String[] contents = directory.list();
		boolean done = true;
		if (contents != null) {
			for (int i = 0; i < contents.length; ++i) {
				File entry = new File(directory, contents[i]);
				if (entry.isDirectory()) {
					done = removeDirectoryAndContents(entry);
				} else {
					done = entry.delete();
				}
			}
		}
		if (done) {
			done = directory.delete();
		}
		return done;
	}

	/**
	 * Remove file or folder.
	 *
	 * @param file file descriptor
	 * @return True if removal was successful, false otherwise.
	 * @see Utils::removeDirectoryAndContents()
	 */
	public static boolean removeAnyFile (File file) {
		if (file.isDirectory()) {
			return removeDirectoryAndContents(file);
		} else {
			if (!file.exists()) {
				return false;
			}
			return file.delete();
		}
	}

	/**
	 * Turns object to string (empty string if object is null).
	 *
	 * @param object
	 * @return Empty string for null reference, Object::toString() otherwise.
	 */
	private static String toString (Object object) {
		if (object == null) {
			return "";
		}
		return object.toString();
	}

	/**
	 * Turn collection to single string with elements delimited by custom string.
	 *
	 * @param collection collection to be stringified
	 * @param delimiter custom delimiter
	 * @return String consisting of collection elements delimited by custom string.
	 * @see Utils::join(Object[], String)
	 */
	public static String join (AbstractCollection collection, String delimiter) {
		if (collection == null) {
			return "";
		}
		return join(collection.toArray(), delimiter);
	}

	/**
	 * Turn array to single string with elements delimited by custom string.
	 *
	 * @param array array to be stringified
	 * @param delimiter custom delimiter
	 * @return String consisting of array elements delimited by custom string.
	 * @see Utils::join(AbstractCollection, String)
	 */
	public static String join (Object[] array, String delimiter) {
		if (array.length == 0) {
			return "";
		} else if (array.length == 1) {
			return toString(array[0]);
		}

		StringBuilder builder = new StringBuilder(toString(array[0]));
		for (int i = 1; i < array.length; ++i) {
			builder.append(toString(delimiter))
				.append(toString(array[i]));
		}
		return builder.toString();
	}

	/**
	 * Turn array to single string.
	 *
	 * @param array array to be stringified
	 * @return String consisting of array elements joined together.
	 * @see Utils::join(Object[], String)
	 * @see Utils::join(AbstractCollection, String)
	 */
	public static String join (Object [] array) {
		return join(array, null);
	}

	/**
	 * Split string as if it was command-line arguments string.
	 *
	 * First takes out all quoted substrings and splits the rest by spaces. Quoted
	 * substrings are separate entries even if not surrounded by spaces.
	 *
	 * @param string string to be split
	 * @return Array of string arguments.
	 */
	public static String[] splitArguments (String string) {
		StringBuilder stringWorker = new StringBuilder(string);
		Matcher matcher = Pattern.compile("(^|[^\\\\])\"([^\"\\\\]+(\\\\\"[^\\\\])?)*\"")
				.matcher(stringWorker.toString());
		List<String> strings = new ArrayList<String>();
		int pos = 0;
		while (matcher.find()) {
			int start = matcher.start();
			if (start > pos) {
				addSplitPartsToArray(stringWorker.substring(pos, start + 1), strings);
			}
			pos = matcher.end();

			String group = matcher.group();
			if (group.charAt(0) == '"') {
				strings.add(group.substring(1, group.length() - 1));
			} else {
				strings.add(group.substring(2, group.length() - 1));
			}
		}
		if (stringWorker.length() > pos) {
			addSplitPartsToArray(stringWorker.substring(pos, stringWorker.length()), strings);
		}
		return strings.toArray(new String[] {});
	}

	/**
	 * Split string by spaces and add non-empty parts to list.
	 * 
	 * @param string string to be split
	 * @param list list to receive parts
	 */
	private static void addSplitPartsToArray (String string, List<String> list) {
		String[] parts = string.split(" ");
		for (int i = 0; i < parts.length; ++i) {
			if (!parts[i].equals("")) {
				list.add(parts[i]);
			}
		}
	}

	/**
	 * Create associative array of strings using odd strings as keys and even as values.
	 *
	 * @param entries keys and values combined
	 * @return Associative array with string keys and values.
	 */
	public static Map<String, String> createStringMap (String ... entries) {
		Map<String, String> map = new HashMap<String, String>(entries.length / 2);
		for (int i = 1; i < entries.length; i += 2) {
			map.put(entries[i - 1], entries[i]);
		}
		return map;
	}

	/**
	 * Create associative array of file descriptors using odd strings as keys and
	 * even as paths.
	 *
	 * @param entries keys and file paths combined
	 * @return Associative array with string keys and file descriptor values.
	 */
	public static Map<String, File> createFileMap (String ... entries) {
		Map<String, File> map = new HashMap<String, File>(entries.length / 2);
		for (int i = 1; i < entries.length; i += 2) {
			map.put(entries[i - 1], new File(entries[i]));
		}
		return map;
	}

	/**
	 * Retrieve stack trace of given exception as string.
	 *
	 * @param e exception
	 * @return Stack trace.
	 * @see Utils::getMessageTrace()
	 */
	public static String getStackTrace (Throwable e) {
		StringWriter bufferWriter = new StringWriter();
		e.printStackTrace(new PrintWriter(bufferWriter));
		return bufferWriter.toString();
	}

	/**
	 * Create simple message from exception using its message (or its class name
	 * in case of no message).
	 *
	 * @param e exception
	 * @return Exception message if present, class name otherwise.
	 * @see Utils::getMessageTrace()
	 */
	private static String createSimpleMessage (Throwable e) {
		String message = e.getMessage();
		if (message == null) {
			message = e.getClass().getSimpleName();
		}
		return message;
	}

	/**
	 * Create detailed message from exception using top of its stack trace to add
	 * aditional info.
	 *
	 * @param e exception
	 * @return Detailed message with exception class and possibly file and line number.
	 * @see Utils::getMessageTrace()
	 */
	private static String createDetailedMessage (Throwable e) {
		StringBuilder messageBuilder = new StringBuilder();
		StackTraceElement[] trace = e.getStackTrace();
		messageBuilder.append(e.getClass().getSimpleName());
		if (trace.length != 0) {
			messageBuilder.append(" @ ")
					.append(trace[0].getFileName());
			int lineNumber = trace[0].getLineNumber();
			if (lineNumber > 0) {
				messageBuilder.append(":")
						.append(lineNumber);
			}
		}
		String simpleMessage = e.getMessage();
		if (simpleMessage != null) {
			messageBuilder.insert(0, " (")
					.insert(0, simpleMessage)
					.append(")");
		}
		return messageBuilder.toString();
	}

	/**
	 * Create human-readable message from exception stack.
	 *
	 * Function goes through exception stack using Throwable::getCause() and prints
	 * short info about each exception in the stack.
	 *
	 * @param e exception (top of the stack)
	 * @param detailed set to true to get detailed report instead of just messages
	 * @return Human-readable exception report.
	 */
	public static String getMessageTrace (Throwable e, boolean detailed) {
		StringBuilder traceBuilder = new StringBuilder();
		while (e != null) {
			String message = detailed ? createDetailedMessage(e) : createSimpleMessage(e);
			traceBuilder.append(message)
				.append(EOL_STRING);
			e = e.getCause();
		}
		return traceBuilder.toString();
	}

	/**
	 * Create simple human-readable message from exception stack.
	 *
	 * @param e exception (top of the stack)
	 * @return Human-readable exception report.
	 */
	public static String getMessageTrace (Throwable e) {
		return getMessageTrace(e, false);
	}

	public static String escapeXml (String str) {
		str = str.replace("&", "&amp;");
		str = str.replace("<", "&lt;");
		str = str.replace(">", "&gt;");
		return str;
	}
}
