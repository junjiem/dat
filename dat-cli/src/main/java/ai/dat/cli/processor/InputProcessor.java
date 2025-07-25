package ai.dat.cli.processor;

/**
 * @Author JunjieM
 * @Date 2025/7/23
 */
public interface InputProcessor {
    String readLine(String prompt);

    void close();
}