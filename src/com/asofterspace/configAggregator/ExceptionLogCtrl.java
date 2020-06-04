/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.configAggregator;

import com.asofterspace.toolbox.codeeditor.JavaCode;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.TextFile;

import java.util.ArrayList;
import java.util.List;


public class ExceptionLogCtrl {

	public final static String RESULT_FILE = "exceptions.txt";

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

		JavaCode javaCodeHighlighter = new JavaCode(null);

		int totalCatchBlocks = 0;
		int totalExDot = 0;
		int totalProblems = 0;
		int totalExToString = 0;
		int totalExPrintStackTrace = 0;
		int totalExIdentMissing = 0;
		StringBuilder exIdentMissingOutput = new StringBuilder();

		// for each code file...
		for (TextFile curFile : codeFiles) {

			// ... if the plaintext "catch" is somewhere in there...
			if (!curFile.getContent().contains("catch")) {
				continue;
			}

			int curCatchBlocks = 0;
			int curProblems = 0;
			int curExIdentMissing = 0;
			int curExToString = 0;
			int curExPrintStackTrace = 0;
			int curExDot = 0;

			// ... actually properly analyse it, by first transforming it into a basic representation...
			String content = curFile.getContent();
			content = javaCodeHighlighter.removeCommentsAndStrings(content);
			content.replace('\t', ' ');
			content.replace('\r', ' ');
			content.replace('\n', ' ');

			System.out.println("DEBUG file: " + curFile.getAbsoluteFilename());

			while (content.contains(" catch ") || content.contains(" catch(") ||
				   content.contains("}catch ") || content.contains("}catch(")) {
				curCatchBlocks++;
			System.out.println("DEBUG catch block: " + curCatchBlocks);

				int nextPos = Integer.MAX_VALUE;
				if (content.indexOf(" catch ") > -1) {
					nextPos = Math.min(nextPos, content.indexOf(" catch "));
				}
				if (content.indexOf(" catch(") > -1) {
					nextPos = Math.min(nextPos, content.indexOf(" catch("));
				}
				if (content.indexOf("}catch ") > -1) {
					nextPos = Math.min(nextPos, content.indexOf("}catch "));
				}
				if (content.indexOf("}catch(") > -1) {
					nextPos = Math.min(nextPos, content.indexOf("}catch("));
				}
				content = content.substring(nextPos + 1);

				// get the exceptionIdentifier
				String exceptionIdentifier = content.substring(content.indexOf("(") + 1);
				exceptionIdentifier = exceptionIdentifier.substring(0, exceptionIdentifier.indexOf(")"));
				exceptionIdentifier = exceptionIdentifier.trim();
				if (exceptionIdentifier.contains(" ")) {
					exceptionIdentifier = exceptionIdentifier.substring(exceptionIdentifier.lastIndexOf(" ") + 1);
				}
				// in case of catch (Foo bar), exceptionIdentifier is now bar

				System.out.println("DEBUG exceptionIdentifier: |" + exceptionIdentifier + "|");

				// get the code contained in the catch block
				int depth = 0;
				int i = 0;
				String codeInCatch = content.substring(content.indexOf("{"));
				for (; i < codeInCatch.length(); i++) {
					if (codeInCatch.charAt(i) == '{') {
						depth++;
					}
					if (codeInCatch.charAt(i) == '}') {
						depth--;
						if (depth < 1) {
							codeInCatch = codeInCatch.substring(0, i+1);
							break;
						}
					}
				}

				System.out.println("DEBUG codeInCatch: |" + codeInCatch + "|");

				// check if the exceptionIdentifier is present in the code inside the catch at all -
				// but .toString() does not count!
				while (codeInCatch.contains(" .")) {
					codeInCatch = codeInCatch.replaceAll(" \\.", ".");
				}

				// e.getCause() basically counts as e - that is, just the exception itself
				while (codeInCatch.contains(exceptionIdentifier + ".getCause()")) {
					codeInCatch = codeInCatch.replaceAll(exceptionIdentifier + "\\.getCause()", exceptionIdentifier);
				}

				boolean exToStringFound = false;
				boolean exPrintStackTraceFound = false;
				while (codeInCatch.contains(exceptionIdentifier + ".toString()")) {
					exToStringFound = true;
					codeInCatch = codeInCatch.replaceAll(exceptionIdentifier + "\\.toString()", "");
				}
				while (codeInCatch.contains(exceptionIdentifier + ".printStackTrace()")) {
					exPrintStackTraceFound = true;
					codeInCatch = codeInCatch.replaceAll(exceptionIdentifier + "\\.printStackTrace()", "");
				}
				while (codeInCatch.contains(exceptionIdentifier + ".getMessage()")) {
					exToStringFound = true;
					codeInCatch = codeInCatch.replaceAll(exceptionIdentifier + "\\.getMessage()", "");
				}
				while (codeInCatch.contains(exceptionIdentifier + ".getLocalizedMessage()")) {
					exToStringFound = true;
					codeInCatch = codeInCatch.replaceAll(exceptionIdentifier + "\\.getLocalizedMessage()", "");
				}
				codeInCatch = codeInCatch.replace('{', ' ');
				codeInCatch = codeInCatch.replace('}', ' ');
				codeInCatch = codeInCatch.replace('(', ' ');
				codeInCatch = codeInCatch.replace(')', ' ');
				codeInCatch = codeInCatch.replace('<', ' ');
				codeInCatch = codeInCatch.replace('>', ' ');
				codeInCatch = codeInCatch.replace('[', ' ');
				codeInCatch = codeInCatch.replace(']', ' ');
				codeInCatch = codeInCatch.replace(';', ' ');
				codeInCatch = codeInCatch.replace(',', ' ');
				codeInCatch = codeInCatch.replace('@', ' ');
				codeInCatch = codeInCatch.replace('=', ' ');
				// we do not replace all dots, as bla(ex) means that ex is used, but bla.ex means that the
				// variable ex is NOT actually used!

				System.out.println("exceptionIdentifier: |" + exceptionIdentifier + "|");
				System.out.println("codeInCatch: |" + codeInCatch + "|");

				boolean exDotFound = false;
				while (codeInCatch.contains(" " + exceptionIdentifier + ".")) {
					exDotFound = true;
					codeInCatch = codeInCatch.replaceAll(" " + exceptionIdentifier + "\\.", " " + exceptionIdentifier + " ");
				}
				if (exDotFound) {
					curExDot++;
				}

				codeInCatch = " " + codeInCatch + " ";
				if (!codeInCatch.contains(" " + exceptionIdentifier + " ")) {
					if (exPrintStackTraceFound) {
						curExPrintStackTrace++;
					}
					if (exToStringFound) {
						curExToString++;
					}
					if ((!exToStringFound) && (!exPrintStackTraceFound)) {
						curExIdentMissing++;
					}
					curProblems++;
					continue;
				}

				// so we now know that the exceptionIdentifier is actually being used inside the catch
				// block, and not just for .toString() - however, we still cannot be sure that its stack
				// trace will be logged (e.g. it could be appended to a string as ""+ex, or its stack
				// trace could go into the trace log and never be seen again rather than being properly
				// logged...)

				// TODO
			}

			if (curProblems > 0) {
				exIdentMissingOutput.append(curFile.getAbsoluteFilename() + " - " + curProblems + " exceptions missing from catch blocks");
				exIdentMissingOutput.append("\n");
			}

			totalCatchBlocks += curCatchBlocks;
			totalExDot += curExDot;
			totalProblems += curProblems;
			totalExIdentMissing += curExIdentMissing;
			totalExToString += curExToString;
			totalExPrintStackTrace += curExPrintStackTrace;

			// output what we found for this one file
			System.out.println("");
			System.out.println("In the code file " + curFile.getAbsoluteFilename() + " we found:");
			System.out.println(curCatchBlocks + " catch blocks");
			System.out.println(curExDot + " times the exception identifier was used with a . behind it (not a problem!)");
			System.out.println(curProblems + " problems in catch blocks");
			System.out.println(curExIdentMissing + " times the exception identifier was completely unused");
			System.out.println(curExToString + " times the exception identifier was unused except for .toString(), .getMessage() or .getLocalizedMessage()");
			System.out.println(curExPrintStackTrace + " times the exception identifier was unused except for .printStackTrace()");
		}

		// output what we found in total
		System.out.println("");
		System.out.println("In total we found:");
		System.out.println(totalCatchBlocks + " catch blocks");
		System.out.println(totalExDot + " times the exception identifier was used with a . behind it (not a problem!)");
		System.out.println(totalProblems + " problems in catch blocks");
		System.out.println(totalExIdentMissing + " times the exception identifier was completely unused");
		System.out.println(totalExToString + " times the exception identifier was unused except for .toString(), .getMessage() or .getLocalizedMessage()");
		System.out.println(totalExPrintStackTrace + " times the exception identifier was unused except for .printStackTrace()");

		System.out.println("");
		TextFile resultFile = new TextFile(RESULT_FILE);
		resultFile.saveContent(exIdentMissingOutput);
		System.out.println("A list of all source files in which we found problems has been saved to " +
			resultFile.getAbsoluteFilename());
	}
}
