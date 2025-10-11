package ai.dat.server.openapi.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "dat.server")
@Slf4j
public class ServerConfig implements InitializingBean {

    // 项目路径
    private String projectPath = ".";

    // 动态参数
    private Map<String, Object> variables = Collections.emptyMap();

    public Path getAbsoluteProjectPath() {
        return Paths.get(projectPath).toAbsolutePath();
    }

    public Map<String, Object> getVariables() {
        return new HashMap<>(variables);
    }

    /**
     * 配置加载后的处理
     * 确保CLI传入的参数被正确设置
     */
    @PostConstruct
    public void postConstruct() {
        log.info("=== ServerConfig PostConstruct ===");
        log.info("  - Project path: {}", projectPath);
        log.info("  - Variables: {}", variables);
        log.info("================================");
    }

    /**
     * 配置初始化后的验证
     */
    @Override
    public void afterPropertiesSet() {
        log.info("=== ServerConfig AfterPropertiesSet ===");
        log.info("  - Project path: {}", projectPath);
        log.info("  - Variables: {}", variables);
        log.info("=====================================");

        // 验证项目路径
        if (projectPath == null || projectPath.trim().isEmpty()) {
            log.warn("Project path is empty, using current directory");
            projectPath = ".";
        }

        // 验证动态参数
        if (variables == null || variables.isEmpty()) {
            log.warn("Variables is null, using empty map");
            variables = Collections.emptyMap();
        }
    }
}