package ai.dat.cli.commands;

import ai.dat.cli.commands.server.McpServerCommand;
import ai.dat.cli.commands.server.OpenApiServerCommand;
import ai.dat.cli.provider.VersionProvider;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Project Server commands
 *
 * @Author JunjieM
 * @Date 2025/8/26
 */
@Command(
        name = "server",
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class,
        description = "Start and manage DAT project server",
        subcommands = {
                OpenApiServerCommand.class,
                McpServerCommand.class
        }
)
@Slf4j
public class ServerCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("🌐 DAT Server Management");
        System.out.println("Use 'dat server --help' show available commands");
        return 0;
    }
}
