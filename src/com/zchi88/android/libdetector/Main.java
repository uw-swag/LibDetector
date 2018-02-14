package com.zchi88.android.libdetector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zchi88.android.libdetector.libmetadata.LibraryMetadata;
import com.zchi88.android.libdetector.libmetadata.LibraryStats;
import com.zchi88.android.libdetector.libmetadata.LibraryVersion;
import com.zchi88.android.libdetector.utilities.LibSnapshot;
import com.zchi88.android.libdetector.utilities.Timer;

/**
 * The main class for the LibDetector tool.
 * 
 * @author Zhihao Chi
 *
 */
public class Main {
	
	// The maximum number of threads running concurrently to process the APKs.
	// Optimal size will vary depending on the power of your machine.
	// If your machine experiences noticeable performance drops while running
	// the tool, the number of threads may need to be reduced
	private static int threadpoolSize = Runtime.getRuntime().availableProcessors();
	private static HashMap<Path, ArrayList<LibraryVersion>> libsSnapshot;
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		CommandLine commands = getOptions(args);
	
		Path whitelistPath = Paths.get(commands.getOptionValue(OPTION_LIBRARIES));

		// Check to make sure that the library whitelist directory exists.
		File[] whitelistedLibraries = whitelistPath.toFile().listFiles();
		if (whitelistedLibraries == null) {
			System.err.println(
					"The specified whitelist directory does not exist. Please check that the provided path exists.");
			System.err.println("Exiting program.");
			System.exit(-1);
		}
		
		Path absApksPath = Paths.get(commands.getOptionValue(OPTION_APKS));
		Path absExtractPath = Paths.get(commands.getOptionValue(OPTION_OUTPUT));

		// Check to make sure that the directory Android APKs directory exists.
		File[] apks = absApksPath.toFile().listFiles();
		if (apks == null) {
			System.err.println("Error: The Android_APKs directory cannot be found.");
			System.err
					.println("Please make sure that this tool is in the same directory as the Android_APKs directory.");
			System.err.println("Exiting program.");
			System.exit(-1);
		}

		long startTime = System.currentTimeMillis();

		System.out.println("Starting LibDetector tool.");
		System.out.println("Performing analysis on APKs located at: " + absApksPath);
		System.out.println("Drawing potential library matches from: " + whitelistPath);
		System.out.println("Extracting results to: " + absExtractPath);
		System.out.println("Looking for libraries now...\n");

		// Get a "snapshot" of all libraries and their different version in the
		// whitelist
		libsSnapshot = LibSnapshot.getLibsSnapshot(whitelistPath);

		// Start a threadpool service to increase processing power
		ExecutorService libDetectorThreads = Executors.newFixedThreadPool(threadpoolSize);

		// For each file in the Android_APKs directory, get their source code
		// then identify the libraries in them.
		File[] apkFiles = absApksPath.toFile().listFiles();

		for (File file : apkFiles) {
			if (file.getName().endsWith(".apk")) {
				LibDetector detector = new LibDetector(libsSnapshot, file, absExtractPath);
				libDetectorThreads.execute(detector);
			}
		}

		// Clean up threadpool after threads finish executing
		libDetectorThreads.shutdown();

		// Wait for all threads to finish executing before displaying completion
		// message
		libDetectorThreads.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);

		if (libDetectorThreads.isShutdown()) {
			System.out.println("Finished scanning APK's for libraries.");
		}

		// Show how long it took to scan all the APKs for libraries
		long endTime = System.currentTimeMillis();
		System.out.println("Processed " + apkFiles.length + " APKs in " + Timer.msToString(endTime - startTime));
		System.out.println();

		// Compute the metadata for the libraries found in the APKs
		HashMap<String, LibraryStats> libMetadata = LibraryMetadata.computeMetadata(absExtractPath);

		// Write the metadata to a text file
		Path workingDir = Paths.get("").toAbsolutePath();
		File outputFile = workingDir.resolve("libMetadata.txt").toFile();
		LibraryMetadata.metadataToFile(libMetadata, outputFile);
	}
	
	private static final String OPTION_LIBRARIES = "libraries";
	private static final String OPTION_APKS = "apks";
	private static final String OPTION_OUTPUT = "output";
	private static final String OPTION_THREADS = "threads";
	
	private static CommandLine getOptions(String[] args) {
		
		Options options = new Options();
		
		options.addOption(Option.builder("l")
							    .longOpt(OPTION_LIBRARIES)
							    .hasArg()
							    .argName("path")
							    .required()
							    .desc("full path to libraries whitelist folder")
							    .build());

		options.addOption(Option.builder("a")
							    .longOpt(OPTION_APKS)
							    .hasArg()
							    .argName("path")
							    .required()
							    .desc("full path to APKs folder")
							    .build());

		options.addOption(Option.builder("o")
							    .longOpt(OPTION_OUTPUT)
							    .hasArg()
							    .argName("path")
							    .required()
							    .desc("full path to results output folder")
							    .build());

		options.addOption(Option.builder("t")
							    .longOpt(OPTION_THREADS)
							    .hasArg()
							    .argName("number")
							    .required(false)
							    .desc("number of threads to run (optional - defaults to number of cores)")
							    .build());
		
		CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
            
            if (cmd.hasOption(OPTION_THREADS)) {
            		threadpoolSize = Integer.parseInt(cmd.getOptionValue(OPTION_THREADS));
            }
        }
        // Display correct usage information for this tool.
        catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -jar AndroidLibDetector.jar [args]", options);
            System.exit(1);
        } catch (NumberFormatException e) {
            formatter.printHelp("java -jar AndroidLibDetector.jar [args]", options);
            System.exit(1);
		}
        
		return cmd;
	}
}
