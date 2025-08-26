package ai.dat.server.openapi.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Component
@ConfigurationProperties(prefix = "dat.server")
@Slf4j
public class ServerConfig implements InitializingBean {

    private String projectPath = ".";
    private String host = "localhost";
    private int port = 8080;
    private boolean corsEnabled = true;

    public Path getAbsoluteProjectPath() {
        return Paths.get(projectPath).toAbsolutePath();
    }

    /**
     * 配置加载后的处理
     * 确保CLI传入的参数被正确设置
     */
    @PostConstruct
    public void postConstruct() {
        log.info("=== ServerConfig PostConstruct ===");
        log.info("  - Project path: {}", projectPath);
        log.info("  - Host: {}", host);
        log.info("  - Port: {}", port);
        log.info("  - CORS enabled: {}", corsEnabled);
        log.info("================================");
    }

    /**
     * 配置初始化后的验证
     */
    @Override
    public void afterPropertiesSet() {
        log.info("=== ServerConfig AfterPropertiesSet ===");
        log.info("  - Project path: {}", projectPath);
        log.info("  - Host: {}", host);
        log.info("  - Port: {}", port);
        log.info("  - CORS enabled: {}", corsEnabled);
        log.info("=====================================");

        // 验证项目路径
        if (projectPath == null || projectPath.trim().isEmpty()) {
            log.warn("Project path is empty, using current directory");
            projectPath = ".";
        }

        // 验证端口
        if (port <= 0 || port > 65535) {
            log.warn("Invalid port number: {}, using default port 8080", port);
            port = 8080;
        }

        // 验证主机地址
        if (host == null || host.trim().isEmpty()) {
            log.warn("Host is empty, using 'localhost'");
            host = "localhost";
        }
    }
}