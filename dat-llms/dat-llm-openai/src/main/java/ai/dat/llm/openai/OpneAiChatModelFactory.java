package ai.dat.llm.openai;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.ChatModelFactory;
import ai.dat.core.utils.FactoryUtil;
import com.google.common.base.Preconditions;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.internal.OpenAiUtils;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    public static final ConfigOption<Double> TOP_P =
            ConfigOptions.key("top-p")
                    .doubleType()
                    .noDefaultValue()
                    .withDescription("OpenAI LLM model Top-P");

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

    public static final ConfigOption<Integer> SEED =
            ConfigOptions.key("seed")
                    .intType()
                    .noDefaultValue()
                    .withDescription("OpenAI LLM model seed");

    public static final ConfigOption<String> USER =
            ConfigOptions.key("user")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("OpenAI user");

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

    public static final ConfigOption<Boolean> STRICT_TOOLS =
            ConfigOptions.key("strict-tools")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to strict tools");

    public static final ConfigOption<Boolean> RETURN_THINKING =
            ConfigOptions.key("return-thinking")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to return thinking. " +
                            "This setting is intended for DeepSeek Reasoning Model.");

    public static final ConfigOption<Boolean> STORE =
            ConfigOptions.key("store")
                    .booleanType()
                    .noDefaultValue()
                    .withDescription("Whether to store");

    public static final ConfigOption<Boolean> ONLY_SUPPORT_STREAM_OUTPUT =
            ConfigOptions.key("only-support-stream-output")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Is streaming output reply only supported. " +
                            "For example, Qwen3 Business Edition (Thinking Mode), Qwen3 Open Source Edition, QwQ, and QVQ only support streaming output.");

    public static final ConfigOption<Map<String, Object>> CUSTOM_PARAMETERS =
            ConfigOptions.key("custom-parameters")
                    .mapObjectType()
                    .noDefaultValue()
                    .withDescription("Custom parameters, format Map<String, Object>. " +
                            "Alternatively, custom parameters can also be specified as a structure of nested maps.");

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
        return new LinkedHashSet<>(List.of(TEMPERATURE, TOP_P, TIMEOUT,
                MAX_RETRIES, MAX_TOKENS, MAX_COMPLETION_TOKENS,
                LOG_REQUESTS, LOG_RESPONSES, RESPONSE_FORMAT, STRICT_JSON_SCHEMA,
                STRICT_TOOLS, STORE, RETURN_THINKING, SEED, USER,
                ONLY_SUPPORT_STREAM_OUTPUT, CUSTOM_PARAMETERS));
    }

    @Override
    public ChatModel create(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);
        validateConfigOptions(config);

        String modelName = config.get(MODEL_NAME);
        Boolean onlySupportStreamOutput = config.get(ONLY_SUPPORT_STREAM_OUTPUT);

        if (onlySupportStreamOutput) {
            return new OpenAiStreamingToChatModel(createStream(config));
        }

        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .modelName(modelName);

        config.getOptional(BASE_URL).ifPresent(builder::baseUrl);
        config.getOptional(API_KEY).ifPresent(builder::apiKey);
        config.getOptional(TEMPERATURE).ifPresent(builder::temperature);
        config.getOptional(TOP_P).ifPresent(builder::topP);
        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(MAX_RETRIES).ifPresent(builder::maxRetries);
        config.getOptional(MAX_TOKENS).ifPresent(builder::maxTokens);
        config.getOptional(MAX_COMPLETION_TOKENS).ifPresent(builder::maxCompletionTokens);
        config.getOptional(RESPONSE_FORMAT).ifPresent(builder::responseFormat);
        config.getOptional(LOG_REQUESTS).ifPresent(builder::logRequests);
        config.getOptional(LOG_RESPONSES).ifPresent(builder::logResponses);
        config.getOptional(STRICT_JSON_SCHEMA).ifPresent(builder::strictJsonSchema);
        config.getOptional(STRICT_TOOLS).ifPresent(builder::strictTools);
        config.getOptional(STORE).ifPresent(builder::store);
        config.getOptional(RETURN_THINKING).ifPresent(builder::returnThinking);
        config.getOptional(SEED).ifPresent(builder::seed);
        config.getOptional(USER).ifPresent(builder::user);

        config.getOptional(CUSTOM_PARAMETERS)
                .ifPresent(customParameters ->
                        builder.defaultRequestParameters(OpenAiChatRequestParameters.builder()
                                .customParameters(customParameters).build()));

        return builder.build();
    }

    @Override
    public StreamingChatModel createStream(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);
        validateConfigOptions(config);

        String modelName = config.get(MODEL_NAME);

        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder =
                OpenAiStreamingChatModel.builder()
                        .modelName(modelName);

        config.getOptional(BASE_URL).ifPresent(builder::baseUrl);
        config.getOptional(API_KEY).ifPresent(builder::apiKey);
        config.getOptional(TEMPERATURE).ifPresent(builder::temperature);
        config.getOptional(TOP_P).ifPresent(builder::topP);
        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(MAX_TOKENS).ifPresent(builder::maxTokens);
        config.getOptional(RESPONSE_FORMAT).ifPresent(builder::responseFormat);
        config.getOptional(LOG_REQUESTS).ifPresent(builder::logRequests);
        config.getOptional(LOG_RESPONSES).ifPresent(builder::logResponses);
        config.getOptional(STRICT_JSON_SCHEMA).ifPresent(builder::strictJsonSchema);
        config.getOptional(STRICT_TOOLS).ifPresent(builder::strictTools);
        config.getOptional(STORE).ifPresent(builder::store);
        config.getOptional(RETURN_THINKING).ifPresent(builder::returnThinking);
        config.getOptional(SEED).ifPresent(builder::seed);
        config.getOptional(USER).ifPresent(builder::user);

        config.getOptional(CUSTOM_PARAMETERS)
                .ifPresent(customParameters ->
                        builder.defaultRequestParameters(OpenAiChatRequestParameters.builder()
                                .customParameters(customParameters).build()));

        return builder.build();
    }

    private void validateConfigOptions(ReadableConfig config) {
        config.getOptional(TEMPERATURE)
                .ifPresent(t -> Preconditions.checkArgument(t >= 0.0 && t <= 2.0,
                        "'" + TEMPERATURE.key() + "' value must be between 0.0 and 2.0"));
        config.getOptional(TOP_P)
                .ifPresent(v -> Preconditions.checkArgument(v > 0.0 && v <= 1.0,
                        "'" + TOP_P.key() + "' value must > 0.0 and <= 1.0"));
        Integer maxRetries = config.get(MAX_RETRIES);
        Preconditions.checkArgument(maxRetries >= 0,
                "'" + MAX_RETRIES.key() + "' value must be greater than or equal to 0");
        Integer maxTokens = config.get(MAX_TOKENS);
        Preconditions.checkArgument(maxTokens > 0,
                "'" + MAX_TOKENS.key() + "' value must be greater than 0");
        Integer maxCompletionTokens = config.get(MAX_COMPLETION_TOKENS);
        Preconditions.checkArgument(maxCompletionTokens > 0,
                "'" + MAX_COMPLETION_TOKENS.key() + "' value must be greater than 0");
    }

}
