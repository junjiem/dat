package ai.dat.cli.commands;

import ai.dat.boot.utils.ProjectUtil;
import ai.dat.cli.processor.InputProcessor;
import ai.dat.cli.provider.VersionProvider;
import ai.dat.cli.utils.AnsiUtil;
import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigurationUtils;
import ai.dat.core.data.project.EmbeddingConfig;
import ai.dat.core.data.project.EmbeddingStoreConfig;
import ai.dat.core.factories.*;
import ai.dat.core.utils.FactoryUtil;
import ai.dat.core.utils.JinjaTemplateUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Initialization project commands
 *
 * @Author JunjieM
 * @Date 2025/7/23
 */
@Command(
        name = "init",
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class,
        description = "Initialization DAT project"
)
@Slf4j
public class InitCommand implements Callable<Integer> {
    private final static String PROJECT_README_TEMPLATE_FILE_NAME = "README.md";
    private final static String PROJECT_CONFIG_FILE_NAME = ProjectUtil.PROJECT_CONFIG_FILE_NAME_YAML;
    private final static String PROJECT_CONFIG_TEMPLATE_FILE_NAME =
            ProjectUtil.PROJECT_CONFIG_FILE_NAME_YAML + ".template";

    private static final String PROJECT_NAME_REGEX = "^[a-zA-Z][A-Za-z0-9_\\-]*$";
    private static final Pattern PROJECT_NAME_PATTERN = Pattern.compile(PROJECT_NAME_REGEX);
    private static final InputProcessor PROCESSOR = new InputProcessor();

    private static final String TEMPLATE_DIR_NAME = "project_init_template";
    private static final String PROJECT_README_TEMPLATE_NAME = "templates/project_readme_init_template.jinja";
    private static final String PROJECT_README_TEMPLATE_CONTENT;
    private static final String PROJECT_YAML_TEMPLATE_NAME = "templates/project_yaml_init_template.jinja";
    private static final String PROJECT_YAML_TEMPLATE_CONTENT;

