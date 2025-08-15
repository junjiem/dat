package ai.dat.cli.processor;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * @Author JunjieM
 * @Date 2025/7/23
 */
@Slf4j
public class InputProcessor {

    private final Scanner scanner;

    public InputProcessor() {
        scanner = new Scanner(new InputStreamReader(System.in, StandardCharsets.UTF_8));
//        scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        if (scanner.hasNextLine()) {
            return scanner.nextLine().trim();
        }
        return null;
    }

    public void close() {
        scanner.close();
    }
}