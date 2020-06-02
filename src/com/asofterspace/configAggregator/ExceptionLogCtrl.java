/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.configAggregator;

import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.TextFile;

import java.util.ArrayList;
import java.util.List;


public class ExceptionLogCtrl {

	private Directory checkoutDir;


	public ExceptionLogCtrl(Directory checkoutDir) {
		this.checkoutDir = checkoutDir;
	}

	public void findUnloggedExceptionTraces() {

		boolean recursively = true;
		List<File> javaFiles = checkoutDir.getAllFilesEndingWith(".java", recursively);
		List<TextFile> codeFiles = new ArrayList<>();
		List<TextFile> testsAndDoubles = new ArrayList<>();
		for (File javaFile : javaFiles) {
			TextFile curFile = new TextFile(javaFile);
			String absName = curFile.getAbsoluteFilename();
			if (absName.contains("/itest/") || absName.contains("/vtest/") ||
				absName.contains("/test/") || absName.contains("/doubles/")) {
				testsAndDoubles.add(curFile);
			} else {
				codeFiles.add(curFile);
			}
		}
		System.out.println(codeFiles.size() + " code files and " + testsAndDoubles.size() +
			" tests and doubles belong to the system...");

		// for each code file...
		for (TextFile curFile : codeFiles) {
			// ... if the plaintext "catch" is somewhere in there...
			if (curFile.getContent().contains("catch")) {
				// ... actually properly analyze it!
				// TODO
			}
		}
	}
}
