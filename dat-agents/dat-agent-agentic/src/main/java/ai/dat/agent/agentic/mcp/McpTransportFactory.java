package ai.dat.agent.agentic.mcp;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.exception.ValidationException;
import ai.dat.core.utils.FactoryUtil;
import com.google.common.base.Preconditions;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/8/12
 */
public class McpTransportFactory {

    public static final ConfigOption<McpTransportType> TRANSPORT =
            ConfigOptions.key("transport")
                    .enumType(McpTransportType.class)
                    .noDefaultValue()
                    .withDescription("MCP transport type. Supported: `stdio`, `http`.");

    //------------------------------- stdio -------------------------------

    public static final ConfigOption<List<String>> COMMAND =
            ConfigOptions.key("command")
                    .stringType()
                    .asList()
                    .noDefaultValue()
                    .withDescription("Stdio command list. " +
                            "For example: [\"/usr/bin/npm\", \"exec\", \"@modelcontextprotocol/server-everything@0.6.2\"]");

    public static final ConfigOption<Boolean> LOG_EVENTS =
            ConfigOptions.key("log-events")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Only if you want to see the traffic in the log");

    //------------------------------- http -------------------------------

    public static final ConfigOption<String> SSE_URL =
            ConfigOptions.key("sse-url")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Http SSE url. For example: http://localhost:3001/sse");

    public static final ConfigOption<Duration> TIMEOUT =
            ConfigOptions.key("timeout")
                    .durationType()
                    .defaultValue(Duration.ofSeconds(60))
                    .withDescription("Http timeout");

    public static final ConfigOption<Boolean> LOG_REQUESTS =
            ConfigOptions.key("log-requests")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("If you want to see the traffic in the log");

    public static final ConfigOption<Boolean> LOG_RESPONSES =
            ConfigOptions.key("log-responses")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("If you want to see the traffic in the log");

    private Set<ConfigOption<?>> requiredOptions() {
        return new LinkedHashSet<>(List.of(TRANSPORT));
    }

    private Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(
                COMMAND, LOG_EVENTS, SSE_URL, TIMEOUT, LOG_REQUESTS, LOG_RESPONSES
        ));
    }

    public McpTransport create(ReadableConfig config) {
        validateConfigOptions(config);
        McpTransportType transport = config.get(TRANSPORT);
        if (McpTransportType.STDIO == transport) {
            List<String> command = config.get(COMMAND);
            Boolean logEvents = config.get(LOG_EVENTS);
            return new StdioMcpTransport.Builder()
                    .command(command)
                    .logEvents(logEvents)
                    .build();
        } else {
            String sseUrl = config.get(SSE_URL);
            Duration timeout = config.get(TIMEOUT);
            Boolean logRequests = config.get(LOG_REQUESTS);
            Boolean logResponses = config.get(LOG_RESPONSES);
            return new HttpMcpTransport.Builder()
                    .sseUrl(sseUrl)
                    .timeout(timeout)
                    .logRequests(logRequests)
                    .logResponses(logResponses)
                    .build();
        }
    }

    private void validateConfigOptions(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(requiredOptions(), optionalOptions(), config);
        McpTransportType transport = config.get(TRANSPORT);
        if (McpTransportType.STDIO == transport) {
            Preconditions.checkArgument(config.getOptional(COMMAND).isPresent(),
                    "'" + COMMAND.key() + "' is required in `stdio` transport");
        } else if (McpTransportType.HTTP == transport) {
            Preconditions.checkArgument(config.getOptional(SSE_URL).isPresent(),
                    "'" + SSE_URL.key() + "' is required in `http` transport");
        } else {
            throw new ValidationException("Not support transport: " + transport);
        }
    }

}