    static {
        try {
            PROJECT_README_TEMPLATE_CONTENT = loadProjectReadmeInitTemplate();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                    "Failed to load project init README.md template file: " + e.getMessage());
        }
        try {
            PROJECT_YAML_TEMPLATE_CONTENT = loadProjectYamlInitTemplate();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                    "Failed to load project init YAML template file: " + e.getMessage());
        }
    }

    private static String loadProjectReadmeInitTemplate() throws IOException {
        try (InputStream stream = InitCommand.class.getClassLoader()
                .getResourceAsStream(PROJECT_README_TEMPLATE_NAME)) {
            if (stream == null) {
                throw new IOException("Project init README.md template file not found in classpath: "
                        + PROJECT_README_TEMPLATE_NAME);
            }
            return new String(stream.readAllBytes());
        }
    }

    private static String loadProjectYamlInitTemplate() throws IOException {
        try (InputStream stream = InitCommand.class.getClassLoader()
                .getResourceAsStream(PROJECT_YAML_TEMPLATE_NAME)) {
            if (stream == null) {
                throw new IOException("Project init YAML template file not found in classpath: "
                        + PROJECT_YAML_TEMPLATE_NAME);
            }
            return new String(stream.readAllBytes());
        }
    }

    @Option(names = {"-w", "--workspace-path"},
            description = "Project workspace path (default: current directory)",
            defaultValue = ".")
    private String workspacePath;

    private final ProjectConfig projectConfig = new ProjectConfig();

    @Override
    public Integer call() {
        try {
            Path path = Paths.get(workspacePath).toAbsolutePath();
            log.info("Init project in {}", path);
            System.out.println("📁 Workspace path: " + path);
            System.out.println("Edit profile of the DAT project");
            System.out.println(AnsiUtil.string("@|fg(green) " + ("─".repeat(100)) + "|@"));
            try {
                projectBasicConfiguration();
                if (projectConfig.isBootMode()) { // 进入配置引导模式
                    projectDbConfiguration();
                    if (!EmbeddingModelFactoryManager.isSupported(EmbeddingConfig.DEFAULT_PROVIDER)) {
                        projectEmbeddingConfiguration();
                    }
                    if (!EmbeddingStoreFactoryManager.isSupported(EmbeddingStoreConfig.DEFAULT_PROVIDER)) {
                        projectEmbeddingStoreConfiguration();
                    }
                    projectLlmConfiguration();
                }
            } catch (EndOfFileException e) {
                // Ctrl+D (EOF) - 优雅退出
                log.debug("EOF received (Ctrl+D)");
                System.out.println("👋 Exit!");
                return 2;
            } catch (UserInterruptException e) {
                // Ctrl+C - 中断信号
                log.debug("User interrupt received (Ctrl+C)");
                System.out.println("👋 Exit!");
                return 2;
            }
            System.out.println(AnsiUtil.string("@|fg(green) " + ("─".repeat(100)) + "|@"));
            System.out.println(AnsiUtil.string("@|fg(red) 📢 For more project configurations, " +
                    "please refer to '" + PROJECT_CONFIG_TEMPLATE_FILE_NAME + "'|@"));
            initDatProject(path); // 初始化 DAT project
            System.out.println(AnsiUtil.string("@|fg(green) ✅ Initialization '"
                    + projectConfig.getName() + "' project completed|@"));
            return 0;
        } catch (Exception e) {
            log.error("Init project failed", e);
            System.err.println(AnsiUtil.string(
                    "@|fg(red) ❌ Init failed: " + e.getMessage() + "|@"));
            return 1;
        }
    }

    private void projectBasicConfiguration() {
        // Project name
        while (true) {
            String name = PROCESSOR.readLine(AnsiUtil.string("@|fg(cyan) Project name|@ " +
                    "[press Enter to next step]: "));
            if (name.isEmpty()) continue;
            if (!isProjectName(name)) {
                System.out.println(AnsiUtil.string("@|fg(red) ⚠️ Project name needs to conform " +
                        "'" + PROJECT_NAME_REGEX + "' regular expression!|@"));
                continue;
            }
            projectConfig.setName(name);
            break;
        }
        // Whether is boot mode
        while (true) {
            String choice = PROCESSOR.readLine(AnsiUtil.string(
                    "@|fg(yellow) Use boot mode to initialize project configuration? (y/n)|@ " +
                            "[User input/press Enter to use the y]: "));
            if (!choice.isEmpty()
                    && !choice.equalsIgnoreCase("n")
                    && !choice.equalsIgnoreCase("y")) continue;
            boolean bootMode = choice.equalsIgnoreCase("y") || choice.isEmpty();
            projectConfig.setBootMode(bootMode);
            break;
        }
        if (projectConfig.isBootMode()) {
            System.out.println("---------- Project basic information ----------");
            // Project description
            String description = PROCESSOR.readLine(AnsiUtil.string(
                    "@|fg(cyan) Project description|@ [press Enter to next step]: "));
            projectConfig.setDescription(description);
        }
    }

    private boolean isProjectName(String str) {
        return PROJECT_NAME_PATTERN.matcher(str.trim()).matches();
    }

    private String selectProvider(Set<String> supports) {
        List<String> options = supports.stream().toList();
        String selected = PROCESSOR.readSelect(AnsiUtil.string(
                "@|fg(yellow) Please select a provider:|@"), options);
        System.out.println(AnsiUtil.string("@|fg(green) ✓ Selected: " + selected + "|@"));
        return selected;
    }

    private void projectDbConfiguration() {
        System.out.println("---------- Data source configuration ----------");
        System.out.println("===== DB provider selection =====");
        String provider = selectProvider(DatabaseAdapterFactoryManager.getSupports());
        ElementConfig config = new ElementConfig();
        config.setProvider(provider);
        DatabaseAdapterFactory factory = DatabaseAdapterFactoryManager.getFactory(provider);
        Set<ConfigOption<?>> requiredOptions = factory.requiredOptions();
        if (!requiredOptions.isEmpty()) {
            System.out.println(AnsiUtil.string(
                    "===== DB provider '@|fg(cyan) " + provider + "|@' configuration ====="));
            config.setConfigs(inputConfigs(requiredOptions));
        }
        projectConfig.setDbConfig(config);
    }

    private void projectLlmConfiguration() {
        System.out.println("---------- LLM configuration ----------");
        System.out.println("===== LLM provider selection =====");
        String provider = selectProvider(ChatModelFactoryManager.getSupports());
        ElementConfig config = new ElementConfig();
        config.setProvider(provider);
        ChatModelFactory factory = ChatModelFactoryManager.getFactory(provider);
        Set<ConfigOption<?>> requiredOptions = factory.requiredOptions();
        if (!requiredOptions.isEmpty()) {
            System.out.println(AnsiUtil.string(
                    "===== LLM provider '@|fg(cyan) " + provider + "|@' configuration ====="));
            config.setConfigs(inputConfigs(requiredOptions));
        }
        projectConfig.setLlmConfig(config);
    }

    private void projectEmbeddingConfiguration() {
        System.out.println("---------- Embedding configuration ----------");
        System.out.println("===== Embedding provider selection =====");
        String provider = selectProvider(EmbeddingModelFactoryManager.getSupports());
        ElementConfig config = new ElementConfig();
        config.setProvider(provider);
        EmbeddingModelFactory factory = EmbeddingModelFactoryManager.getFactory(provider);
        Set<ConfigOption<?>> requiredOptions = factory.requiredOptions();
        if (!requiredOptions.isEmpty()) {
            System.out.println(AnsiUtil.string(
                    "===== Embedding provider '@|fg(cyan) " + provider + "|@' configuration ====="));
            config.setConfigs(inputConfigs(requiredOptions));
        }
        projectConfig.setEmbeddingConfig(config);
    }

    private void projectEmbeddingStoreConfiguration() {
        System.out.println("---------- Embedding Store configuration ----------");
        System.out.println("===== Embedding Store provider selection =====");
        String provider = selectProvider(EmbeddingStoreFactoryManager.getSupports());
        ElementConfig config = new ElementConfig();
        config.setProvider(provider);
        EmbeddingStoreFactory factory = EmbeddingStoreFactoryManager.getFactory(provider);
        Set<ConfigOption<?>> requiredOptions = factory.requiredOptions();
        if (!requiredOptions.isEmpty()) {
            System.out.println(AnsiUtil.string(
                    "===== Embedding Store provider '@|fg(cyan) " + provider + "|@' configuration ====="));
            config.setConfigs(inputConfigs(requiredOptions));
        }
        projectConfig.setEmbeddingStoreConfig(config);
    }

    private Map<String, Object> inputConfigs(Set<ConfigOption<?>> requiredOptions) {
        Map<String, Object> configs = new HashMap<>();
        requiredOptions.forEach(option -> {
            while (true) {
                String key = option.key();
                Class<?> clazz = option.getClazz();
                boolean hasDefaultValue = option.hasDefaultValue();

                String input;
                if (clazz.isEnum()) {
                    List<String> options = Arrays.stream((Enum<?>[]) clazz.getEnumConstants())
                            .map(Enum::name).collect(Collectors.toList());
                    String prompt = AnsiUtil.string("@|fg(cyan) Please select a " + key + ":|@");
                    if (hasDefaultValue) {
                        String defaultValue = ConfigurationUtils.convertValue(option.defaultValue(), String.class);
                        input = PROCESSOR.readSelect(prompt, options, defaultValue, false);
                    } else {
                        input = PROCESSOR.readSelect(prompt, options, false);
                    }
                } else {
                    String hint = " [User input]";
                    String defaultValue = null;
                    if (hasDefaultValue) {
                        defaultValue = ConfigurationUtils.convertValue(option.defaultValue(), String.class);
                        hint = " @|fg(green) (Default: " + defaultValue + ")|@" +
                                " [User input/press Enter to use the default value]";
                    }
                    String prompt = AnsiUtil.string("@|fg(cyan) " + key + "|@" + hint + ": ");
                    if (hasDefaultValue) {
                        input = PROCESSOR.readLine(prompt, defaultValue);
                    } else if (FactoryUtil.isSensitive(key)) {
                        input = PROCESSOR.readPassword(prompt);
                    } else {
                        input = PROCESSOR.readLine(prompt);
                    }
                }

                if (input.isEmpty()) continue;

                Object value = ConfigurationUtils.convertValue(input, clazz);
                configs.put(option.key(), value);
                return;
            }
        });
        return configs;
    }

    /**
     * 初始化DAT项目目录结构和文件
     */
    private void initDatProject(Path workspacePath) throws IOException {
        Path projectPath = workspacePath.resolve(projectConfig.getName());
        System.out.println(AnsiUtil.string(
                "@|fg(blue) 📁 Project path: " + projectPath + "|@"));
        if (Files.exists(projectPath)) {
            throw new IOException("The project directory already exists: " + projectPath);
        }
        log.info("Start init the DAT project: {}", projectPath);
        copyTemplateDirectory(projectPath); // 拷贝模板目录到项目目录
        createProjectReadmeFile(projectPath); // 创建项目 README.md 文件
        if (projectConfig.isBootMode()) {
            createProjectYamlFile(projectPath); // 创建项目配置文件
        }
        createProjectYamlTemplateFile(projectPath); // 创建项目模板配置文件
    }

    /**
     * 递归拷贝模板目录
     *
     * @param projectPath
     * @throws IOException
     */
    private void copyTemplateDirectory(Path projectPath) throws IOException {
        try {
            URL resourceUrl = getClass().getClassLoader().getResource(TEMPLATE_DIR_NAME);
            if (resourceUrl == null) {
                throw new IOException("Template directory not found: " + TEMPLATE_DIR_NAME);
            }
            if ("jar".equals(resourceUrl.getProtocol())) { // 在JAR包中，使用流方式复制
                copyTemplateFromJar(resourceUrl, projectPath);
            } else { // 在文件系统中，使用路径方式复制
                Path templatePath = Paths.get(resourceUrl.toURI());
                if (!Files.exists(templatePath)) {
                    throw new IOException("Template directory not found: " + TEMPLATE_DIR_NAME);
                }
                copyDirectory(templatePath, projectPath);
            }
            log.info("Copy template directory is complete: {} -> {}", TEMPLATE_DIR_NAME, projectPath);
        } catch (URISyntaxException e) {
            throw new IOException("Failed to get template directory URI", e);
        }
    }

    /**
     * 从JAR包中复制模板目录
     *
     * @param source
     * @param target
     * @throws IOException
     */
    private void copyTemplateFromJar(URL source, Path target) throws IOException {
        Files.createDirectories(target);
        JarURLConnection jarConnection = (JarURLConnection) source.openConnection();
        try (JarFile jarFile = jarConnection.getJarFile()) {
            String jarEntryName = jarConnection.getEntryName();
            if (jarEntryName != null) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (entryName.startsWith(jarEntryName + "/") && !entry.isDirectory()) {
                        try (InputStream inputStream = jarFile.getInputStream(entry)) {
                            String fileName = entryName.substring((jarEntryName + "/").length());
                            Path targetPath = target.resolve(fileName);
                            Files.createDirectories(targetPath.getParent());
                            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            log.debug("Copy template file: {} -> {}", entryName, targetPath);
                        }
                    }
                }
            }
        }
    }

    /**
     * 递归拷贝目录
     *
     * @param source
     * @param target
     * @throws IOException
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        if (Files.isDirectory(source)) {
            Files.createDirectories(target);
            try (Stream<Path> stream = Files.list(source)) {
                stream.forEach(sourcePath -> {
                    try {
                        Path targetPath = target.resolve(sourcePath.getFileName());
                        copyDirectory(sourcePath, targetPath);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy: " + sourcePath, e);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("Failed to list directory: " + source, e);
            }
        } else {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 创建项目 README.md 文件
     *
     * @param projectPath
     * @throws IOException
     */
    private void createProjectReadmeFile(Path projectPath) throws IOException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("project", projectConfig);
        String yamlContent = JinjaTemplateUtil.render(PROJECT_README_TEMPLATE_CONTENT, variables);
        Path projectReadmePath = projectPath.resolve(PROJECT_README_TEMPLATE_FILE_NAME);
        Files.write(projectReadmePath, yamlContent.getBytes());
        log.info("Create " + PROJECT_README_TEMPLATE_FILE_NAME + " file: {}", projectReadmePath);
    }

    /**
     * 创建项目配置文件
     *
     * @param projectPath
     * @throws IOException
     */
    private void createProjectYamlFile(Path projectPath) throws IOException {
        DatProjectFactory factory = new DatProjectFactory();
        Map<String, Object> variables = new HashMap<>();
        variables.put("project", projectConfig);
        variables.put("project_configuration", factory.getProjectConfiguration());
        variables.put("agent_configuration", factory.getDefaultAgentConfiguration());
        String yamlContent = JinjaTemplateUtil.render(PROJECT_YAML_TEMPLATE_CONTENT, variables);
        Path projectYamlPath = projectPath.resolve(PROJECT_CONFIG_FILE_NAME);
        Files.write(projectYamlPath, yamlContent.getBytes());
        log.info("Create " + PROJECT_CONFIG_FILE_NAME + " file: {}", projectYamlPath);
    }

    /**
     * 创建项目模板配置文件
     *
     * @param projectPath
     * @throws IOException
     */
    private void createProjectYamlTemplateFile(Path projectPath) throws IOException {
        String yamlContent = new DatProjectFactory().yamlTemplate();
        Path projectYamlTemplatePath = projectPath.resolve(PROJECT_CONFIG_TEMPLATE_FILE_NAME);
        Files.write(projectYamlTemplatePath, yamlContent.getBytes());
        log.info("Create " + PROJECT_CONFIG_TEMPLATE_FILE_NAME + " file: {}", projectYamlTemplatePath);
    }

    @Getter
    @Setter
    static class ProjectConfig {
        private String name;
        private String description;
        private boolean bootMode = false;
        private ElementConfig dbConfig;
        private ElementConfig embeddingConfig;
        private ElementConfig embeddingStoreConfig;
        private ElementConfig llmConfig;
    }

    @Getter
    @Setter
    static class ElementConfig {
        private String provider;
        private Map<String, Object> configs;
    }
}
