package ai.dat.cli.commands.server;

import ai.dat.boot.ProjectBuilder;
import ai.dat.cli.provider.VersionProvider;
import ai.dat.server.openapi.Application;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Project OpenAPI Server commands
 *
 * @Author JunjieM
 * @Date 2025/8/26
 */
@Command(
        name = "openapi",
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class,
        description = "Start DAT OpenAPI Server and Swagger UI"
)
@Slf4j
public class OpenApiServerCommand implements Callable<Integer> {

    @Option(names = {"-p", "--project-path"},
            description = "Project path (default: current directory)",
            defaultValue = ".")
    private String projectPath;

    @Option(names = {"-H", "--host"},
            description = "Server host (default: localhost)",
            defaultValue = "localhost")
    private String host;

    @Option(names = {"-P", "--port"},
            description = "Server port (default: 8080)",
            defaultValue = "8080")
    private int port;

    @Option(names = {"--cors"},
            description = "Enable CORS (default: true)",
            defaultValue = "true")
    private boolean corsEnabled;

    @Override
    public Integer call() {
        try {
            Path path = Paths.get(projectPath).toAbsolutePath();
            log.info("Start openapi server the project: {}", path);
            System.out.println("📁 Project path: " + path);

            ProjectBuilder builder = new ProjectBuilder(path);
            builder.build();

            System.out.println();
            System.out.println("🚀 Starting DAT OpenAPI Server...");
            System.out.println("🌐 Server Address: http://" + host + ":" + port);
            System.out.println();

            // 创建Spring应用
            SpringApplication app = new SpringApplication(Application.class);
            app.setBannerMode(Banner.Mode.OFF);

            String[] args = {
                    "--server.port=" + port,
                    "--dat.server.project-path=" + projectPath,
                    "--dat.server.host=" + host,
                    "--dat.server.cors-enabled=" + corsEnabled
            };

            // 直接运行Spring Boot应用，它会阻塞当前线程
            try {
                // Spring Boot应用会阻塞当前线程直到应用关闭
                ConfigurableApplicationContext context = app.run(args);
                addShutdownHook(context);

                // 验证应用是否成功启动
                if (context.isActive()) {
                    System.out.println("✅ Server started successfully!");
                    printUrls();

                    // 等待应用关闭
                    context.registerShutdownHook();

                    // 保持主线程运行，直到应用关闭
                    while (context.isActive()) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } else {
                    System.err.println("❌ Server failed to start properly");
                    return 1;
                }
            } catch (Exception e) {
                log.error("Failed to start Spring Boot application", e);
                System.err.println("❌ Failed to start server: " + e.getMessage());
                return 1;
            }

            System.out.println("Server stopped.");

            return 0;
        } catch (Exception e) {
            log.error("Failed to start server", e);
            System.err.println("❌ Failed to start server: " + e.getMessage());
            return 1;
        }
    }

    private void printUrls() {
        String baseUrl = "http://" + host + ":" + port;
        System.out.println("📖 Swagger UI: " + baseUrl + "/swagger-ui/index.html");
        System.out.println("📄 API Docs:   " + baseUrl + "/v3/api-docs");
        System.out.println("🏥 Health:     " + baseUrl + "/api/v1/health");
        System.out.println();
        System.out.println("Press Ctrl+C to stop");
    }

    private void addShutdownHook(ConfigurableApplicationContext context) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Stopping server...");
            context.close();
        }));
    }
}
