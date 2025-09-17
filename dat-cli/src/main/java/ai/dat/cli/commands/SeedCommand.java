package ai.dat.cli.commands;

import ai.dat.boot.ProjectSeeder;
import ai.dat.boot.utils.ProjectUtil;
import ai.dat.cli.provider.VersionProvider;
import ai.dat.cli.utils.AnsiUtil;
import ai.dat.core.data.project.DatProject;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Seed project commands
 *
 * @Author JunjieM
 * @Date 2025/9/12
 */
@Command(
        name = "seed",
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class,
        description = "Seed DAT project"
)
@Slf4j
public class SeedCommand implements Callable<Integer> {

    @Option(names = {"-p", "--project-path"},
            description = "Project path (default: current directory)",
            defaultValue = ".")
    private String projectPath;

    @ArgGroup(exclusive = true)
    SelectAndExclude selectAndExclude;

    static class SelectAndExclude {
        @Option(names = {"-s", "--select"},
                description = "Selected seed names (comma-separated)")
        String select;

        @Option(names = {"-e", "--exclude"},
                description = "Excluded seed names (comma-separated)")
        String exclude;
    }

    @Override
    public Integer call() {
        try {
            Path path = Paths.get(projectPath).toAbsolutePath();
            log.info("Start seed the project: {}", path);
            System.out.println("📁 Project path: " + path);
            DatProject project = ProjectUtil.loadProject(path);
            ProjectSeeder seeder = new ProjectSeeder(path, project);
            log.info("Seed...");
            if (selectAndExclude != null
                    && selectAndExclude.select != null && !selectAndExclude.select.trim().isEmpty()) {
                List<String> selects = Arrays.stream(selectAndExclude.select.split(",")).map(String::trim).toList();
                log.info("Selected seeds: {}", selects);
                System.out.println(AnsiUtil.string("@|fg(yellow) 🎯 Selected seeds: " + selects + "|@"));
                seeder.seedSelect(selects);
            } else if (selectAndExclude != null
                    && selectAndExclude.exclude != null && !selectAndExclude.exclude.trim().isEmpty()) {
                List<String> excludes = Arrays.stream(selectAndExclude.exclude.split(",")).map(String::trim).toList();
                log.info("Excluded seeds: {}", excludes);
                System.out.println(AnsiUtil.string("@|fg(yellow) 🚫 Excluded seeds: " + excludes + "|@"));
                seeder.seedExclude(excludes);
            } else {
                seeder.seedAll();
            }
            System.out.println(AnsiUtil.string("@|fg(green) ✅ Seed completed|@"));
            log.info("Seed completed");
            return 0;
        } catch (Exception e) {
            log.error("Project seed failed", e);

            System.err.println(AnsiUtil.string(
                    "@|fg(red) ❌ Seed failed: " + e.getMessage() + "|@"));
            return 1;
        }
    }
}