package ai.dat.cli;

import ai.dat.cli.commands.*;
import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

/**
 * DAT CLI 主类
 *
 * @Author JunjieM
 * @Date 2025/7/22
 */
@Slf4j
@Command(
        name = "dat",
        mixinStandardHelpOptions = true,
        version = "DAT CLI 0.1-SNAPSHOT",
        description = "DAT (Data Ask Tool) Command Line Interface",
        subcommands = {
                InitCommand.class,
                BuildCommand.class,
                RunCommand.class,
                CleanCommand.class,
                ListCommand.class
        }
)
public class DatCli implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("DAT - Data Ask Tool");
        System.out.println("Use 'dat --help' show available commands");
        return 0;
    }

    private static void printBanner() {
        try (InputStream is = DatCli.class.getResourceAsStream("/banner.txt")) {
            if (is != null) {
                new BufferedReader(new InputStreamReader(is)).lines()
                        .forEach(System.out::println);
            }
        } catch (IOException e) {
            //
        }
    }

    public static void main(String[] args) {
//        System.setProperty("file.encoding", "UTF-8");
//        System.setProperty("sun.jnu.encoding", "UTF-8");
        printBanner();
        AnsiConsole.systemInstall(); // enable colors on Windows
        int exitCode = new CommandLine(new DatCli()).execute(args);
        AnsiConsole.systemUninstall(); // cleanup when done
        System.exit(exitCode);
    }
}