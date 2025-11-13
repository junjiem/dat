package ai.dat.cli.commands.server;

import ai.dat.boot.ProjectBuilder;
import ai.dat.cli.provider.VersionProvider;
import ai.dat.server.mcp.Application;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Project MCP Server commands
 *
 * @Author JunjieM
 * @Date 2025/9/8
 */
@Command(
        name = "mcp",
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class,
        description = "Start DAT MCP (SSE) Server"
)
@Slf4j
public class McpServerCommand implements Callable<Integer> {

    @Option(names = {"-p", "--project-path"},
            description = "Project path (default: current directory)",
            defaultValue = ".")
    private String projectPath;

    @Option(names = {"-H", "--host"},
            description = "Server host (default: 0.0.0.0)",
            defaultValue = "0.0.0.0")
    private String host;

    @Option(names = {"-P", "--port"},
            description = "Server port (default: 8081)",
            defaultValue = "8081")
    private int port;

    public enum TransportType {
        SSE("HTTP with SSE"),
        STREAMABLE("Streamable HTTP"),
        STATELESS("Stateless Streamable HTTP");

        private final String value;

        TransportType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @Option(names = {"-t", "--transport"},
            description = "MCP transport type. " +
                    "Supported: `SSE` - HTTP with SSE, `STREAMABLE` - Streamable HTTP, " +
                    "`STATELESS` - Stateless Streamable HTTP. (default: SSE)",
            defaultValue = "SSE")
    private TransportType transport;

    @Option(names = {"-var", "--variable"},
            arity = "1..*",
            description = "Dynamic variable, key-value pairs in format key=value")
    private Map<String, Object> variables;

    @Override
    public Integer call() {
        try {
            Path path = Paths.get(projectPath).toAbsolutePath();
            log.info("Start MCP server the project: {}", path);
            System.out.println("📁 Project path: " + path);
            System.out.println("🛠️ Dynamic variables: " + variables);

            ProjectBuilder builder = new ProjectBuilder(path);
            builder.build(variables);

            System.out.println();
            System.out.println("🚀 Starting DAT MCP Server...");
            System.out.println("🌐 Server Address: http://" + host + ":" + port);
            System.out.println();

            // 创建Spring应用
            SpringApplication app = new SpringApplication(Application.class);
            app.setBannerMode(Banner.Mode.OFF);

            List<String> argsList = new ArrayList<>() {{
                add("--spring.profiles.active=mcp");
                add("--server.port=" + port);
                add("--server.address=" + host);
                add("--dat.server.project-path=" + projectPath);
            }};
            if (variables != null && !variables.isEmpty()) {
                variables.forEach((k, v) -> argsList.add("--dat.server.variables." + k + "=" + v));
            }
            if (TransportType.SSE != transport) {
                argsList.add("--spring.ai.mcp.server.protocol=" + transport.name());
            }
            String[] args = argsList.toArray(new String[0]);

            // 直接运行Spring Boot应用，它会阻塞当前线程
            try {
                // Spring Boot应用会阻塞当前线程直到应用关闭
                ConfigurableApplicationContext context = app.run(args);
                addShutdownHook(context);

                // 验证应用是否成功启动
                if (context.isActive()) {
                    System.out.println("✅ Server started successfully!");
                    printUrls(transport);

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
                log.error("Failed to start MCP server", e);
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

    private void printUrls(TransportType transport) {
        System.out.println("🧩 MCP Server type: " + transport.value);
        String endpoint = TransportType.SSE == transport ? "/sse" : "/mcp";
        String url = "http://" + host + ":" + port + endpoint;
        System.out.println("📖 MCP Server url: " + url);
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
