package ai.dat.cli.processor;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Help.Ansi;

import java.io.Console;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * @Author JunjieM
 * @Date 2025/7/23
 */
@Slf4j
public class WindowsInputProcessor implements InputProcessor {

    private static final Charset[] CHARSETS = {
            StandardCharsets.UTF_8,
            StandardCharsets.UTF_16,
            Charset.forName("GBK"),
            Charset.forName("GB2312"),
            Charset.defaultCharset()
    };

    private Console console;
    private Scanner scanner;

    public WindowsInputProcessor() {
        // 尝试使用 Console 类
        console = System.console();
        if (console != null) {
            log.info("Using System.console() for input");
            return;
        }
        scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    }

    @Override
    public String readLine(String prompt) {
        if (console != null) {
            return console.readLine(prompt);
        }
        System.out.print(prompt);
//        System.out.flush();
//        if (scanner.hasNextLine()) {
        return scanner.nextLine().trim();
//        }
//        return null;
    }

    @Override
    public void close() {
        if (scanner != null) {
            scanner.close();
        }
    }
}