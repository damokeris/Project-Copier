package com.example.copier;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SmartJavaCopier {

    // ========================= Configuration Area =========================
    // 1. Root path of IdeaProjects directory (automatically obtained from user home directory)
    private static final Path PROJECTS_ROOT = Paths.get(System.getProperty("user.home"), "IdeaProjects");

    // 2. Target root directory for copying code
    private static final Path DOC_ROOT = Paths.get(System.getProperty("user.home"), "Documents", "CODE");
    // ==========================================================

    public static void main(String[] args) {
        // Java uses UTF-8 by default, usually no need to set like chcp
        // Setting program title is not easy in standard Java, but it doesn't affect functionality

        printHeader("Java Source Code Copier (Interactive Mode)");

        // 1. Find and display available projects
        if (!Files.isDirectory(PROJECTS_ROOT)) {
            System.err.println("[Error] IdeaProjects directory not found: " + PROJECTS_ROOT);
            System.err.println("Please check if the \"PROJECTS_ROOT\" configuration in the script is correct.");
            waitForEnterAndExit();
        }

        System.out.println("[Info] Scanning projects under \"" + PROJECTS_ROOT + "\"...\n");
        List<Path> projects = findProjectDirectories(PROJECTS_ROOT);

        if (projects.isEmpty()) {
            System.out.println("[Warning] No project folders found in \"" + PROJECTS_ROOT + "\" directory.");
            waitForEnterAndExit();
        }

        displayProjects(projects);
        System.out.println("-------------------------------------------------------------");

        // 2. Get and validate user input
        int choice = getUserChoice(projects.size());
        Path selectedProject = projects.get(choice - 1);
        String projectName = selectedProject.getFileName().toString();

        // 3. Determine source and target paths
        Path sourcePath = determineSourcePath(selectedProject);
        Path destPath = DOC_ROOT.resolve(projectName);

        // 4. Perform copy operation
        copyJavaFiles(projectName, sourcePath, destPath);

        waitForEnterAndExit();
    }

    private static List<Path> findProjectDirectories(Path root) {
        try (Stream<Path> stream = Files.list(root)) {
            return stream.filter(Files::isDirectory)
                         .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                         .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("[Error] IO exception occurred while scanning project directories: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private static void displayProjects(List<Path> projects) {
        for (int i = 0; i < projects.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, projects.get(i).getFileName());
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
        Path potentialSourcePath = projectDir.resolve("src").resolve("main").resolve("java");
        if (Files.isDirectory(potentialSourcePath)) {
            return potentialSourcePath;
        } else {
            System.out.println("[Info] \"" + projectDir.getFileName() + "/src/main/java\" not found, will copy from project root directory.");
            return projectDir;
        }
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
