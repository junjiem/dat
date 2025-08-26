package ai.dat.cli.commands;

import ai.dat.cli.provider.VersionProvider;
import ai.dat.cli.utils.AnsiUtil;
import ai.dat.core.data.project.AgentConfig;
import ai.dat.core.data.project.DatProject;
import ai.dat.boot.utils.ProjectUtil;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * List information commands
 *
 * @Author JunjieM
 * @Date 2025/7/22
 */
@Command(
        name = "list",
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class,
        description = "List DAT project information"
)
@Slf4j
public class ListCommand implements Callable<Integer> {

    @Option(names = {"-p", "--project-path"},
            description = "Project path (default: current directory)",
            defaultValue = ".")
    private String projectPath;

    @Override
    public Integer call() {
        try {
            Path path = Paths.get(projectPath).toAbsolutePath();
            log.info("List project: {}", path);
            System.out.println("📁 Project path: " + path);
            DatProject project = ProjectUtil.loadProject(path);
            // 列出Project配置
            System.out.println(AnsiUtil.string("@|fg(green) " + ("─".repeat(100)) + "|@"));
            System.out.println(" Name: " + project.getName());
            System.out.println(" Description: " +
                    (project.getDescription() == null ? "<none>" : project.getDescription()));
            System.out.println(" Path: " + path);
            System.out.println();
            // 列出Agent配置
            System.out.println("🤖 Agents:");
            for (AgentConfig agent : project.getAgents()) {
                System.out.println("   - Name: " + agent.getName());
                System.out.println("     Provider: " + agent.getProvider());
                System.out.println("     Description: " +
                        (agent.getDescription() == null ? "<none>" : agent.getDescription()));
                System.out.println("     Semantic Models: "
                        + (agent.getSemanticModels().isEmpty() ? "<none>" :
                        String.join(", ", agent.getSemanticModels())));
            }
            System.out.println(AnsiUtil.string("@|fg(green) " + ("─".repeat(100)) + "|@"));
            return 0;
        } catch (Exception e) {
            log.error("List project information failed", e);
            System.err.println(AnsiUtil.string(
                    "@|fg(red) ❌ List information failed: " + e.getMessage() + "|@"));
            return 1;
        }
    }
}