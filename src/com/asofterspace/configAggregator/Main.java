/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.configAggregator;

import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.JsonFile;
import com.asofterspace.toolbox.io.SimpleFile;
import com.asofterspace.toolbox.io.XmlElement;
import com.asofterspace.toolbox.io.XmlFile;
import com.asofterspace.toolbox.utils.Record;
import com.asofterspace.toolbox.Utils;

import java.util.List;


public class Main {

	public final static String PROGRAM_TITLE = "ConfigAggregator";
	public final static String VERSION_NUMBER = "0.0.0.2(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "10. March 2020 - 11. March 2020";

	public final static String TEMP_FILE = "temp.xml";
	public final static String RESULT_FILE = "messages.json";


	public static void main(String[] args) {

		// let the Utils know in what program it is being used
		Utils.setProgramTitle(PROGRAM_TITLE);
		Utils.setVersionNumber(VERSION_NUMBER);
		Utils.setVersionDate(VERSION_DATE);

		for (String arg : args) {
			if ("--version".equals(arg)) {
				System.out.println(Utils.getFullProgramIdentifierWithDate());
				return;
			}
			if ("--version_for_zip".equals(arg)) {
				System.out.println("version " + Utils.getVersionNumber());
				return;
			}
		}

		if (args.length < 1) {
			System.err.println("Please call the " + PROGRAM_TITLE + " with the location of the checked out source codes as first argument!");
			return;
		}

		String checkoutLocation = args[0];

		System.out.println("Loading the configuration from " + checkoutLocation + "...");

		checkoutLocation += "/";

		File messageTemplates = new File(checkoutLocation + "egscc-deployment/impl/esa.egscc.deployment.sysConfGen/src/main/resources/fragments/messageTemplates.xml");

		if (!messageTemplates.exists()) {
			System.err.println("The file " + messageTemplates.getFilename() + " does not seem to exist!");
			return;
		}

		messageTemplates.copyToDisk(TEMP_FILE);

		SimpleFile simpleMessageTemplates = new SimpleFile(TEMP_FILE);

		simpleMessageTemplates.insertContent("<root>", 0);
		simpleMessageTemplates.appendContent("</root>");
		simpleMessageTemplates.save();

		XmlFile xmlMessageTemplates = new XmlFile(TEMP_FILE);
		XmlElement root = xmlMessageTemplates.getRoot();
		List<XmlElement> dataItems = root.getChildren("egscc_conf:configurationDataItem");
		System.out.println(dataItems.size() + " messages have been defined in the system...");

		Record result = Record.emptyArray();
		for (XmlElement dataItem : dataItems) {
			Record resobj = Record.emptyObject();
			List<XmlElement> entries = dataItem.getChild("egscc_conf:value").getChild("egscc_conf:fields").getChildren("egscc_conf:entry");
			for (XmlElement entry : entries) {
				if ("messageText".equals(entry.getChild("egscc_conf:key").getInnerText())) {
					resobj.set("text", entry.getChild("egscc_conf:value").getAttribute("value"));
				}
			}
			resobj.set("uuid", dataItem.getChild("egscc_conf:dataItemIdentifier").getAttribute("name"));
			resobj.set("name", dataItem.getChild("egscc_conf:dataItemName").getAttribute("name"));
			// TODO resobj.set("component", );
			result.append(resobj);
		}

		JsonFile resultFile = new JsonFile(RESULT_FILE);
		resultFile.setAllContents(result);
		resultFile.save();

		System.out.println("The aggregated result has been saved to " + RESULT_FILE + " - have a fun day! :)");
	}

}
