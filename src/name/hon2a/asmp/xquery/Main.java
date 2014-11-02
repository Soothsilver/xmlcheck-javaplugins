/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.hon2a.asmp.xquery;

import name.hon2a.asm.Plugin;
import name.hon2a.asm.PluginException;
import name.hon2a.asm.SingleTestPlugin;
import name.hon2a.asm.Utils;

/**
 *
 * @author hon2a
 */
public class Main extends SingleTestPlugin {

	/**
	 * Run plugin.
	 *
	 * @param args command line arguments
	 */
	public static void main (String[] args) {
		Plugin self = new Main();
		System.out.println(self.run(args));
	}

	@Override
	protected void setUp(String[] params) throws PluginException {
		Integer queryCountMin = 1;
		if (params.length > 0) {
			queryCountMin = Integer.parseInt(params[0]);
		}
		
		this.setTest(new XqueryTest(Utils.createStringMap(
				XqueryTest.sourceXml, this.getSourcePath("data.xml"),
				XqueryTest.sourceXqueryMaskLegacy, this.getSourcePath("xquery/query%d.xq"),
                XqueryTest.sourceXqueryMask, this.getSourcePath("query%d.xq")
			), Utils.createStringMap(
				XqueryTest.paramQueryCountMin, queryCountMin.toString(),
				XqueryTest.paramOutputXmlMask, "xqueryXml%d.xml"
			),
                this.getSourceFile("."),
                this.getOutputFile(".")));
	}

}
