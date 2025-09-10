# Smart Java Copier (智能Java代码复制器)

一个强大的Java项目源代码复制工具，能够智能识别和复制Java项目文件，支持多模块项目和配置文件处理。

## 功能特性

- 🔍 **智能项目扫描**: 自动递归扫描用户主目录下的Java项目
- 📁 **多格式支持**: 识别Maven、Gradle、Ant和Eclipse项目结构
- 🧩 **多模块处理**: 自动检测和处理Maven/Gradle多模块项目
- ⚙️ **配置文件复制**: 同时复制常见的配置文件（application.properties, logback.xml等）
- 🔄 **冲突解决**: 自动重命名冲突的文件名
- 💻 **交互式操作**: 提供用户友好的命令行交互界面
- 🚀 **原生执行**: 支持GraalVM原生镜像编译

## 系统要求

- Java 17 或更高版本
- Maven 3.6+ (用于构建)
- (可选) GraalVM 22.3.1+ (用于原生编译)

## 快速开始

### 1. 克隆项目
```bash
git clone <项目地址>
cd project-copier
```

### 2. 构建项目
```bash
# 编译为JAR包
mvn clean package

# 或者编译为原生可执行文件 (需要GraalVM)
mvn clean package -Pnative
```

### 3. 运行工具
```bash
# 运行JAR版本
java -jar target/project-copier-1.0.0.jar

# 或者运行原生版本 (Linux)
./target/SmartJavaCopier
```

## 使用方法

1. **启动工具**: 运行程序后，它会自动扫描您的用户主目录下的Java项目
2. **选择项目**: 从显示的列表中选择要复制的项目编号
3. **确认操作**: 如果目标目录已存在，可以选择覆盖、跳过或重命名
4. **完成复制**: 工具会将所有Java源文件和配置文件复制到 `~/Documents/CODE/` 目录

## 配置选项

工具提供以下可配置参数（在 [`SmartJavaCopier.java`](src/main/java/com/example/copier/SmartJavaCopier.java:13) 中修改）：

- `PROJECTS_ROOT`: 项目扫描根目录（默认：用户主目录）
- `DOC_ROOT`: 文件复制目标目录（默认：~/Documents/CODE）
- `JAVA_PROJECT_INDICATORS`: Java项目识别标识符
- `ADDITIONAL_FILES_TO_COPY`: 额外要复制的配置文件类型
- `MAX_SCAN_DEPTH`: 最大递归扫描深度（默认：5）
- `EXCLUDED_DIRECTORIES`: 排除扫描的目录列表

## 支持的项目类型

- **Maven项目**: 识别 pom.xml 文件
- **Gradle项目**: 识别 build.gradle 文件  
- **Ant项目**: 识别 build.xml 文件
- **Eclipse项目**: 识别 .project 和 .classpath 文件
- **标准项目结构**: 识别 src/main/java 目录
- **简单项目结构**: 识别包含 .java 文件的 src 目录

## 多模块项目支持

工具能够自动检测多模块项目：
- 解析 Maven pom.xml 中的 `<modules>` 配置
- 解析 Gradle settings.gradle 中的 include 配置
- 为每个子模块的文件添加服务名称后缀以避免冲突

## 构建选项

### 标准JAR构建
```bash
mvn clean package
```

### 原生镜像构建 (需要GraalVM)
```bash
mvn clean package -Pnative
```

### Linux平台特定构建 (静态链接)
```bash
mvn clean package -Pnative -Plinux
```

## 故障排除

### 常见问题
1. **权限不足**: 确保对扫描目录有读取权限
2. **项目未找到**: 检查 `PROJECTS_ROOT` 配置是否正确
3. **复制失败**: 检查目标目录是否有写入权限

### 日志信息
工具会在控制台输出详细的操作日志，包括：
- 扫描到的项目列表
- 文件复制进度
- 冲突解决情况
- 错误和警告信息

## 许可证

本项目基于 MIT 许可证开源 - 查看 [`LICENSE`](LICENSE:1) 文件了解详情。

## 贡献

欢迎提交Issue和Pull Request来改进这个项目！

## 版本历史

- v1.0.0 (2025-09-10): 初始版本发布，包含基本复制功能和多模块支持