package name.hon2a.asmp.listing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.util.Map;
import name.hon2a.asm.Test;
import name.hon2a.asm.TestException;
import name.hon2a.asm.Utils;

/**
 * Example test for demonstration purposes.
 *
 * Checks whether input folder (ListingTest::baseDirSourceId) contains at least
 * one file or folder and outputs recursive directory listing to file provided
 * in ListingTest::outputFileParamId parameter.
 *
 * @author %hon2a
 */
public class ListingTest extends Test {

	public static final String baseDirSourceId = "baseDir"; ///< ID of required source (input folder)
	public static final String outputFileParamId = "listingFile"; ///< ID of required parameter (output file)
	public static final String demoGoalId = "someInputExists"; ///< ID of demo goal of this test

	/**
	 * Required sources: ListingTest::baseDirSourceId, required parameters:
	 * ListingTest::outputFileParamId .
	 */
	public ListingTest (Map<String, String> sources, Map<String, String> params, File outputFolder) {
		super(sources, params, outputFolder);
	}

	/**
	 * Set demo goal (input folder must contain something).
	 *
	 * @throws TestException
	 */
	@Override
	protected void setGoals() throws TestException {
		this.addGoal(ListingTest.demoGoalId, "Include something to list");
	}

	/**
	 * Execute test (check whether input folder contains something and output its
	 * listing to output file).
	 *
	 * @throws TestException
	 */
	@Override
	protected void doTest() throws TestException {
		this.requireSources(ListingTest.baseDirSourceId);
		this.requireParams(ListingTest.outputFileParamId);

		String listing = this.createFolderListing(this.getSourceFile(ListingTest.baseDirSourceId)).trim();
		InputStream is = new ByteArrayInputStream(listing.getBytes());
		this.saveTextFile(this.getParam(ListingTest.outputFileParamId), is);

		this.getGoal(ListingTest.demoGoalId)
			.reachOnCondition(((listing != null) && (!listing.equals(""))), "No files/folders found");
	}

	/**
	 * Create recursive directory listing.
	 *
	 * @param folder base directory
	 * @return Returns formatted directory listing in a string.
	 */
	protected String createFolderListing (File folder) {
		if ((folder == null) || (!folder.isDirectory())) {
			return "";
		}

		File[] subfolders = folder.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory();
			}
		});
		File[] files = folder.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return file.isFile();
			}
		});

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < subfolders.length; ++i) {
			builder.append(subfolders[i].getName())
				.append(File.separator)
				.append(Utils.EOL_STRING);
			StringBuilder subbuilder = new StringBuilder()
				.append(this.createFolderListing(subfolders[i]));
			builder.append(Utils.indent(subbuilder.toString(), 1, "  "));
		}
		for (int i = 0; i < files.length; ++i) {
			builder.append(files[i].getName())
				.append(Utils.EOL_STRING);
		}

		return builder.toString();
	}
}
