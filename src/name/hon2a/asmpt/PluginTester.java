package name.hon2a.asmpt;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * ASM Plugin Tester application.
 *
 * Application consists of single window with ASM Plugin Tester GUI. Window is
 * separated into two parts. Upper part is the form for plugin & data selection
 * and optionally command-line arguments to be passed to plugin. Lower part
 * containts results of plugin run test.
 *
 * @author hon2a
 */
public class PluginTester {

	/**
	 * Plugin status after running.
	 */
	private static enum PluginStatus {
		PASSED, ///< all criteria were met
		FAILED, ///< some criteria were not met
		ERROR ///< plugin run didn't finish successfully
	}

	/**
	 * Wrapper for parsed plugin response.
	 */
	private static class PluginResponse {
		public PluginStatus status = PluginStatus.FAILED; ///< status
		public int fulfillment = 0; ///< fulfillment
		public String details = ""; ///< details in case of status == FAILED or ERROR
	}

	/**
	 * @name GUI defaults and shared resources
	 */
	/*@{*/
	private static final Dimension DEFAULT_SIZE = new Dimension(600, 400); ///< default window size
	private static final Dimension MIN_SIZE = new Dimension(400, 300); ///< minimum window size

	private static final	int fontSize = 12; ///< default font size
	/// sans serif font
	private static final Font plainSansSerifFont = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
	/// bold sans serif font
	private static final Font boldSansSerifFont = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
	/// monospace font
	private static final Font plainMonospaceFont = new Font(Font.MONOSPACED, Font.PLAIN, fontSize);

	private static final Color defaultColor = Color.BLACK; ///< default text color
	private static final Color errorColor = Color.RED; ///< error text color
	private static final Color passedColor = new Color(0, 85, 0); ///< passed status text color
	private static final Color failedColor = new Color(85, 0, 0); ///< failed status text color
	/*@}*/

	/**
	 * @name GUI elements
	 */
	/*@{*/
	private static JFrame frame; ///< main window
	private static JFileChooser pluginChooser; ///< plugin file chooser
	private static JFileChooser dataChooser; ///< data file chooser
	private static JFileChooser saveChooser; ///< save output file chooser
	private static JLabel pluginPathLabel; ///< path of selected plugin
	private static JLabel dataPathLabel; ///< path of selected data file
	private static JLabel statusTextLabel; ///< plugin response status text
	private static JLabel fulfillmentTextLabel; ///< plugin response fulfillment text
	private static JTextField argumentsField; ///< command-line arguments for plugin
	private static JTextArea detailsTextArea; ///< plugin response details text
	private static JButton runButton; ///< runs plugin
	private static JButton saveOutputButton; ///< saves output
	/*@}*/

	/**
	 * @name Other internal variables
	 */
	/*@{*/
	private static File pluginFile; ///< selected plugin
	private static File dataFile; ///< selected data
	private static PluginResponse pluginResponse; ///< parsed plugin response
	private static File pluginOutput; ///< plugin output file
	/*@}*/

