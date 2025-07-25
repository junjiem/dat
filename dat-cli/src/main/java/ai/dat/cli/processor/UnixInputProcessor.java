package ai.dat.cli.processor;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Help.Ansi;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * @Author JunjieM
 * @Date 2025/7/23
 */
@Slf4j
public class UnixInputProcessor implements InputProcessor {

    private Scanner scanner;

    public UnixInputProcessor() {
        try {
            scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        } catch (Exception e) {
            scanner = new Scanner(System.in);
        }
    }

    @Override
    public String readLine(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        if (scanner.hasNextLine()) {
            return scanner.nextLine().trim();
        }
        return null;
    }

    @Override
    public void close() {
        if (scanner != null) {
            scanner.close();
        }
    }
}