package ai.dat.agent.agentic.tools.mcp;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.exception.ValidationException;
import ai.dat.core.utils.FactoryUtil;
import com.google.common.base.Preconditions;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;

import java.time.Duration;
import java.util.*;

/**
 * @Author JunjieM
 * @Date 2025/8/12
 */
public class McpTransportFactory {

    private static final ConfigOption<McpTransportType> TRANSPORT =
            ConfigOptions.key("transport")
                    .enumType(McpTransportType.class)
                    .noDefaultValue()
                    .withDescription("MCP transport type. Supported: `stdio`, `http`.");

    //------------------------------- stdio -------------------------------

    private static final ConfigOption<List<String>> COMMAND =
            ConfigOptions.key("command")
                    .stringType()
                    .asList()
                    .noDefaultValue()
                    .withDescription("Stdio command list. " +
                            "For example: [\"/usr/bin/npm\", \"exec\", \"@modelcontextprotocol/server-everything@0.6.2\"]");

    private static final ConfigOption<Map<String, String>> ENVIRONMENT =
            ConfigOptions.key("environment")
                    .mapType()
                    .noDefaultValue()
                    .withDescription("Stdio environment configuration. " +
                            "For example: {\"key1\": \"value1\", \"key2\": \"value2\"}");

    private static final ConfigOption<Boolean> LOG_EVENTS =
            ConfigOptions.key("log-events")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Only if you want to see the traffic in the log");

    //------------------------------- http -------------------------------

    private static final ConfigOption<String> URL =
            ConfigOptions.key("url")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Streamable HTTP url. For example: http://localhost:3002/mcp");

    private static final ConfigOption<String> SSE_URL =
            ConfigOptions.key("sse-url")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("HTTP with SSE url. For example: http://localhost:3001/sse");

    private static final ConfigOption<Map<String, String>> CUSTOM_HEADERS =
            ConfigOptions.key("custom-headers")
                    .mapType()
                    .noDefaultValue()
                    .withDescription("Custom HTTP headers. " +
                            "For example: {\"content-type\": \"application/json\", " +
                            "\"accept\": \"application/json, text/event-stream\"}");

    private static final ConfigOption<Duration> TIMEOUT =
            ConfigOptions.key("timeout")
                    .durationType()
                    .defaultValue(Duration.ofSeconds(60))
                    .withDescription("Http timeout");

    private static final ConfigOption<Boolean> LOG_REQUESTS =
            ConfigOptions.key("log-requests")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("If you want to see the traffic in the log");

    private static final ConfigOption<Boolean> LOG_RESPONSES =
            ConfigOptions.key("log-responses")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("If you want to see the traffic in the log");

    private Set<ConfigOption<?>> requiredOptions() {
        return new LinkedHashSet<>(List.of(TRANSPORT));
    }

    private Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(
                // STDIO
                COMMAND, ENVIRONMENT, LOG_EVENTS,
                // HTTP
                URL, SSE_URL, CUSTOM_HEADERS, TIMEOUT, LOG_REQUESTS, LOG_RESPONSES
        ));
    }

    public McpTransport create(ReadableConfig config) {
        validateConfigOptions(config);
        McpTransportType transport = config.get(TRANSPORT);
        if (McpTransportType.STDIO == transport) {
            List<String> command = config.get(COMMAND);
            Boolean logEvents = config.get(LOG_EVENTS);
            StdioMcpTransport.Builder builder = new StdioMcpTransport.Builder()
                    .command(command)
                    .logEvents(logEvents);
            config.getOptional(ENVIRONMENT).ifPresent(builder::environment);
            return builder.build();
        } else if (McpTransportType.HTTP == transport) {
            Optional<String> urlOptional = config.getOptional(URL);
            Optional<String> sseUrlOptional = config.getOptional(SSE_URL);
            Duration timeout = config.get(TIMEOUT);
            Boolean logRequests = config.get(LOG_REQUESTS);
            Boolean logResponses = config.get(LOG_RESPONSES);
            if (urlOptional.isPresent()) { // Streamable HTTP
                StreamableHttpMcpTransport.Builder builder = new StreamableHttpMcpTransport.Builder()
                        .url(urlOptional.get())
                        .timeout(timeout)
                        .logRequests(logRequests)
                        .logResponses(logResponses);
                config.getOptional(CUSTOM_HEADERS).ifPresent(builder::customHeaders);
                return builder.build();
            } else if (sseUrlOptional.isPresent()) { // HTTP with SSE
                HttpMcpTransport.Builder builder = new HttpMcpTransport.Builder()
                        .sseUrl(sseUrlOptional.get())
                        .timeout(timeout)
                        .logRequests(logRequests)
                        .logResponses(logResponses);
                config.getOptional(CUSTOM_HEADERS).ifPresent(builder::customHeaders);
                return builder.build();
            } else {
                throw new IllegalArgumentException("'" + URL.key() + "' or '" + SSE_URL.key()
                        + "' is required in `http` transport");
            }
        } else {
            throw new UnsupportedOperationException("Not supported yet: " + transport);
        }
    }

    private void validateConfigOptions(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(requiredOptions(), optionalOptions(), config);
        McpTransportType transport = config.get(TRANSPORT);
        if (McpTransportType.STDIO == transport) {
            Preconditions.checkArgument(config.getOptional(COMMAND).isPresent(),
                    "'" + COMMAND.key() + "' is required in `stdio` transport");
        } else if (McpTransportType.HTTP == transport) {
            Preconditions.checkArgument(
                    config.getOptional(URL).isPresent() || config.getOptional(SSE_URL).isPresent(),
                    "'" + URL.key() + "' or '" + SSE_URL.key() + "' is required in `http` transport");
        } else {
            throw new ValidationException("Not support transport: " + transport);
        }
    }

}
