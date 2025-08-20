package com.example.copier;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SmartJavaCopier {

    // ========================= 配置区域 =========================
    // 1. IdeaProjects目录的根路径 (自动获取用户主目录)
    private static final Path PROJECTS_ROOT = Paths.get(System.getProperty("user.home"), "IdeaProjects");

    // 2. 拷贝代码的目标根目录
    private static final Path DOC_ROOT = Paths.get(System.getProperty("user.home"), "Documents", "CODE");
    // ==========================================================

    public static void main(String[] args) {
        // Java默认使用UTF-8，通常无需像chcp那样设置
        // 设置程序标题在标准Java中不易实现，但对功能无影响

        printHeader("Java Source Code Copier (Interactive Mode)");

        // 1. 查找并显示可用的项目
        if (!Files.isDirectory(PROJECTS_ROOT)) {
            System.err.println("[错误] 未找到IdeaProjects目录: " + PROJECTS_ROOT);
            System.err.println("请检查脚本中的 \"PROJECTS_ROOT\" 配置是否正确。");
            waitForEnterAndExit();
        }

        System.out.println("[信息] 正在扫描 \"" + PROJECTS_ROOT + "\" 下的项目...\n");
        List<Path> projects = findProjectDirectories(PROJECTS_ROOT);

        if (projects.isEmpty()) {
            System.out.println("[警告] 在 \"" + PROJECTS_ROOT + "\" 目录中没有找到任何项目文件夹。");
            waitForEnterAndExit();
        }

        displayProjects(projects);
        System.out.println("-------------------------------------------------------------");

        // 2. 获取并验证用户输入
        int choice = getUserChoice(projects.size());
        Path selectedProject = projects.get(choice - 1);
        String projectName = selectedProject.getFileName().toString();

        // 3. 确定源路径和目标路径
        Path sourcePath = determineSourcePath(selectedProject);
        Path destPath = DOC_ROOT.resolve(projectName);

        // 4. 执行拷贝操作
        copyJavaFiles(projectName, sourcePath, destPath);

        waitForEnterAndExit();
    }

    private static List<Path> findProjectDirectories(Path root) {
        try (Stream<Path> stream = Files.list(root)) {
            return stream.filter(Files::isDirectory)
                         .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                         .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("[错误] 扫描项目目录时发生IO异常: " + e.getMessage());
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
            System.out.print("\n请输入你想要拷贝的项目的编号: ");
            String input = scanner.nextLine();
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= maxChoice) {
                    return choice;
                } else {
                    System.err.println("[错误] 输入无效，请输入 1 到 " + maxChoice + " 之间的一个数字。");
                }
            } catch (NumberFormatException e) {
                System.err.println("[错误] 输入无效，请输入列表中一个有效的数字。");
            }
        }
    }

    private static Path determineSourcePath(Path projectDir) {
        Path potentialSourcePath = projectDir.resolve("src").resolve("main").resolve("java");
        if (Files.isDirectory(potentialSourcePath)) {
            return potentialSourcePath;
        } else {
            System.out.println("[信息] 未找到 \"" + projectDir.getFileName() + "/src/main/java\"，将从项目根目录拷贝。");
            return projectDir;
        }
    }

    private static void copyJavaFiles(String projectName, Path sourceDir, Path destDir) {
        clearConsole();
        System.out.println("[信息] 正在扁平化复制Java项目文件...");
        System.out.println("  项目名称: " + projectName);
        System.out.println("  源目录:   " + sourceDir);
        System.out.println("  目标目录: " + destDir);
        System.out.println();

        try {
            if (!Files.exists(destDir)) {
                System.out.println("[操作] 正在创建目标目录: " + destDir);
                Files.createDirectories(destDir);
            }

            final int[] fileCount = {0};
            final int[] conflictCount = {0};

            System.out.println("[操作] 正在收集并仅复制 .java 文件...");
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
                            
                            System.out.println("  [重命名] " + fileName + " -> " + newName);
                            copyFile(sourceFile, newDestFile);
                        } else {
                            System.out.println("  [复制] " + fileName);
                            copyFile(sourceFile, destFile);
                        }
                    });
            }

            printSummary(fileCount[0], conflictCount[0], destDir);

        } catch (IOException e) {
            System.err.println("\n[严重错误] 文件操作失败: " + e.getMessage());
        }
    }

    private static void copyFile(Path source, Path dest) {
        try {
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("  [错误] 复制文件失败: " + source + " -> " + dest);
        }
    }

    private static void printHeader(String title) {
        clearConsole();
        System.out.println("=============================================================");
        System.out.println("     " + title);
        System.out.println("=============================================================\n");
    }

    private static void printSummary(int fileCount, int conflictCount, Path destDir) {
        System.out.println("\n====================== 操 作 完 成 ======================");
        if (fileCount == 0) {
            System.out.println("  在指定源目录中未找到任何 .java 文件。");
        } else {
            System.out.println("  处理 .java 文件总数: " + fileCount);
            System.out.println("  因重名而重命名文件数: " + conflictCount);
            System.out.println("  所有文件已复制到:");
            System.out.println("  " + destDir);
        }
        System.out.println("=========================================================\n");
    }

    private static void waitForEnterAndExit() {
        System.out.print("按 Enter 键退出...");
        new Scanner(System.in).nextLine();
        System.exit(0);
    }


    private static void clearConsole() {
        // 这是一个简单的模拟清屏，在所有终端上不一定完美，但能提供视觉分隔
        for (int i = 0; i < 50; ++i) System.out.println();
    }
}
