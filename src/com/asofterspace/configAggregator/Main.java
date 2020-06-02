/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.configAggregator;

import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.Utils;


public class Main {

	public final static String PROGRAM_TITLE = "ConfigAggregator";
	public final static String VERSION_NUMBER = "0.0.0.2(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "10. March 2020 - 11. March 2020";


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
			if ("--help".equals(arg) || "-help".equals(arg)) {
				System.out.println("Call the " + PROGRAM_TITLE + " with the location of the checked out source codes as first argument, " +
					"optionally followed by one or several configuration files that contain (among other things) message templates.");
				return;
			}
		}

		System.out.println(Utils.getFullProgramIdentifierWithDate());
		System.out.println("");

		if (args.length < 1) {
			System.err.println("Please call the " + PROGRAM_TITLE + " with the location of the checked out source codes as first argument!");
			return;
		}

		String checkoutLocation = args[0];
		Directory checkoutDir = new Directory(checkoutLocation);

		ConfigAggCtrl configAggCtrl = new ConfigAggCtrl(checkoutDir);
		configAggCtrl.aggregateConfiguration(args);

		ExceptionLogCtrl exceptionLogCtrl = new ExceptionLogCtrl(checkoutDir);
		exceptionLogCtrl.findUnloggedExceptionTraces();

		System.out.println("\nAll done - have a fun day! :)");
	}

}
