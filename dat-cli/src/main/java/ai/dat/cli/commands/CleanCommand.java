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
 * Clean commands
 *
 * @Author JunjieM
 * @Date 2025/7/22
 */
@Command(
        name = "clean",
        description = "Clean DAT project state and cache"
)
@Slf4j
public class CleanCommand implements Callable<Integer> {

    @Option(names = {"-p", "--project-path"},
            description = "Project path (default: current directory)",
            defaultValue = ".")
    private String projectPath;

    @Option(names = {"-a", "--all"},
            description = "Clean all build state files")
    private boolean cleanAll;

    @Option(names = {"-k", "--keep-count"},
            description = "Number of build state files to keep (default: 1)",
            defaultValue = "1")
    private int keepCount;

    @Override
    public Integer call() {
        try {
            Path path = Paths.get(projectPath).toAbsolutePath();
            log.info("Clean project: {}", path);
            System.out.println("📁 Project path: " + path);
            ProjectBuilder builder = new ProjectBuilder(path);
            if (cleanAll) {
                log.info("Clear all state files...");
                builder.cleanAllStates();
                System.out.println(Ansi.ON.string(
                        "@|fg(green) ✅ All state files have been cleared|@"));
            } else {
                log.info("Clear the expired state files, keep count: {}", keepCount);
                builder.cleanOldStates(keepCount);
                System.out.println(Ansi.ON.string(
                        "@|fg(green) ✅ The expired state files has been cleared|@"));
            }
            return 0;
        } catch (Exception e) {
            log.error("Clean project failed", e);
            System.err.println(Ansi.ON.string(
                    "@|fg(red) ❌ Clean failed: " + e.getMessage() + "|@"));
            return 1;
        }
    }
}