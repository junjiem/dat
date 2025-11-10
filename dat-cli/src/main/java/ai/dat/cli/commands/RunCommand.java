package ai.dat.cli.commands;

import ai.dat.boot.ProjectRunner;
import ai.dat.boot.utils.ProjectUtil;
import ai.dat.boot.utils.QuestionSqlPairCacheUtil;
import ai.dat.cli.processor.InputProcessor;
import ai.dat.cli.provider.VersionProvider;
import ai.dat.cli.utils.AnsiUtil;
import ai.dat.cli.utils.TablePrinter;
import ai.dat.core.agent.data.StreamAction;
import ai.dat.core.agent.data.StreamEvent;
import ai.dat.core.contentstore.data.QuestionSqlPair;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Run project commands
 *
 * @Author JunjieM
 * @Date 2025/7/22
 */
@Command(
        name = "run",
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class,
        description = "Run DAT project and start interactive Q&A"
)
@Slf4j
public class RunCommand implements Callable<Integer> {

    private final static ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final static String NOT_GENERATE = "<not generate>";
    private final static String RUN_COMMAND_HISTORY = "run_command_history";

    private final static String CONVERSATION_ID = UUID.randomUUID().toString();

    // the keys whose values should be highlighted
    private static final String[] EXCEPTION_KEYS =
            new String[]{
                    "warning",
                    "warn",
                    "error",
                    "exception",
            };

    @Option(names = {"-p", "--project-path"},
            description = "Project path (default: current directory)",
            defaultValue = ".")
    private String projectPath;

    @Option(names = {"-a", "--agent"},
            description = "Agent name (default: default)",
            defaultValue = "default")
    private String agentName;

    @Option(names = {"-var", "--variable"},
            arity = "1..*",
            description = "Dynamic variable, key-value pairs in format key=value")
    private Map<String, Object> variables;

    @Override
    public Integer call() {
        Path path = Paths.get(projectPath).toAbsolutePath();
        log.info("Run project: {}, Agent: {}", path, agentName);
        System.out.println("📁 Project path: " + path);
        System.out.println("🤖 Agent: " + agentName);
        System.out.println("🛠️ Dynamic variables: " + variables);
        System.out.println("🆔 Conversation ID: " + CONVERSATION_ID);
        Path historyFilePath = path.resolve(ProjectUtil.DAT_DIR_NAME + "/" + RUN_COMMAND_HISTORY);
        try (InputProcessor processor = new InputProcessor(historyFilePath)) {
            ProjectRunner runner = new ProjectRunner(path, agentName, variables);
            printHelp(); // 打印帮助信息
            int round = 1;
            while (true) {
                System.out.println(AnsiUtil.string("@|fg(green) "
                        + ("─".repeat(50)) + "|@ @|bold,fg(yellow) Round " + round
                        + "|@ @|fg(green) " + ("─".repeat(50)) + "|@"));
                String question;
                try {
                    question = processor.readLine(AnsiUtil.string(
                            "@|fg(yellow) ❓ Please enter the question:|@ "));
                } catch (EndOfFileException e) {
                    // Ctrl+D (EOF) - 优雅退出
                    log.debug("EOF received (Ctrl+D)");
                    System.out.println("👋 Bye!");
                    break;
                } catch (UserInterruptException e) {
                    // Ctrl+C - 中断信号
                    log.debug("User interrupt received (Ctrl+C)");
                    System.out.println("👋 Bye!");
                    break;
                }
                if (question.isEmpty()) {
                    continue;
                }
                if ("quit".equalsIgnoreCase(question) || "exit".equalsIgnoreCase(question)) {
                    System.out.println("👋 Bye!");
                    break;
                }
                if ("clear".equalsIgnoreCase(question)) {
                    processor.clearScreen();
                    continue;
                }
                if ("help".equalsIgnoreCase(question)) {
                    printHelp();
                    continue;
                }
                System.out.println("Question: [" + question + "]");
                System.out.println("🤖 Dealing with ask...");
                List<QuestionSqlPair> histories = QuestionSqlPairCacheUtil.get(CONVERSATION_ID);
                StreamAction action = runner.ask(question, histories);
                print(processor, runner, question, action);
                round += 1;
            }
            System.out.println(AnsiUtil.string("@|fg(green) " + ("─".repeat(100)) + "|@"));
            return 0;
        } catch (Exception e) {
            log.error("Run project failed", e);
            System.err.println(AnsiUtil.string(
                    "@|fg(red) ❌ Run failed: " + e.getMessage() + "|@"));
            return 1;
        }
    }

