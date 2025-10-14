package ai.dat.cli.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JLine 3 交互式输入处理器
 * 提供基本的命令行交互功能，包括历史记录等
 *
 * @Author JunjieM
 * @Date 2025/7/23
 */
@Slf4j
public class InputProcessor implements AutoCloseable {

    private static final String[] SPINNER_FRAMES = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    private final AtomicBoolean spinnerRunning = new AtomicBoolean(false);
    private Thread spinnerThread;

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
     * 开始等待指示器
     */
    public void startSpinner() {
        startSpinner(null);
    }

    /**
     * 开始等待指示器
     *
     * @param message
     */
    public void startSpinner(String message) {
        if (spinnerRunning.get()) {
            return;
        }

        String label = StringUtils.isNotBlank(message) ? message + " " : "";

        spinnerRunning.set(true);
        spinnerThread = new Thread(() -> {
            int i = 0;
            try {
                // 隐藏光标
                terminal.writer().print("\033[?25l");
                terminal.flush();

                while (spinnerRunning.get()) {
                    String frame = SPINNER_FRAMES[i % SPINNER_FRAMES.length];
                    String coloredFrame = "\u001B[36m" + frame + "\u001B[0m"; // 青色
                    String output = "\r" + label + coloredFrame;

                    terminal.writer().print(output);
                    terminal.flush();

                    i++;
                    Thread.sleep(80);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        spinnerThread.setDaemon(true);
        spinnerThread.start();
    }

    /**
     * 停止等待指示器
     */
    public void stopSpinner() {
        if (!spinnerRunning.getAndSet(false)) {
            return;
        }

        try {
            if (spinnerThread != null) {
                spinnerThread.join(1000);
            }
            // 清除行并恢复光标
            terminal.writer().print("\r\033[K\033[?25h");
            terminal.flush();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            spinnerThread = null;
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
     * 读取列表选择
     *
     * @param prompt
     * @param options
     * @return
     */
    public String readSelect(String prompt, List<String> options) {
        return readSelect(prompt, options, null, true);
    }

    /**
     * 读取列表选择，支持默认选项
     *
     * @param prompt
     * @param options
     * @param defaultOption
     * @return
     */
    public String readSelect(String prompt, List<String> options, String defaultOption) {
        return readSelect(prompt, options, defaultOption, true);
    }

    /**
     * 读取列表选择
     *
     * @param prompt
     * @param options
     * @param displayIndex
     * @return
     */
    public String readSelect(String prompt, List<String> options, boolean displayIndex) {
        return readSelect(prompt, options, null, displayIndex);
    }

    /**
     * 读取列表选择
     *
     * @param prompt
     * @param options
     * @param defaultOption
     * @param displayIndex
     * @return
     */
    public String readSelect(String prompt, List<String> options, String defaultOption, Boolean displayIndex) {
        boolean display = Optional.ofNullable(displayIndex).orElse(false);

        int currentSelection = 0;
        boolean selected = false;
        int defaultIndex = -1;

        if (StringUtils.isNotBlank(defaultOption)) {
            defaultIndex = options.indexOf(defaultOption);
            if (defaultIndex >= 0) {
                currentSelection = defaultIndex;
            }
        }

        try {
            // 保存原始终端属性
            Attributes originalAttributes = terminal.enterRawMode();
            terminal.enterRawMode();

            try {
                while (!selected) {
                    // 清屏并显示选项
                    terminal.writer().println("\033[2J\033[H"); // 清屏指令
                    terminal.writer().println(prompt);

                    for (int i = 0; i < options.size(); i++) {
                        String option = (display ? (i + 1) + ". " : "") + options.get(i);
                        if (defaultIndex >= 0 && i == defaultIndex) {
                            option += " (Default)";
                        }
                        if (i == currentSelection) {
                            terminal.writer().println("> \033[1;32m" + option + "\033[0m"); // 绿色高亮
                        } else {
                            terminal.writer().println("  " + option);
                        }
                    }
                    terminal.flush();

                    // 读取键盘输入
                    int key = terminal.reader().read();
                    if (key == 13 || key == 10) { // Enter键或Ctrl+Enter键
                        selected = true;
                    } else if (key == 65 || key == 'k') { // 上箭头或k键
                        currentSelection = Math.max(0, currentSelection - 1);
                    } else if (key == 66 || key == 'j') { // 下箭头或j键
                        currentSelection = Math.min(options.size() - 1, currentSelection + 1);
                    } else if (key == 3) { // Ctrl+C
                        throw new UserInterruptException("User interrupt received (Ctrl+C)");
                    }
                }
            } finally {
                terminal.setAttributes(originalAttributes);
            }

            return options.get(currentSelection);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read select", e);
        }
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