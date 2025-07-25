package ai.dat.cli.commands;

import ai.dat.core.project.build.ProjectBuilder;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Help.Ansi;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Build project commands
 *
 * @Author JunjieM
 * @Date 2025/7/22
 */
@Command(
        name = "build",
        description = "Build DAT project"
)
@Slf4j
public class BuildCommand implements Callable<Integer> {

    @Option(names = {"-p", "--project-path"},
            description = "Project path (default: current directory)",
            defaultValue = ".")
    private String projectPath;

    @Option(names = {"-f", "--force"},
            description = "Force rebuild project")
    private boolean force;

    @Override
    public Integer call() {
        try {
            Path path = Paths.get(projectPath).toAbsolutePath();
            log.info("Start build the project: {}", path);
            System.out.println("📁 Project path: " + path);
            ProjectBuilder builder = new ProjectBuilder(path);
            if (force) {
                log.info("Force rebuild...");
                builder.forceRebuild();
                System.out.println(Ansi.ON.string(
                        "@|fg(green) ✅ Force rebuild completed|@"));
            } else {
                log.info("Incremental build...");
                builder.build();
                System.out.println(Ansi.ON.string(
                        "@|fg(green) ✅ Incremental build completed|@"));
            }
            return 0;
        } catch (Exception e) {
            log.error("Project build failed", e);
            System.err.println(Ansi.ON.string(
                    "@|fg(red) ❌ Build failed: " + e.getMessage() + "|@"));
            return 1;
        }
    }
}