    public void print(InputProcessor processor, ProjectRunner runner, String question, StreamAction action) {
        String sql = NOT_GENERATE;
        String previousEvent = "";
        boolean previousIncremental = false;
        boolean isAccurateSql = false;
        processor.startSpinner(); // 开始等待指示器
        for (StreamEvent event : action) {
            processor.stopSpinner(); // 停止等待指示器
            if (event == null) break;
            String eventName = event.name();
            if (event.getSemanticSql().isPresent()) {
                sql = event.getSemanticSql().get();
            }
            if (event.getQueryData().isPresent()) {
                isAccurateSql = true;
            }
            if (!previousEvent.equals(eventName)) {
                if (previousIncremental) System.out.println();
                previousEvent = eventName;
                previousIncremental = event.getIncrementalContent().isPresent();
                String color = isException(eventName) ? "red" : "blue";
                if (event.getHitlAiRequest().isPresent()) color = "magenta";
                if (event.getHitlToolApproval().isPresent()) color = "yellow";
                System.out.println(AnsiUtil.string(
                        "--------------------- @|bold,underline,fg(" + color + ") "
                                + eventName + "|@ ---------------------"));
            }
            print(processor, runner, event);
            // 非流式增量内容
            if (event.getIncrementalContent().isEmpty()) {
                processor.startSpinner(); // 开始等待指示器
            }
        }
        // 非流式增量内容
        if (!previousIncremental) {
            processor.stopSpinner(); // 停止等待指示器
        }
        if (previousIncremental) System.out.println();
        if (!isAccurateSql && !NOT_GENERATE.equals(sql)) {
            sql = "/* Incorrect SQL */ " + sql;
        }
        QuestionSqlPairCacheUtil.add(CONVERSATION_ID, QuestionSqlPair.from(question, sql));
    }

    private void print(InputProcessor processor, ProjectRunner runner, StreamEvent event) {
        event.getIncrementalContent().ifPresent(content ->
                System.out.print(AnsiUtil.string("@|fg(blue) " + content + "|@")));
        event.getSemanticSql().ifPresent(content ->
                System.out.println(AnsiUtil.string("@|fg(blue) " + content + "|@")));
        event.getQuerySql().ifPresent(content ->
                System.out.println(AnsiUtil.string("@|fg(blue) " + content + "|@")));
        event.getQueryData().ifPresent(data -> {
            System.out.println(AnsiUtil.string("@|fg(cyan) 📊 Query Results:|@"));
            TablePrinter.printTable(data);
        });
        event.getToolExecutionRequest().ifPresent(request ->
                System.out.println(AnsiUtil.string("@|fg(blue) id: " + request.id()
                        + "\nname: " + request.name() + "\narguments: " + request.arguments() + "|@")));
        event.getToolExecutionResult().ifPresent(result ->
                System.out.println(AnsiUtil.string("@|fg(blue) result: " + result + "|@")));
        event.getHitlAiRequest().ifPresent(request ->
                printHitlAiRequest(request, processor, runner, event.getHitlWaitTimeout().orElse(null)));
        event.getHitlToolApproval().ifPresent(request ->
                printHitlToolApproval(request, processor, runner, event.getHitlWaitTimeout().orElse(null)));
        event.getMessages().forEach((k, v) -> print(event, k, v));
    }