	/**
	 * Report error to user.
	 *
	 * @param message error message to show
	 */
	private static void reportError (String message) {
		JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Report error to user based on caught exception.
	 *
	 * @param message error message
	 * @param cause exception
	 */
	private static void reportError (String message, Throwable cause) {
		reportError(message + "\n" + cause.getMessage());
	}

	/**
	 * Close application.
	 */
	private static void exitProgram () {
		frame.dispose();
	}

	/**
	 * Let user select plugin for testing.
	 */
	private static void loadPlugin () {
		if (pluginChooser == null) {
			pluginChooser = new JFileChooser();
			pluginChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			pluginChooser.setFileFilter(new FileNameExtensionFilter("Java archive", "jar"));
			pluginChooser.setAcceptAllFileFilterUsed(false);
		}

		int selectedOption = pluginChooser.showDialog(frame, "Load");
		if (selectedOption == JFileChooser.APPROVE_OPTION) {
			pluginFile = pluginChooser.getSelectedFile();
			pluginPathLabel.setText(Utils.makeReadablePath(pluginFile.getAbsolutePath(), 2));
			enDisRunButton();
		}
	}

	/**
	 * Let user select data for testing.
	 */
	private static void loadData () {
		if (dataChooser == null) {
			dataChooser = new JFileChooser();
			dataChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			dataChooser.setFileFilter(new FileNameExtensionFilter("Zip archive", "zip"));
			dataChooser.setAcceptAllFileFilterUsed(false);
		}

		int selectedOption = dataChooser.showDialog(frame, "Load");
		if (selectedOption == JFileChooser.APPROVE_OPTION) {
			dataFile = dataChooser.getSelectedFile();
			dataPathLabel.setText(Utils.makeReadablePath(dataFile.getAbsolutePath(), 2));
			enDisRunButton();
		}
	}

	/**
	 * Let user select location and copy plugin output file there.
	 */
	private static void saveOutput () {
		if (saveChooser == null) {
			saveChooser = new JFileChooser();
			saveChooser.setFileFilter(new FileNameExtensionFilter("Zip archive", "zip"));
			saveChooser.setAcceptAllFileFilterUsed(false);
		}

		File selectedFile = new File("output.zip");
		saveChooser.setSelectedFile(selectedFile);
		int selectedOption = saveChooser.showSaveDialog(frame);
		if (selectedOption == JFileChooser.APPROVE_OPTION) {
			selectedFile = saveChooser.getSelectedFile();
			try {
				Utils.copyFile(pluginOutput, selectedFile);
			} catch (IOException e) {
				reportError("Cannot save output file.", e);
			}
		}
	}

	/**
	 * Relinquish used resources.
	 */
	private static void cleanUp () {
		deletePluginOutput();
	}

	/**
	 * Delete plugin output file if it exists.
	 */
	private static void deletePluginOutput () {
		if (pluginOutput != null) {
			if (!pluginOutput.delete()) {
				reportError("Could not remove plugin output file " + pluginOutput.getAbsolutePath());
			}
		}
	}

	/**
	 * Enable/disable "Run" [plugin] button.
	 */
	private static void enDisRunButton () {
		runButton.setEnabled((pluginFile != null) && (dataFile != null));
	}

	/**
	 * Display plugin response contained in PluginTester::pluginResponse.
	 */
	private static void displayPluginResponse () {
		String status = null,
				 fulfillment = null,
				 details = null;
		Color statusColor = defaultColor;
		if ((pluginResponse != null)) {
			status = pluginResponse.status.toString();
			details = pluginResponse.details;
			switch (pluginResponse.status) {
				case ERROR:
					statusColor = errorColor;
					break;
				case PASSED:
					statusColor = passedColor;
					fulfillment = Integer.toString(pluginResponse.fulfillment) + " %";
					break;
				case FAILED:
					statusColor = failedColor;
					fulfillment = Integer.toString(pluginResponse.fulfillment) + " %";
					break;
			}
			if (pluginResponse.status != PluginStatus.ERROR) {
				
			}
		}
		statusTextLabel.setForeground(statusColor);
		statusTextLabel.setText(status);
		fulfillmentTextLabel.setText(fulfillment);
		detailsTextArea.setText(details);

		if (pluginOutput != null) {
			saveOutputButton.setEnabled(true);
			saveOutputButton.setVisible(true);
		}
	}

	/**
	 * Parse plugin response.
	 *
	 * @param responseString response string received from plugin
	 * @return Parsed response, or null if it could not be parsed.
	 */
	private static PluginResponse parsePluginResponse (String responseString) {
		Document xml;
		try {
			xml = Utils.parseXmlString(responseString);
		} catch (Exception e) {
			reportError("Plugin response could not be parsed.", e);
			return null;
		}

		int criteria = 0;
		int passedCriteria = 0;
		StringBuilder errors = new StringBuilder();
		boolean passed = true;
		boolean error = false;

		deletePluginOutput();
		pluginOutput = null;

		List<Node> nodes = Utils.getChildElements(xml.getElementsByTagName("plugin-reply").item(0));
		for (Node node : nodes) {
			String nodeName = node.getNodeName();
			if (nodeName.equals("output")) {
				List<Node> files = Utils.getChildElements(node);
				Node file = files.get(0);
				pluginOutput = new File(file.getTextContent());
			} else if (nodeName.equals("criterion")) {
				++criteria;
				List<Node> details = Utils.getChildElements(node);
				for (Node detail : details) {
					String detailName = detail.getNodeName();
					if (detailName.equals("passed")) {
						if (Boolean.parseBoolean(detail.getTextContent())) {
							++passedCriteria;
							break;
						} else {
							passed = false;
						}
					} else if (detailName.equals("details")) {
						errors.append(detail.getTextContent())
							.append('\n');
					}
				}
			} else if (nodeName.equals("error")) {
				error = true;
				errors.append(node.getTextContent());
			}
		}

		PluginResponse ret = new PluginResponse();
		if (error) {
			ret.status = PluginStatus.ERROR;
			ret.details = errors.toString();
		} else {
			ret.status = passed ? PluginStatus.PASSED : PluginStatus.FAILED;
			if (criteria == 0) {
				ret.fulfillment = 100;
				ret.details = "Plugin does not have any criteria.";
			} else {
				ret.fulfillment = (passedCriteria * 100 / criteria);
				ret.details = errors.toString();
			}
		}
		return ret;
	}

	/**
	 * Run selected plugin on selected data and display result.
	 */
	private static void runPlugin () {
		final String buttonText = runButton.getText();
		runButton.setEnabled(false);
		runButton.setText("Running...");

		SwingWorker pluginTester = new SwingWorker() {
			@Override
			protected Object doInBackground () {
				StringBuilder shellCommand = new StringBuilder("java -jar ");
				shellCommand.append('"')
					.append(pluginFile.getAbsolutePath())
					.append("\" \"")
					.append(dataFile.getAbsolutePath())
					.append("\" ")
					.append(argumentsField.getText());

				String pluginResponseString = null;
				try {
					pluginResponseString = Utils.runShellCommand(shellCommand.toString(), null);
				} catch (IOException e) {
					reportError("Error while running plugin.", e);
				}
				pluginResponse = parsePluginResponse(pluginResponseString);

				return null;
			}
			@Override
			protected void done () {
				displayPluginResponse();
				runButton.setText(buttonText);
				runButton.setEnabled(true);
			}
		};
		pluginTester.execute();
	}

	/**
	 * Create and display application GUI.
	 */
	private static void createAndDisplayGUI () {
		// Create main window and set its size, location, close action, etc.
		frame = new JFrame("Plugin Tester for Assignment Manager");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Point center = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		center.translate(- (DEFAULT_SIZE.width / 2), - (DEFAULT_SIZE.height / 2));
		frame.setLocation(center);
		frame.setSize(DEFAULT_SIZE);
		frame.setMinimumSize(MIN_SIZE);
		// Add window close listener cleaning up resources before application terminates.
		frame.addWindowListener(new WindowListener() {
			public void windowOpened(WindowEvent e) {
			}
			public void windowClosing(WindowEvent e) {
				cleanUp();
			}
			public void windowClosed(WindowEvent e) {
			}
			public void windowIconified(WindowEvent e) {
			}
			public void windowDeiconified(WindowEvent e) {
			}
			public void windowActivated(WindowEvent e) {
			}
			public void windowDeactivated(WindowEvent e) {
			}
		});

		// Create main content pane with GridBagLayout.
		Container pane = frame.getContentPane();
		pane.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = GridBagConstraints.RELATIVE;
		c.gridy = 0;
		c.weightx = 0;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 1;
		c.insets = new Insets(3, 5, 2, 5);

		// Add plugin path label.
		JLabel pluginLabel = new JLabel("Plugin:");
		pluginLabel.setFont(boldSansSerifFont);
		pluginLabel.setDisplayedMnemonic(KeyEvent.VK_P);
		pane.add(pluginLabel, c);

		// Add plugin path text.
		c.weightx = 1;
		pluginPathLabel = new JLabel();
		pluginPathLabel.setFont(plainMonospaceFont);
		pane.add(pluginPathLabel, c);

		// Add button for plugin selection.
		c.weightx = 0;
		JButton loadPluginButton = new JButton("Load plugin");
		loadPluginButton.setMnemonic(KeyEvent.VK_P);
		loadPluginButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadPlugin();
			}
		});
		pane.add(loadPluginButton, c);

		// Start new row.
		c.gridy = 1;

		// Add data path label.
		JLabel dataLabel = new JLabel("Data:");
		dataLabel.setFont(boldSansSerifFont);
		dataLabel.setDisplayedMnemonic(KeyEvent.VK_D);
		pane.add(dataLabel, c);

		c.gridx = GridBagConstraints.RELATIVE;

		// Add data path text.
		c.weightx = 1;
		dataPathLabel = new JLabel();
		dataPathLabel.setFont(plainMonospaceFont);
		pane.add(dataPathLabel, c);

		// Add button for data selection.
		c.weightx = 0;
		JButton loadDataButton = new JButton("Load data");
		loadDataButton.setMnemonic(KeyEvent.VK_D);
		loadDataButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadData();
			}
		});
		pane.add(loadDataButton, c);

		// Start new row.
		c.gridy = 2;

		// Add arguments field label.
		JLabel argumentsLabel = new JLabel("Arguments:");
		argumentsLabel.setFont(boldSansSerifFont);
		argumentsLabel.setDisplayedMnemonic(KeyEvent.VK_A);
		pane.add(argumentsLabel, c);

		// Add input field for command-line arguments.
		c.weightx = 1;
		argumentsField = new JTextField();
		argumentsField.setFont(plainMonospaceFont);
		argumentsField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runPlugin();
			}
		});
		argumentsLabel.setLabelFor(argumentsField);
		pane.add(argumentsField, c);

		// Add button for launching plugin (enabled only if plugin and data are selected).
		c.weightx = 0;
		runButton = new JButton("Run");
		runButton.setEnabled(false);
		runButton.setMnemonic(KeyEvent.VK_R);
		runButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runPlugin();
			}
		});
		pane.add(runButton, c);

		// Start new row.
		c.gridy = 3;
		c.insets = new Insets(14, 5, 0, 5);

		// Add separator.
		c.gridwidth = 3;
		JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
		pane.add(separator, c);

		// Start new row.
		c.gridy = 4;

		// Add status label.
		c.gridwidth = 1;
		JLabel statusLabel = new JLabel("Status:");
		statusLabel.setFont(boldSansSerifFont);
		pane.add(statusLabel, c);

		// Add status text.
		c.weightx = 1;
		statusTextLabel = new JLabel();
		statusTextLabel.setFont(boldSansSerifFont);
		pane.add(statusTextLabel, c);

		// Add button for saving output (visible only if output exists).
		c.weightx = 0;
		saveOutputButton = new JButton("Save output");
		saveOutputButton.setMnemonic(KeyEvent.VK_S);
		saveOutputButton.setEnabled(false);
		saveOutputButton.setVisible(false);
		saveOutputButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent e) {
				saveOutput();
			}
		});
		pane.add(saveOutputButton, c);

		// Start new row.
		c.gridy = 5;

		// Add fulfillment label.
		JLabel fulfillmentLabel = new JLabel("Fulfillment:");
		fulfillmentLabel.setFont(boldSansSerifFont);
		pane.add(fulfillmentLabel, c);

		// Add fulfillment text.
		c.weightx = 1;
		c.gridwidth = 2;
		fulfillmentTextLabel = new JLabel();
		fulfillmentTextLabel.setFont(plainSansSerifFont);
		pane.add(fulfillmentTextLabel, c);

		c.gridy = 6;

		// Add details label.
		c.weightx = 0;
		c.gridwidth = 1;
		JLabel detailsLabel = new JLabel("Details:");
		detailsLabel.setFont(boldSansSerifFont);
		detailsLabel.setVerticalAlignment(JLabel.TOP);
		pane.add(detailsLabel, c);

		// Add details text with word wrap and possible vertical scrolling.
		c.weightx = 1;
		c.gridwidth = 2;
		c.weighty = 1;
		detailsTextArea = new JTextArea();
		detailsTextArea.setFont(plainSansSerifFont);
		detailsTextArea.setEditable(false);
		detailsTextArea.setLineWrap(true);
		detailsTextArea.setWrapStyleWord(true);
		detailsTextArea.setBackground(UIManager.getColor("Panel.background"));
		JScrollPane detailsScrollPane = new JScrollPane(detailsTextArea);
		detailsScrollPane.setBorder(new EmptyBorder(0, 0, 6, 0));
		pane.add(detailsScrollPane, c);

		// Add escape key listener (closes application on Esc pressed anywhere).
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
			public boolean dispatchKeyEvent(KeyEvent e) {
				if (e.getID() == KeyEvent.KEY_PRESSED) {
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						exitProgram();
						return true;
					}
				}
				return false;
			}
		});

		// Show main window.
		frame.setVisible(true);
	}

	/**
	 * Application launcher.
	 * 
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndDisplayGUI();
			}
		});
	}

}
