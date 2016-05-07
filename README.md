# LibDetector
A tool for detecting external third party libraries used in Android apps. It does this by comparing the structure and bytecode of the APK's of interest against diff files generated by the [LibDiff tool](https://github.com/zchi88/LibDiff "LibDiff Tool").



## DEPENDENCIES
- Java 1.7+

## GET THE TOOL
Simply clone this repository to your local machine.


## TRY IT OUT
The easiest way to use this tool is by creating a runnable Jar. To do so:

1. Import this project into Eclipse:

	```Eclipse
	File > Import > General > Existing Projects into Workspace 

	Select the folder where the tool is cloned to

	Click "Finish" to import this project into Eclipse
	```

2. Create a runnable JAR file:

	```Eclipse
	File > Export > Java > Runnable JAR file

	Choose a destination and name for the JAR(such as LibDetector.jar)

	Click "Finish" to create the runnable JAR
	```

## USAGE
This tool works in conjunction with the LibDiff tool, which computes the diffs between successive versions of a given library. Therefore, as a prerequisite the user must first collect libraries of interest and run the LibDiff tool on that collection before using the LibDetector tool. More about the LibDiff tool and how to use it can be found in the [LibDiff documentation](https://github.com/zchi88/LibDiff "LibDiff Tool"). After having satisfied this requirement, the steps to running the tool and obtaining results on APKs are as follows:

1. Choose any directory to be the tool's working directory.
2. In that working directory, create another directory named ```"Android_APKs"```. Note that the name must be exact so that the LibDetector tool and locate it. 
3. Collect any APK's that you would like the tool to analyze, and place them in the Android_APKs folder as is.
4. Place the runnable LibDetector.jar file in the working directory next to the Android_APKs folder.
An example of the folder structure is shown below. Note that in this example, the libraries whitelist is in the same directory as well. While this is NOT a requirement, it is recommended.

![LibDector Folder Structure](https "LibDector Folder Structure")

5. In the command line, make sure that you have moved to the working directory. Then, issue the following command to run the tool:

	```console
	java -jar LibDetector.jar PATH/TO/WHITELIST_LIBRARIES
	```
	
	Note that the tool expects one argument, which is the path to the libraries whitelist. This is the same whitelist that is being used and maintained by the LibDiff tool.

6. The tool will then create an ```"Extracted_APKs"``` folder. For each APK, it will create a folder in the ```"Extracted_APKs"``` folder named after the APK, and extract the bytecode of the APK to this folder. It will then scan the byecode and try to find matches to any libraries in the provided libraries whitelist. The results will be outputted to a ```libraryMatchResults.txt``` file.


### NOTES
The ```libraryMatchResults.txt``` mentions the term "Levenshtein Ratio". This refers to the ratio of the Levenshtein distance between the class files of a library in the whitelist and the library that exists in the APK to the total size of those files. A ratio of 1.0 means that the files are completely different, and therefore the app most likely does not use that library. However, a ratio of 0.0 means that the files are complete copies, and therefore the app most likely IS using that library.