    private void printHitlAiRequest(String request, InputProcessor processor, ProjectRunner runner, Long timeout) {
        System.out.println(AnsiUtil.string("@|fg(magenta) 🤖 AI: " + request + "|@"));
        String response;
        if (timeout != null) {
            System.out.println(AnsiUtil.string(
                    "@|fg(red) ⏰ The timeout period for waiting for user input: " + timeout + "s|@"));
            try {
                response = processor.readLineWithTimeout(AnsiUtil.string("@|fg(yellow) 👨 >|@ "),
                        timeout, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.debug("Read line timeout after {} s", timeout);
                response = null;
                System.out.println(AnsiUtil.string(
                        "@|fg(red) ⏰ " + timeout + "s wait timeout, skip user input.|@"));
            }
        } else {
            response = processor.readLine(AnsiUtil.string("@|fg(yellow) 👨 >|@ "));
        }
        if (response != null) {
            runner.userResponse(response);
        }
    }

    private void printHitlToolApproval(String request, InputProcessor processor, ProjectRunner runner, Long timeout) {
        System.out.println(AnsiUtil.string("@|fg(yellow) ⚠️ " + request + "|@"));
        String prompt = "(y/n) [User input/press Enter to use the y]:";
        String response;
        if (timeout != null) {
            System.out.println(AnsiUtil.string(
                    "@|fg(red) ⏰ The timeout period for waiting for user input: " + timeout + "s|@"));
            try {
                response = processor.readLineWithTimeout(AnsiUtil.string("@|fg(yellow) 👨 " + prompt + "|@ "),
                        timeout, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.debug("Read line timeout after {} s", timeout);
                response = null;
                System.out.println(AnsiUtil.string(
                        "@|fg(red) ⏰ " + timeout + "s wait timeout, approved by default.|@"));
            }
        } else {
            response = processor.readLine(AnsiUtil.string("@|fg(yellow) 👨 " + prompt + "|@ "));
        }
        boolean approval = response == null || response.equalsIgnoreCase("y") || response.isEmpty();
        runner.userApproval(approval);
    }

    private void print(StreamEvent event, String key, Object value) {
        String valueStr;
        if (value instanceof String) {
            valueStr = (String) value;
        } else {
            try {
                valueStr = JSON_MAPPER.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize " + key + " message to JSON: "
                        + e.getMessage(), e);
            }
        }
        String fg = isException(event.name()) || isException(key) ? "red" : "blue";
        System.out.println(AnsiUtil.string("@|fg(" + fg + ") " + key + ": " + valueStr + "|@"));
    }

    public static boolean isException(@NonNull String key) {
        final String keyInLower = key.toLowerCase();
        for (String eKey : EXCEPTION_KEYS) {
            if (keyInLower.length() >= eKey.length() && keyInLower.contains(eKey)) {
                return true;
            }
        }
        return false;
    }

    private void printHelp() {
        System.out.println();
        System.out.println(AnsiUtil.string("@|bold,fg(cyan) 📖 DAT Interactive Help|@"));
        System.out.println();
        System.out.println(AnsiUtil.string("@|bold Commands:|@"));
        System.out.println(AnsiUtil.string("  @|fg(green) help|@         - Show this help message"));
        System.out.println(AnsiUtil.string("  @|fg(green) clear|@        - Clear the screen"));
        System.out.println(AnsiUtil.string("  @|fg(green) quit/exit|@    - Exit the conversation"));
        System.out.println();
        System.out.println(AnsiUtil.string("@|bold Keyboard Shortcuts:|@"));
        System.out.println(AnsiUtil.string("  @|fg(yellow) ↑/↓|@         - Navigate command history"));
        System.out.println(AnsiUtil.string("  @|fg(yellow) Home/End|@    - Move to start/end of line"));
        System.out.println(AnsiUtil.string("  @|fg(yellow) Ctrl+A/E|@    - Move to start/end of line"));
        System.out.println(AnsiUtil.string("  @|fg(yellow) Ctrl+C|@      - Interrupt and exit (immediate)"));
        System.out.println(AnsiUtil.string("  @|fg(yellow) Ctrl+D|@      - EOF signal and exit (graceful)"));
        System.out.println(AnsiUtil.string("  @|fg(yellow) Ctrl+L|@      - Clear screen"));
        System.out.println();
        System.out.println(AnsiUtil.string("@|bold Question Examples:|@"));
        System.out.println(AnsiUtil.string("  @|fg(blue) \"count orders by date\"|@"));
        System.out.println(AnsiUtil.string("  @|fg(blue) \"各国的covid病例总数\"|@"));
        System.out.println();
    }
}