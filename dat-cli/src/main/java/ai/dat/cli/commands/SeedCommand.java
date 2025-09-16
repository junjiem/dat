package ai.dat.cli.commands;

import ai.dat.boot.ProjectSeeder;
import ai.dat.boot.utils.ProjectUtil;
import ai.dat.cli.provider.VersionProvider;
import ai.dat.cli.utils.AnsiUtil;
import ai.dat.core.data.project.DatProject;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Override
    public Integer call() {
        try {
            Path path = Paths.get(projectPath).toAbsolutePath();
            log.info("Start seed the project: {}", path);
            System.out.println("📁 Project path: " + path);
            DatProject project = ProjectUtil.loadProject(path);
            ProjectSeeder seeder = new ProjectSeeder(path, project);
            log.info("Seed...");
            seeder.seed();
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