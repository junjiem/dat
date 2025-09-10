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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
     * @return 用户输入的字符串
     */
    public String readLine(String prompt) {
        return lineReader.readLine(prompt);
    }

    /**
     * 读取用户输入，支持超时（不使用第二个 LineReader，避免竞争 System.in）
     * 简单行读取：支持回车结束、退格删除，字符回显。
     *
     * @param prompt  提示信息（可包含 ANSI 颜色）
     * @param timeout 超时时长
     * @param unit    时间单位
     * @return 用户输入的字符串；超时抛异常TimeoutException
     */
    public String readLineWithTimeout(String prompt, long timeout, TimeUnit unit) throws TimeoutException {
        System.out.print(prompt);
        long timeoutMs = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + timeoutMs;
        StringBuilder sb = new StringBuilder();
        var reader = terminal.reader(); // NonBlockingReader
        try {
            while (true) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    // 超时：不再阻塞读取，返回 null 并换行
                    terminal.writer().println();
                    terminal.flush();
                    throw new TimeoutException("read line timeout: " + timeoutMs + "ms");
                }
                int ch = reader.read(remaining);
                if (ch == -1) {
                    // Ctrl+D (EOF) - 优雅退出
                    terminal.writer().println();
                    terminal.flush();
                    return sb.toString();
                }
                if (ch == -2) {
                    // READ_EXPIRED（非阻塞读取超时片段），继续轮询
                    continue;
                }
                char c = (char) ch;
                // 回车/换行：结束输入
                if (c == '\n' || c == '\r') {
                    terminal.writer().println();
                    terminal.flush();
                    return sb.toString();
                }
                // 退格处理（支持 BS 和 DEL）
                if (c == '\b' || ch == 127) {
                    if (!sb.isEmpty()) {
                        sb.deleteCharAt(sb.length() - 1);
                        // 在终端上回显删除：回退一格、空格覆盖、再回退
                        terminal.writer().print("\b \b");
                        terminal.flush();
                    }
                    continue;
                }
                // 其他可见字符进行累加
                sb.append(c);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read line with timeout", e);
        }
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

    public void println() {
        terminal.writer().println();
        terminal.flush();
    }

    public void println(String input) {
        terminal.writer().println(input);
        terminal.flush();
    }

    public void print(String input) {
        terminal.writer().print(input);
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
                terminal.flush();
                terminal.close();
            }
            log.debug("InputProcessor closed successfully");
        } catch (IOException e) {
            log.warn("Error closing terminal: {}", e.getMessage());
        }
    }
}