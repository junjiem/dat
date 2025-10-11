package ai.dat.server.openapi.controller;

import ai.dat.core.data.project.DatProject;
import ai.dat.server.openapi.config.ServerConfig;
import ai.dat.server.openapi.service.ProjectService;
import ai.dat.server.openapi.utils.VersionUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Information", description = "Health check and system/project/agents information API")
public class InfoController {

    private final ServerConfig serverConfig;
    private final ProjectService projectService;
    
    @Value("${server.address:0.0.0.0}")
    private String serverAddress;
    
    @Value("${server.port:8080}")
    private int serverPort;

    @Operation(summary = "Health checkup", description = "Health check endpoint")
    @ApiResponse(responseCode = "200", description = "Successful")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "message", "DAT OpenAPI Server is running"
        ));
    }

    @Operation(summary = "System information",
            description = "Obtain system version and environment information")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Successful")})
    @GetMapping("/system")
    public ResponseEntity<Map<String, Object>> system() {
        Runtime runtime = Runtime.getRuntime();
        return ResponseEntity.ok(Map.of(
                "system", Map.of(
                        "os", System.getProperty("os.name"),
                        "arch", System.getProperty("os.arch"),
                        "processors", runtime.availableProcessors(),
                        "maxMemory", runtime.maxMemory(),
                        "totalMemory", runtime.totalMemory(),
                        "freeMemory", runtime.freeMemory()
                ),
                "java", Map.of(
                        "version", System.getProperty("java.version"),
                        "vendor", System.getProperty("java.vendor"),
                        "runtime", System.getProperty("java.runtime.name")
                ),
                "server", Map.of(
                        "name", "DAT OpenAPI Server",
                        "version", VersionUtil.getVersion(),
                        "description", "DAT (Data Ask Tool) OpenAPI Server",
                        "address", serverAddress,
                        "port", serverPort
                ),
                "timestamp", LocalDateTime.now()
        ));
    }

    @Operation(summary = "Project information",
            description = "Obtain project information")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Successful")})
    @GetMapping("/project")
    public ResponseEntity<Map<String, Object>> project() {
        DatProject project = projectService.getProject();
        Map<String, Object> projectMap = Map.of(
                "path", serverConfig.getProjectPath(),
                "name", project.getName(),
                "description", project.getDescription() == null ? "<none>" : project.getDescription()
        );
        return ResponseEntity.ok(Map.of(
                "project", projectMap,
                "variables", serverConfig.getVariables(),
                "timestamp", LocalDateTime.now()
        ));
    }

    @Operation(summary = "Agents information list",
            description = "Obtain agents information list")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Successful")})
    @GetMapping("/agents")
    public ResponseEntity<Map<String, Object>> agents() {
        DatProject project = projectService.getProject();
        List<Map<String, String>> agentList = project.getAgents().stream()
                .map(agent -> Map.of(
                        "name", agent.getName(),
                        "provider", agent.getProvider(),
                        "description", agent.getDescription() == null ? "<none>" : agent.getDescription()
                )).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "agents", agentList,
                "variables", serverConfig.getVariables(),
                "timestamp", LocalDateTime.now()
        ));
    }

}
