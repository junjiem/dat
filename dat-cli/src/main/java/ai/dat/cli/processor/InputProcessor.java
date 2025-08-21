package ai.dat.cli.processor;

import lombok.extern.slf4j.Slf4j;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JLine 3 交互式输入处理器
 * 提供基本的命令行交互功能，包括历史记录等
 *
 * @Author JunjieM
 * @Date 2025/7/23
 */
@Slf4j
public class InputProcessor implements AutoCloseable {
    private final Terminal terminal;
    private final LineReader lineReader;

    /**
     * 默认构造函数，使用内存历史记录
     */
    public InputProcessor() {
        this(null);
    }

    /**
     * 构造函数，支持自定义历史文件路径
     *
     * @param historyFilePath 历史文件路径，如果为null则使用内存历史记录
     */
    public InputProcessor(Path historyFilePath) {
        try {
            // 创建终端实例
            this.terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();
            // 创建 LineReader 构建器
            LineReaderBuilder builder = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .variable(LineReader.HISTORY_SIZE, 1000)
                    .option(LineReader.Option.CASE_INSENSITIVE_SEARCH, true);
            // 如果指定了历史文件路径，配置文件历史记录
            if (historyFilePath != null) {
                // 确保历史文件的父目录存在
                Path parentDir = historyFilePath.getParent();
                if (parentDir != null && !Files.exists(parentDir)) {
                    try {
                        Files.createDirectories(parentDir);
                    } catch (IOException e) {
                        log.warn("Failed to create history file parent directory: {}, using memory history instead",
                                parentDir, e);
                        historyFilePath = null;
                    }
                }
                if (historyFilePath != null) {
                    // 使用文件历史记录
                    builder.variable(LineReader.HISTORY_FILE, historyFilePath);
                } else {
                    // 使用内存历史记录
                    builder.history(new DefaultHistory());
                }
            } else {
                // 使用内存历史记录
                builder.history(new DefaultHistory());
            }
            // 创建 LineReader 实例
            this.lineReader = builder.build();
        } catch (IOException e) {
            log.error("Failed to initialize JLine 3 terminal", e);
            throw new RuntimeException("Failed to initialize terminal", e);
        }
    }


    /**
     * 读取用户输入
     *
     * @param prompt 提示信息
     * @return 用户输入的字符串，去除前后空格；特殊值：null表示EOF(Ctrl+D)，EXIT_SIGNAL表示中断(Ctrl+C)
     */
    public String readLine(String prompt) {
        return lineReader.readLine(prompt);
    }

    /**
     * 读取用户输入，支持默认值
     *
     * @param prompt       提示信息
     * @param defaultValue 默认值（当用户直接按回车时使用）
     * @return 用户输入的字符串或默认值
     */
    public String readLine(String prompt, String defaultValue) {
        String input = lineReader.readLine(prompt);
        return (input == null || input.isEmpty()) ? defaultValue : input;
    }

    /**
     * 读取密码输入（不显示在终端上）
     *
     * @param prompt 提示信息
     * @return 密码字符串
     */
    public String readPassword(String prompt) {
        return lineReader.readLine(prompt, '*');
    }

    /**
     * 清屏
     */
    public void clearScreen() {
        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.flush();
    }

    /**
     * 打印彩色文本
     *
     * @param text  文本内容
     * @param color ANSI 颜色代码
     */
    public void printColored(String text, String color) {
        terminal.writer().println(String.format("\u001B[%sm%s\u001B[0m", color, text));
        terminal.flush();
    }

    /**
     * 保存历史记录到文件
     * 当使用HISTORY_FILE配置时，JLine会自动保存历史记录
     */
    public void saveHistory() {
        try {
            // 对于使用HISTORY_FILE配置的情况，JLine会自动保存历史记录
            // 这里主要用于手动触发保存（如果需要的话）
            lineReader.getHistory().save();
            log.debug("History saved successfully");
        } catch (IOException e) {
            log.warn("Failed to save history: {}", e.getMessage());
        } catch (Exception e) {
            // 某些情况下save()方法可能不可用，忽略错误
            log.debug("History save not available or failed: {}", e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            // 保存历史记录
            saveHistory();

            if (terminal != null) {
                terminal.close();
            }
            log.debug("InputProcessor closed successfully");
        } catch (IOException e) {
            log.warn("Error closing terminal: {}", e.getMessage());
        }
    }
}