package com.zchi88.android.libdetector.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Contains methods for extracting the byte code(.class files) from APKS.
 * 
 */
public class DexExtractor {
	private static final String DEX2JAR_VERSION = "dex2jar-2.1";
	private static final String DEX2JAR_FILE_WINDOWS = "d2j-dex2jar.bat";
	private static final String DEX2JAR_FILE_LINUX = "d2j-dex2jar.sh";
	
	/**
	 * Extracts the class files from all APKs found at the specified folder, and
	 * places them in a separate folder
	 * 
	 * @param apksFolder
	 *            the directory where the folder containing all the APKs exist
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void dex2jarAPKs(Path apksFolder, Path extractedApkFolder) throws InterruptedException, IOException {
		File[] apkFiles = apksFolder.toFile().listFiles();
		for (File file : apkFiles) {
			if (file.getName().endsWith(".apk")) {
				extractJars(file, true, extractedApkFolder);
			} else {
				System.err.println("Warning: " + file + " is not an APK. Skipping file...");
			}
		}
	}

	/**
	 * Given an APK file, extracts the class files found in the APK's
	 * classes.dex files and places them in a "Extracted_APKs" folder.
	 * 
	 * @param apkFile
	 *            the APK file to extract
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void extractJars(File apkFile, Boolean showOutput, Path extractedApkFolder) throws InterruptedException, IOException {
		if (showOutput) {
			System.out.println("Classes.dex files for " + apkFile + " have not been extracted. Extracting now...");
		}
		
		File extractedApkFile = new File(apkFile.getName().replace(".apk", ""));
		Path extractionPath = apkFile.toPath().getParent().getParent().resolve(extractedApkFolder)
				.resolve(extractedApkFile.toPath());
		ArrayList<File> apksToExtract = new ArrayList<File>();

		// Create the folder to extract to if it does not exist
		if (!extractionPath.toFile().exists()) {
			extractionPath.toFile().mkdirs();
		}

		// Check for multiple class files in the APK
		int dexCount = 0;
		JarFile jar = new JarFile(apkFile);
		Enumeration<JarEntry> filesInJar = jar.entries();
		while (filesInJar.hasMoreElements()) {
			JarEntry entry = filesInJar.nextElement();
			if (entry.toString().contains("classes") && entry.toString().endsWith(".dex")) {
				dexCount++;
			}
		}

		// If more than one exists, we must extract their classes.dex
		// differently
		if (dexCount > 1) {
			Enumeration<JarEntry> dexFiles = jar.entries();
			int dexId = 0;
			while (dexFiles.hasMoreElements()) {
				JarEntry entry = dexFiles.nextElement();
				if (entry.toString().contains("classes") && entry.toString().endsWith(".dex")) {
					dexId++;
					String dexName = apkFile.getName().replace(".apk", "(" + dexId + ")");
					File dexFile = new File(dexName + ".dex");
					Path dexPath = extractionPath.resolve(dexFile.toPath());

					if (!dexPath.toFile().exists()) {
						java.io.InputStream is = jar.getInputStream(entry);
						java.io.FileOutputStream fos = new java.io.FileOutputStream(dexPath.toFile());
						while (is.available() > 0) {
							fos.write(is.read());
						}
						fos.close();
						is.close();
						apksToExtract.add(dexPath.toFile());
					}
				}
			}
		} else {
			String extractedName = apkFile.getName().replace(".apk", "") + "-dex2jar.jar";
			File extractedFile = extractionPath.resolve(Paths.get(extractedName)).toFile();
			if (!extractedFile.exists()) {
				apksToExtract.add(apkFile);
			}
		}
		jar.close();

		// Create a process
		final ProcessBuilder processBuilder = new ProcessBuilder();
		// Redirect any output (including error) to a file. This avoids
		// deadlocks when the buffers get full.
		File outputFile = extractionPath.resolve("dex2jarOutput.txt").toFile();
		processBuilder.redirectErrorStream(true);
		processBuilder.redirectOutput(outputFile);

		// Iterate through the list of APKs/dex files to extract, and run
		// dex2jar on it with processbuilder
		for (File file : apksToExtract) {
			String machineOS = System.getProperty("os.name").toLowerCase();
			
			if (machineOS.startsWith("linux") || machineOS.startsWith("mac")) {
				Path pathToDex2Jar = Paths.get("").toAbsolutePath().resolve(DEX2JAR_VERSION).resolve(DEX2JAR_FILE_LINUX);
				String[] command = {"sh", pathToDex2Jar.toString(), file.toString() };
				processBuilder.command(command);
			}

			if (machineOS.startsWith("windows")) {
				Path pathToDex2Jar = Paths.get("").toAbsolutePath().resolve(DEX2JAR_VERSION).resolve(DEX2JAR_FILE_WINDOWS);
				String[] command = {pathToDex2Jar.toString(), file.toString() };
				processBuilder.command(command);
			}

			// Set working directory. By default, this is where the APKs will
			// extract to.
			processBuilder.directory(extractionPath.toFile());

			// Start the process and wait for it to finish.
			final Process process = processBuilder.start();
			process.waitFor();

			// Delete the output file since we don't care about it
			outputFile.delete();
		}
		
		if (showOutput) {
			System.out.println("Extraction complete.\n");
		}
	}
}
