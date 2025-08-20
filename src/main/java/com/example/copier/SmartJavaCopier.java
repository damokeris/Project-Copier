package com.example.copier;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SmartJavaCopier {

    // ========================= Configuration Area =========================
    // 1. Root path to scan for Java projects (set to user home directory)
    private static final Path PROJECTS_ROOT = Paths.get(System.getProperty("user.home"));

    // 2. Target root directory for copying code
    private static final Path DOC_ROOT = Paths.get(System.getProperty("user.home"), "Documents", "CODE");
    
    // 3. Java project indicators
    private static final List<String> JAVA_PROJECT_INDICATORS = Arrays.asList(
        "pom.xml",          // Maven
        "build.gradle",     // Gradle
        "build.xml",        // Ant
        ".project",         // Eclipse
        ".classpath",       // Eclipse
        "src/main/java",    // Standard source structure
        "src"               // Simple source structure
    );
    
    // 4. Maximum depth for recursive directory scanning (to avoid performance issues)
    private static final int MAX_SCAN_DEPTH = 5;
    
    // 5. Directories to exclude from scanning (to avoid permission issues)
    private static final List<String> EXCLUDED_DIRECTORIES = Arrays.asList(
        "AppData", "Application Data", "Local Settings", "Windows", "Program Files",
        "Program Files (x86)", "ProgramData", "System Volume Information", "$Recycle.Bin"
    );
    // ==========================================================

    public static void main(String[] args) {
        printHeader("Java Source Code Copier (Interactive Mode)");

        // 1. Find and display available Java projects under user home (recursively)
        if (!Files.isDirectory(PROJECTS_ROOT)) {
            System.err.println("[Error] User home directory not found: " + PROJECTS_ROOT);
            System.err.println("Please check if the system user home directory is accessible.");
            waitForEnterAndExit();
        }

        System.out.println("[Info] Recursively scanning for Java projects under \"" + PROJECTS_ROOT + "\" (max depth: " + MAX_SCAN_DEPTH + ")...\n");
        List<Path> javaProjects = findJavaProjectDirectories(PROJECTS_ROOT);

        if (javaProjects.isEmpty()) {
            System.out.println("[Warning] No Java projects found in \"" + PROJECTS_ROOT + "\" directory.");
            waitForEnterAndExit();
        }

        displayProjects(javaProjects);
        System.out.println("-------------------------------------------------------------");

        // 2. Get and validate user input
        int choice = getUserChoice(javaProjects.size());
        Path selectedProject = javaProjects.get(choice - 1);
        String projectName = selectedProject.getFileName().toString();

        // 3. Determine source and target paths
        Path sourcePath = determineSourcePath(selectedProject);
        Path destPath = DOC_ROOT.resolve(projectName);

        // 4. Perform copy operation
        copyJavaFiles(projectName, sourcePath, destPath);

        waitForEnterAndExit();
    }

    private static List<Path> findJavaProjectDirectories(Path root) {
        List<Path> javaProjects = new ArrayList<>();
        try {
            findJavaProjectsRecursive(root, 0, javaProjects);
        } catch (Exception e) {
            System.err.println("[Warning] Error during directory scanning: " + e.getMessage());
        }
        return javaProjects;
    }

    private static void findJavaProjectsRecursive(Path directory, int depth, List<Path> javaProjects) {
        if (depth > MAX_SCAN_DEPTH) {
            return;
        }
        
        // Skip excluded directories
        String dirName = directory.getFileName().toString();
        if (EXCLUDED_DIRECTORIES.contains(dirName)) {
            return;
        }

        try {
            // Check if current directory is a Java project
            if (isJavaProject(directory)) {
                javaProjects.add(directory);
            }

            // Recursively scan subdirectories
            try (Stream<Path> stream = Files.list(directory)) {
                stream.filter(Files::isDirectory)
                      .forEach(subDir -> findJavaProjectsRecursive(subDir, depth + 1, javaProjects));
            }
        } catch (AccessDeniedException e) {
            System.err.println("[Warning] Access denied to directory: " + directory);
        } catch (IOException e) {
            System.err.println("[Warning] Error accessing directory: " + directory + " - " + e.getMessage());
        }
    }

    private static boolean isJavaProject(Path directory) {
        // Skip if we can't read the directory
        if (!Files.isReadable(directory)) {
            return false;
        }

        // Check for common Java project indicators
        for (String indicator : JAVA_PROJECT_INDICATORS) {
            try {
                Path indicatorPath = directory.resolve(indicator);
                if (Files.exists(indicatorPath)) {
                    // Special handling for src directory - check if it contains Java files
                    if (indicator.equals("src")) {
                        try (Stream<Path> srcWalk = Files.walk(indicatorPath, 1)) {
                            boolean hasJavaFiles = srcWalk.anyMatch(path -> 
                                path.toString().endsWith(".java") && Files.isRegularFile(path));
                            if (hasJavaFiles) return true;
                        } catch (IOException e) {
                            // If we can't check the src directory, skip it
                            continue;
                        }
                    } else if (indicator.equals("src/main/java")) {
                        // For src/main/java, just check if it exists (it's a directory already)
                        return true;
                    } else {
                        // For files like pom.xml, build.gradle, etc.
                        return true;
                    }
                }
            } catch (SecurityException e) {
                // Skip if we can't access the indicator path
                continue;
            }
        }
        
        // Check if directory directly contains Java files (for simple projects)
        try (Stream<Path> stream = Files.list(directory)) {
            boolean hasJavaFiles = stream.anyMatch(path -> 
                path.toString().endsWith(".java") && Files.isRegularFile(path));
            if (hasJavaFiles) return true;
        } catch (IOException e) {
            // If we can't list the directory, skip it
        }
        
        return false;
    }

    private static void displayProjects(List<Path> projects) {
        for (int i = 0; i < projects.size(); i++) {
            Path project = projects.get(i);
            String relativePath = PROJECTS_ROOT.relativize(project).toString();
            if (relativePath.isEmpty()) {
                relativePath = project.getFileName().toString();
            }
            System.out.printf("  %d. %s%n", i + 1, relativePath);
        }
    }

    private static int getUserChoice(int maxChoice) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\nPlease enter the number of the project you want to copy: ");
            String input = scanner.nextLine();
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= maxChoice) {
                    return choice;
                } else {
                    System.err.println("[Error] Invalid input, please enter a number between 1 and " + maxChoice + ".");
                }
            } catch (NumberFormatException e) {
                System.err.println("[Error] Invalid input, please enter a valid number from the list.");
            }
        }
    }

    private static Path determineSourcePath(Path projectDir) {
        // Check for standard Maven/Gradle source structure
        Path standardSourcePath = projectDir.resolve("src").resolve("main").resolve("java");
        if (Files.isDirectory(standardSourcePath)) {
            return standardSourcePath;
        }
        
        // Check for simple src directory
        Path simpleSourcePath = projectDir.resolve("src");
        if (Files.isDirectory(simpleSourcePath)) {
            // Verify it contains Java files
            try (Stream<Path> walk = Files.walk(simpleSourcePath, 2)) {
                boolean hasJavaFiles = walk.anyMatch(path -> 
                    path.toString().endsWith(".java") && Files.isRegularFile(path));
                if (hasJavaFiles) return simpleSourcePath;
            } catch (IOException e) {
                // If we can't check, fall back to project directory
            }
        }
        
        // Fall back to project directory itself
        System.out.println("[Info] Standard source structure not found, will copy from project root directory.");
        return projectDir;
    }

    private static void copyJavaFiles(String projectName, Path sourceDir, Path destDir) {
        clearConsole();
        System.out.println("[Info] Flattening and copying Java project files...");
        System.out.println("  Project Name: " + projectName);
        System.out.println("  Source Directory:   " + sourceDir);
        System.out.println("  Target Directory: " + destDir);
        System.out.println();

        try {
            if (!Files.exists(destDir)) {
                System.out.println("[Operation] Creating target directory: " + destDir);
                Files.createDirectories(destDir);
            }

            final int[] fileCount = {0};
            final int[] conflictCount = {0};

            System.out.println("[Operation] Collecting and copying only .java files...");
            try (Stream<Path> walk = Files.walk(sourceDir)) {
                walk.filter(path -> path.toString().endsWith(".java") && Files.isRegularFile(path))
                    .forEach(sourceFile -> {
                        fileCount[0]++;
                        String fileName = sourceFile.getFileName().toString();
                        Path destFile = destDir.resolve(fileName);

                        if (Files.exists(destFile)) {
                            conflictCount[0]++;
                            int counter = 1;
                            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                            String extension = fileName.substring(fileName.lastIndexOf('.'));
                            Path newDestFile;
                            String newName;
                            do {
                                newName = String.format("%s_%d%s", baseName, counter++, extension);
                                newDestFile = destDir.resolve(newName);
                            } while (Files.exists(newDestFile));
                            
                            System.out.println("  [Rename] " + fileName + " -> " + newName);
                            copyFile(sourceFile, newDestFile);
                        } else {
                            System.out.println("  [Copy] " + fileName);
                            copyFile(sourceFile, destFile);
                        }
                    });
            }

            printSummary(fileCount[0], conflictCount[0], destDir);

        } catch (IOException e) {
            System.err.println("\n[Critical Error] File operation failed: " + e.getMessage());
        }
    }

    private static void copyFile(Path source, Path dest) {
        try {
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("  [Error] Failed to copy file: " + source + " -> " + dest);
        }
    }

    private static void printHeader(String title) {
        clearConsole();
        System.out.println("=============================================================");
        System.out.println("     " + title);
        System.out.println("=============================================================\n");
    }

    private static void printSummary(int fileCount, int conflictCount, Path destDir) {
        System.out.println("\n====================== Operation Completed ======================");
        if (fileCount == 0) {
            System.out.println("  No .java files found in the specified source directory.");
        } else {
            System.out.println("  Total .java files processed: " + fileCount);
            System.out.println("  Number of files renamed due to conflicts: " + conflictCount);
            System.out.println("  All files copied to:");
            System.out.println("  " + destDir);
        }
        System.out.println("=========================================================\n");
    }

    private static void waitForEnterAndExit() {
        System.out.print("Press Enter to exit...");
        new Scanner(System.in).nextLine();
        System.exit(0);
    }

    private static void clearConsole() {
        // This is a simple simulation of clearing the console, may not be perfect on all terminals, but provides visual separation
        for (int i = 0; i < 50; ++i) System.out.println();
    }
}
