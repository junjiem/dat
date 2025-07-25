package ai.dat.llm.openai;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.ChatModelFactory;
import ai.dat.core.utils.FactoryUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.internal.OpenAiUtils;

import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/7/1
 */
public class OpneAiChatModelFactory implements ChatModelFactory {

    public static final String IDENTIFIER = "openai";

    public static final ConfigOption<String> BASE_URL =
            ConfigOptions.key("base-url")
                    .stringType()
                    .defaultValue(OpenAiUtils.DEFAULT_OPENAI_URL)
                    .withDescription("OpenAI API base URL");

    public static final ConfigOption<String> API_KEY =
            ConfigOptions.key("api-key")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("OpenAI API KEY");

    public static final ConfigOption<String> MODEL_NAME =
            ConfigOptions.key("model-name")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("OpenAI LLM model name");

    public static final ConfigOption<Double> TEMPERATURE =
            ConfigOptions.key("temperature")
                    .doubleType()
                    .noDefaultValue()
                    .withDescription("OpenAI LLM model temperature");

    public static final ConfigOption<Duration> TIMEOUT =
            ConfigOptions.key("timeout")
                    .durationType()
                    .noDefaultValue()
                    .withDescription("OpenAI LLM model timeout");

    public static final ConfigOption<Integer> MAX_RETRIES =
            ConfigOptions.key("max-retries")
                    .intType()
                    .defaultValue(2)
                    .withDescription("OpenAI LLM model maximum retries");

    public static final ConfigOption<Integer> MAX_TOKENS =
            ConfigOptions.key("max-tokens")
                    .intType()
                    .defaultValue(4096)
                    .withDescription("OpenAI LLM model maximum tokens");

    public static final ConfigOption<Integer> MAX_COMPLETION_TOKENS =
            ConfigOptions.key("max-completion-tokens")
                    .intType()
                    .defaultValue(4096)
                    .withDescription("OpenAI LLM model maximum completion tokens");

    public static final ConfigOption<Boolean> LOG_REQUESTS =
            ConfigOptions.key("log-requests")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to print the LLM requests log");

    public static final ConfigOption<Boolean> LOG_RESPONSES =
            ConfigOptions.key("log-responses")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to print the LLM responses log");

    public static final ConfigOption<String> RESPONSE_FORMAT =
            ConfigOptions.key("response-format")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("OpenAI LLM response format. For example: json_schema");

    public static final ConfigOption<Boolean> STRICT_JSON_SCHEMA =
            ConfigOptions.key("strict-json-schema")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to output strict json schema");

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return new LinkedHashSet<>(List.of(BASE_URL, MODEL_NAME, API_KEY));
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(TEMPERATURE, TIMEOUT,
                MAX_RETRIES, MAX_TOKENS, MAX_COMPLETION_TOKENS,
                LOG_REQUESTS, LOG_RESPONSES, RESPONSE_FORMAT, STRICT_JSON_SCHEMA));
    }

    @Override
    public ChatModel create(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);

        String modelName = config.get(MODEL_NAME);
        Boolean logRequests = config.get(LOG_REQUESTS);
        Boolean logResponses = config.get(LOG_RESPONSES);
        Boolean strictJsonSchema = config.get(STRICT_JSON_SCHEMA);

        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .modelName(modelName)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .strictJsonSchema(strictJsonSchema);

        config.getOptional(BASE_URL).ifPresent(builder::baseUrl);
        config.getOptional(API_KEY).ifPresent(builder::apiKey);
        config.getOptional(TEMPERATURE).ifPresent(builder::temperature);
        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(MAX_RETRIES).ifPresent(builder::maxRetries);
        config.getOptional(MAX_TOKENS).ifPresent(builder::maxTokens);
        config.getOptional(MAX_COMPLETION_TOKENS).ifPresent(builder::maxCompletionTokens);
        config.getOptional(RESPONSE_FORMAT).ifPresent(builder::responseFormat);

        return builder.build();
    }

    @Override
    public StreamingChatModel createStream(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);

        String modelName = config.get(MODEL_NAME);
        Boolean logRequests = config.get(LOG_REQUESTS);
        Boolean logResponses = config.get(LOG_RESPONSES);
        Boolean strictJsonSchema = config.get(STRICT_JSON_SCHEMA);

        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .modelName(modelName)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .strictJsonSchema(strictJsonSchema);

        config.getOptional(BASE_URL).ifPresent(builder::baseUrl);
        config.getOptional(API_KEY).ifPresent(builder::apiKey);
        config.getOptional(TEMPERATURE).ifPresent(builder::temperature);
        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(MAX_TOKENS).ifPresent(builder::maxTokens);
        config.getOptional(RESPONSE_FORMAT).ifPresent(builder::responseFormat);

        return builder.build();
    }
}
