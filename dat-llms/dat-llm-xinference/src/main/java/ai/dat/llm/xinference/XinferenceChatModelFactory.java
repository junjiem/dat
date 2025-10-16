package ai.dat.llm.xinference;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.ChatModelFactory;
import ai.dat.core.utils.FactoryUtil;
import com.google.common.base.Preconditions;
import dev.langchain4j.community.model.xinference.XinferenceChatModel;
import dev.langchain4j.community.model.xinference.XinferenceStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/10/16
 */
public class XinferenceChatModelFactory implements ChatModelFactory {

    public static final String IDENTIFIER = "xinference";

    public static final ConfigOption<String> BASE_URL =
            ConfigOptions.key("base-url")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The base URL of Xinference server.");

    public static final ConfigOption<String> API_KEY =
            ConfigOptions.key("api-key")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The API KEY of Xinference server.");

    public static final ConfigOption<String> MODEL_NAME =
            ConfigOptions.key("model-name")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The name of the model to use from Xinference server.");

    public static final ConfigOption<Double> TEMPERATURE =
            ConfigOptions.key("temperature")
                    .doubleType()
                    .noDefaultValue()
                    .withDescription("Controls the randomness of the generated responses. " +
                            "Higher values (e.g., 1.0) result in more diverse output, " +
                            "while lower values (e.g., 0.2) produce more deterministic responses.");

    public static final ConfigOption<Double> TOP_P =
            ConfigOptions.key("top-p")
                    .doubleType()
                    .noDefaultValue()
                    .withDescription("Controls the diversity of the generated responses by setting a threshold " +
                            "for the cumulative probability of top tokens.");

    public static final ConfigOption<Duration> TIMEOUT =
            ConfigOptions.key("timeout")
                    .durationType()
                    .noDefaultValue()
                    .withDescription("The maximum time allowed for the API call to complete.");

    public static final ConfigOption<Integer> MAX_RETRIES =
            ConfigOptions.key("max-retries")
                    .intType()
                    .defaultValue(2)
                    .withDescription("The maximum number of retries in case of API call failure.");

    public static final ConfigOption<Integer> MAX_TOKENS =
            ConfigOptions.key("max-tokens")
                    .intType()
                    .defaultValue(4096)
                    .withDescription("Xinference LLM model maximum tokens");

    public static final ConfigOption<Integer> SEED =
            ConfigOptions.key("seed")
                    .intType()
                    .noDefaultValue()
                    .withDescription("Xinference LLM model seed");

    public static final ConfigOption<String> USER =
            ConfigOptions.key("user")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Xinference user");

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

    public static final ConfigOption<List<String>> STOP =
            ConfigOptions.key("stop")
                    .stringType()
                    .asList()
                    .noDefaultValue()
                    .withDescription("A list of strings that, if generated, will mark the end of the response.");

    public static final ConfigOption<Map<String, String>> CUSTOM_HEADERS =
            ConfigOptions.key("custom-headers")
                    .mapType()
                    .noDefaultValue()
                    .withDescription("Custom HTTP headers. " +
                            "For example: {\"content-type\": \"application/json\", " +
                            "\"accept\": \"application/json, text/event-stream\"}");

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
                MAX_RETRIES, MAX_TOKENS, LOG_REQUESTS, LOG_RESPONSES,
                SEED, USER, STOP, CUSTOM_HEADERS));
    }

    @Override
    public ChatModel create(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);
        validateConfigOptions(config);

        String baseUrl = config.get(BASE_URL);
        String modelName = config.get(MODEL_NAME);
        String apiKey = config.get(API_KEY);

        XinferenceChatModel.XinferenceChatModelBuilder builder = XinferenceChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .apiKey(apiKey);

        config.getOptional(TEMPERATURE).ifPresent(builder::temperature);
        config.getOptional(TOP_P).ifPresent(builder::topP);
        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(MAX_RETRIES).ifPresent(builder::maxRetries);
        config.getOptional(MAX_TOKENS).ifPresent(builder::maxTokens);
        config.getOptional(LOG_REQUESTS).ifPresent(builder::logRequests);
        config.getOptional(LOG_RESPONSES).ifPresent(builder::logResponses);
        config.getOptional(SEED).ifPresent(builder::seed);
        config.getOptional(USER).ifPresent(builder::user);
        config.getOptional(STOP).ifPresent(builder::stop);
        config.getOptional(CUSTOM_HEADERS).ifPresent(builder::customHeaders);

        return builder.build();
    }

    @Override
    public StreamingChatModel createStream(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);
        validateConfigOptions(config);

        String baseUrl = config.get(BASE_URL);
        String modelName = config.get(MODEL_NAME);
        String apiKey = config.get(API_KEY);

        XinferenceStreamingChatModel.XinferenceStreamingChatModelBuilder builder =
                XinferenceStreamingChatModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .apiKey(apiKey);

        config.getOptional(TEMPERATURE).ifPresent(builder::temperature);
        config.getOptional(TOP_P).ifPresent(builder::topP);
        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(MAX_TOKENS).ifPresent(builder::maxTokens);
        config.getOptional(LOG_REQUESTS).ifPresent(builder::logRequests);
        config.getOptional(LOG_RESPONSES).ifPresent(builder::logResponses);
        config.getOptional(SEED).ifPresent(builder::seed);
        config.getOptional(USER).ifPresent(builder::user);
        config.getOptional(STOP).ifPresent(builder::stop);
        config.getOptional(CUSTOM_HEADERS).ifPresent(builder::customHeaders);

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
    }
}
