/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.configAggregator;

import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.JsonFile;
import com.asofterspace.toolbox.io.SimpleFile;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.io.XmlElement;
import com.asofterspace.toolbox.io.XmlFile;
import com.asofterspace.toolbox.utils.Record;
import com.asofterspace.toolbox.Utils;

import java.util.ArrayList;
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

		System.out.println(Utils.getFullProgramIdentifierWithDate());
		System.out.println("");

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
		List<XmlElement> possibleDataItems = root.getChildren("egscc_conf:configurationDataItem");
		List<XmlElement> dataItems = new ArrayList<>();

		for (XmlElement possibleDataItem : possibleDataItems) {
			// how confident are we that we are looking at a message template?
			int confidence = 0;

			try {
				List<XmlElement> entries = possibleDataItem.getChild("egscc_conf:value").getChild("egscc_conf:fields").getChildren("egscc_conf:entry");
				for (XmlElement entry : entries) {
					String key = entry.getChild("egscc_conf:key").getInnerText();
					if ("severity".equals(key)) {
						confidence++;
					}
					if ("messageText".equals(key)) {
						confidence++;
					}
					if ("categoryIds".equals(key)) {
						confidence++;
					}
					if ("type".equals(key)) {
						confidence++;
					}
					if ("requiresAcknowledgement".equals(key)) {
						confidence++;
					}
				}
			} catch (NullPointerException e) {
				// whoops!
			}

			if (confidence > 4) {
				dataItems.add(possibleDataItem);
			}
		}

		System.out.println(dataItems.size() + " messages have been defined in the system...");

		Directory checkoutDir = new Directory(checkoutLocation);
		List<File> codeFiles = checkoutDir.getAllFilesEndingWith(".java", true);
		List<TextFile> codeFilesContainingUuids = new ArrayList<>();
		List<TextFile> testsAndDoublesContainingUuids = new ArrayList<>();
		for (File codeFile : codeFiles) {
			TextFile codeFileMaybeContainingUuids = new TextFile(codeFile);
			if (codeFileMaybeContainingUuids.getContent().contains("import java.util.UUID;")) {
				// ignore tests and doubles for now, but store them for later...
				String absName = codeFile.getAbsoluteFilename();
				if (absName.contains("/itest/") || absName.contains("/vtest/") ||
					absName.contains("/test/") || absName.contains("/doubles/")) {
					testsAndDoublesContainingUuids.add(codeFileMaybeContainingUuids);
				} else {
					codeFilesContainingUuids.add(codeFileMaybeContainingUuids);
				}
			}
		}
		System.out.println(codeFilesContainingUuids.size() + " code files and " + testsAndDoublesContainingUuids.size() +
			" tests and doubles containing some UUIDs belong to the system...");

		Record result = Record.emptyArray();
		for (XmlElement dataItem : dataItems) {
			Record resobj = Record.emptyObject();
			List<XmlElement> entries = dataItem.getChild("egscc_conf:value").getChild("egscc_conf:fields").getChildren("egscc_conf:entry");
			for (XmlElement entry : entries) {
				String key = entry.getChild("egscc_conf:key").getInnerText();
				if ("messageText".equals(key)) {
					resobj.set("text", entry.getChild("egscc_conf:value").getAttribute("value"));
				}
			}

			XmlElement dataItemUuid = dataItem.getChild("egscc_conf:dataItemIdentifier");
			String uuid = null;
			if (dataItemUuid != null) {
				uuid = dataItemUuid.getAttribute("name");
			}
			resobj.set("uuid", uuid);

			XmlElement dataItemName = dataItem.getChild("egscc_conf:dataItemName");
			if (dataItemName == null) {
				resobj.set("name", null);
			} else {
				resobj.set("name", dataItemName.getAttribute("name"));
			}

			List<String> components = new ArrayList<>();
			List<String> sources = new ArrayList<>();
			if (uuid != null) {
				for (TextFile codeFile : codeFilesContainingUuids) {
					if (codeFile.getContent().contains(uuid)) {
						String source = checkoutDir.getRelativePath(codeFile);
						sources.add(source);
						String component = source.substring(0, source.indexOf("/"));
						if (!components.contains(component)) {
							components.add(component);
						}
					}
				}
				boolean noneBeforeTestsAndDoubles = sources.size() < 1;
				for (TextFile codeFile : testsAndDoublesContainingUuids) {
					if (codeFile.getContent().contains(uuid)) {
						String source = checkoutDir.getRelativePath(codeFile);
						sources.add(source);
						String component = source.substring(0, source.indexOf("/"));
						if (!components.contains(component)) {
							components.add(component);
						}
					}
				}
				if (sources.size() < 1) {
					System.out.println(uuid + " is not contained in any sources!");
				} else {
					if (noneBeforeTestsAndDoubles) {
						System.out.println(uuid + " is only contained in tests and / or doubles!");
					}
				}
				if (sources.size() > 1) {
					System.out.println(uuid + " is contained in " + sources.size() + " sources!");
				}
			} else {
				System.out.println("Encounted a message without UUID!");
			}
			resobj.set("components", components);
			resobj.set("sources", sources);

			result.append(resobj);
		}

		JsonFile resultFile = new JsonFile(RESULT_FILE);
		resultFile.setAllContents(result);
		resultFile.save();

		System.out.println("The aggregated result has been saved to " + RESULT_FILE + " - have a fun day! :)");
	}

}
