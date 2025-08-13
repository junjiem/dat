package ai.dat.cli.commands;

import ai.dat.boot.ProjectRunner;
import ai.dat.cli.processor.InputProcessor;
import ai.dat.cli.utils.AnsiUtil;
import ai.dat.cli.utils.TablePrinter;
import ai.dat.core.agent.data.StreamAction;
import ai.dat.core.agent.data.StreamEvent;
import ai.dat.core.contentstore.data.QuestionSqlPair;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Run project commands
 *
 * @Author JunjieM
 * @Date 2025/7/22
 */
@Command(
        name = "run",
        description = "Run DAT project and start interactive Q&A"
)
@Slf4j
public class RunCommand implements Callable<Integer> {

    private final static ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final InputProcessor PROCESSOR = new InputProcessor();

    private final static String NOT_GENERATE = "<not generate>";

    // the keys whose values should be highlighted
    private static final String[] EXCEPTION_KEYS =
            new String[]{
                    "warning",
                    "warn",
                    "error",
                    "exception",
            };

    private final List<QuestionSqlPair> histories = new ArrayList<>();

    @Option(names = {"-p", "--project-path"},
            description = "Project path (default: current directory)",
            defaultValue = ".")
    private String projectPath;

    @Option(names = {"-a", "--agent"},
            description = "Agent name (default: default)",
            defaultValue = "default")
    private String agentName;

    @Override
    public Integer call() {
        try {
            Path path = Paths.get(projectPath).toAbsolutePath();
            log.info("Run project: {}, Agent: {}", path, agentName);
            ProjectRunner runner = new ProjectRunner(path, agentName);
            // 交互模式
            System.out.println("🤖 DAT interaction mode has been activated");
            System.out.println(AnsiUtil.string(
                    "💡 Enter the question to start the conversation. " +
                            "@|fg(red) Enter 'quit' or 'exit' to exit|@"));
            System.out.println("📁 Project path: " + path);
            System.out.println("🤖 Agent: " + agentName);
            int round = 1;
            while (true) {
                System.out.println(AnsiUtil.string("@|fg(green) "
                        + ("─".repeat(50)) + "|@ @|bold,fg(yellow) Round " + round
                        + "|@ @|fg(green) " + ("─".repeat(50)) + "|@"));
                String question = PROCESSOR.readLine(AnsiUtil.string(
                        "@|fg(yellow) ❓ Please enter the question:|@ "));
                if (question == null || question.isEmpty()) {
                    continue;
                }
                System.out.println("Question: [" + question + "]");
                if ("quit".equalsIgnoreCase(question) || "exit".equalsIgnoreCase(question)) {
                    System.out.println("👋 Bye!");
                    break;
                }
                System.out.println("🤖 Dealing with ask...");
                StreamAction action = runner.ask(question, histories);
                print(runner, question, action);
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

    public void print(ProjectRunner runner, String question, StreamAction action) {
        String sql = NOT_GENERATE;
        String lastEvent = "";
        boolean lastIncremental = false;
        boolean isAccurateSql = false;
        for (StreamEvent event : action) {
            if (event == null) break;
            String eventName = event.name();
            if (event.getSemanticSql().isPresent()) {
                sql = event.getSemanticSql().get();
            }
            if (!lastEvent.equals(eventName)) {
                if (lastIncremental) System.out.println();
                lastEvent = eventName;
                lastIncremental = event.getIncrementalContent().isPresent();
                String color = isException(eventName) ? "red" : "blue";
                if (event.getHitlAiRequest().isPresent()) color = "magenta";
                System.out.println(AnsiUtil.string(
                        "--------------------- @|bold,underline,fg(" + color + ") "
                                + eventName + "|@ ---------------------"));
            }
            if (event.getQueryData().isPresent()) {
                isAccurateSql = true;
            }
            print(runner, event);
        }
        if (lastIncremental) System.out.println();
        if (!isAccurateSql && !NOT_GENERATE.equals(sql)) {
            sql = "/* Incorrect SQL */ " + sql;
        }
        histories.add(QuestionSqlPair.from(question, sql));
    }

    private void print(ProjectRunner runner, StreamEvent event) {
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
        event.getHitlAiRequest().ifPresent(request -> {
            System.out.println(AnsiUtil.string("@|fg(magenta) 🤖 AI: " + request + "|@"));
            while (true) {
                String response = PROCESSOR.readLine(AnsiUtil.string("@|fg(yellow) 👨 > |@ "));
                if (response.isEmpty()) continue;
                runner.userResponse(response);
                return;
            }
        });
        event.getMessages().forEach((k, v) -> print(event, k, v));
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

    public static boolean isException(String key) {
        Preconditions.checkNotNull(key, "key is null");
        final String keyInLower = key.toLowerCase();
        for (String eKey : EXCEPTION_KEYS) {
            if (keyInLower.length() >= eKey.length() && keyInLower.contains(eKey)) {
                return true;
            }
        }
        return false;
    }
}