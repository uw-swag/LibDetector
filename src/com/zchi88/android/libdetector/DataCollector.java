package com.zchi88.android.libdetector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.zchi88.android.libdetector.libmetadata.LibraryMetadata;
import com.zchi88.android.libdetector.libmetadata.LibraryStats;

public class DataCollector {

	public static void main(String[] args) throws IOException {
		
		File currentDir = new File(".");
		File outputFile = new File(currentDir, "libMetadata.txt");
		
		System.out.println("Looking for Extracted_APKs folders in " + currentDir.getAbsolutePath());
		
		List<File> extractedAPKsFoldersList = scanExtractedAPKsFoldersInRoot(currentDir);
		
		HashMap<String, LibraryStats> compiledMetadata = new HashMap<String, LibraryStats>();
		int totalExtractedAPKs = 0;
		
		for (File extractedAPKsFolder : extractedAPKsFoldersList) {
			
			System.out.println("Computing " + extractedAPKsFolder.getAbsolutePath());
			
			// Compute the metadata for the libraries found in the Extracted_APKs
			addToCompiledMetadata(compiledMetadata, LibraryMetadata.computeMetadata(extractedAPKsFolder.toPath()));
			totalExtractedAPKs += extractedAPKsFolder.listFiles().length;
		}

		// Write the metadata to a text file
		System.out.println("Writing output file...");
		LibraryMetadata.metadataToFile(compiledMetadata, outputFile);
		System.out.println("Finished computing data. Extracted APKs: " + totalExtractedAPKs);
		
	}

	private static void addToCompiledMetadata(HashMap<String, LibraryStats> compiledMetadata, HashMap<String, LibraryStats> libMetadata)
			throws IOException {

		for (Entry<String, LibraryStats> entry : libMetadata.entrySet()) {
			
			String key = entry.getKey();
			LibraryStats stats = entry.getValue();
			
			if (!compiledMetadata.containsKey(key)) {
				compiledMetadata.put(key, stats);
			}
			else {
				
				LibraryStats compiledStats = compiledMetadata.get(key);
				
				for (int i = 0; i < stats.getLibCount(); i++) {
					compiledStats.incrementLibCount();
				}	
			}
		}
	}
	
	private static List<File> scanExtractedAPKsFoldersInRoot(File rootDir) {
		
		// Check if folder exists in current directory
		File extractedAPKsFolder = new File(rootDir, "Extracted_APKs");
		if (extractedAPKsFolder.exists() && extractedAPKsFolder.isDirectory()) {
			return Arrays.asList(extractedAPKsFolder);
		}

		// Look inside child folders
		List<File> extractedAPKsFolders = new ArrayList<File>();
		for (File item : rootDir.listFiles()) {
			if (item.isDirectory()) {
				File childFolder = new File(item, "Extracted_APKs");
				if (childFolder.exists() && childFolder.isDirectory()) {
					extractedAPKsFolders.add(childFolder);
				}
			}
		}
		
		return extractedAPKsFolders;
	}

}
