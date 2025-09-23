package ai.dat.llm.ollama;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.ChatModelFactory;
import ai.dat.core.utils.FactoryUtil;
import com.google.common.base.Preconditions;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/9/23
 */
public class OllamaChatModelFactory implements ChatModelFactory {

    public static final String IDENTIFIER = "ollama";

    public static final ConfigOption<String> BASE_URL =
            ConfigOptions.key("base-url")
                    .stringType()
                    .defaultValue("http://localhost:11434")
                    .withDescription("The base URL of Ollama server.");

    public static final ConfigOption<String> MODEL_NAME =
            ConfigOptions.key("model-name")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The name of the model to use from Ollama server.");

    public static final ConfigOption<Double> TEMPERATURE =
            ConfigOptions.key("temperature")
                    .doubleType()
                    .noDefaultValue()
                    .withDescription("Controls the randomness of the generated responses. " +
                            "Higher values (e.g., 1.0) result in more diverse output, " +
                            "while lower values (e.g., 0.2) produce more deterministic responses.");

    public static final ConfigOption<Integer> TOP_K =
            ConfigOptions.key("top-k")
                    .intType()
                    .noDefaultValue()
                    .withDescription("Specifies the number of highest probability tokens to consider " +
                            "for each step during generation.");

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

    public static final ConfigOption<Integer> SEED =
            ConfigOptions.key("seed")
                    .intType()
                    .noDefaultValue()
                    .withDescription("Sets the random seed for reproducibility of generated responses.");

    public static final ConfigOption<Boolean> LOG_REQUESTS =
            ConfigOptions.key("log-requests")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to print the LLM requests log.");

    public static final ConfigOption<Boolean> LOG_RESPONSES =
            ConfigOptions.key("log-responses")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to print the LLM responses log.");

    public static final ConfigOption<Integer> NUM_PREDICT =
            ConfigOptions.key("num-predict")
                    .intType()
                    .noDefaultValue()
                    .withDescription("The number of predictions to generate for each input prompt.");

    public static final ConfigOption<List<String>> STOP =
            ConfigOptions.key("stop")
                    .stringType()
                    .asList()
                    .noDefaultValue()
                    .withDescription("A list of strings that, if generated, will mark the end of the response.");

    public static final ConfigOption<Boolean> THINK =
            ConfigOptions.key("think")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("""
                            Controls whether the LLM thinks and how.
                            `true`: the LLM thinks and returns thoughts in a separate `thinking` field;
                            `false`: the LLM does not think;
                            `null` (not set): reasoning LLMs (e.g., DeepSeek R1) will prepend thoughts, delimited by `<think>` and `</think>`, to the actual response;
                            """);

    public static final ConfigOption<Boolean> RETURN_THINKING =
            ConfigOptions.key("return-thinking")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("controls whether thinking field from the API response is parsed and returned.");

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
        return new LinkedHashSet<>(List.of(BASE_URL, MODEL_NAME));
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(TEMPERATURE, TOP_K, TOP_P, TIMEOUT,
                MAX_RETRIES, SEED, LOG_REQUESTS, LOG_RESPONSES,
                NUM_PREDICT, STOP, THINK, RETURN_THINKING, CUSTOM_HEADERS));
    }

    @Override
    public ChatModel create(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);
        validateConfigOptions(config);

        String baseUrl = config.get(BASE_URL);
        String modelName = config.get(MODEL_NAME);

        OllamaChatModel.OllamaChatModelBuilder builder = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName);

        config.getOptional(TEMPERATURE).ifPresent(builder::temperature);
        config.getOptional(TOP_K).ifPresent(builder::topK);
        config.getOptional(TOP_P).ifPresent(builder::topP);
        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(MAX_RETRIES).ifPresent(builder::maxRetries);
        config.getOptional(SEED).ifPresent(builder::seed);
        config.getOptional(LOG_REQUESTS).ifPresent(builder::logRequests);
        config.getOptional(LOG_RESPONSES).ifPresent(builder::logResponses);
        config.getOptional(NUM_PREDICT).ifPresent(builder::numPredict);
        config.getOptional(STOP).ifPresent(builder::stop);
        config.getOptional(THINK).ifPresent(builder::think);
        config.getOptional(RETURN_THINKING).ifPresent(builder::returnThinking);
        config.getOptional(CUSTOM_HEADERS).ifPresent(builder::customHeaders);

        return builder.build();
    }

    @Override
    public StreamingChatModel createStream(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);
        validateConfigOptions(config);

        String baseUrl = config.get(BASE_URL);
        String modelName = config.get(MODEL_NAME);

        OllamaStreamingChatModel.OllamaStreamingChatModelBuilder builder =
                OllamaStreamingChatModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(modelName);

        config.getOptional(TEMPERATURE).ifPresent(builder::temperature);
        config.getOptional(TOP_K).ifPresent(builder::topK);
        config.getOptional(TOP_P).ifPresent(builder::topP);
        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(SEED).ifPresent(builder::seed);
        config.getOptional(LOG_REQUESTS).ifPresent(builder::logRequests);
        config.getOptional(LOG_RESPONSES).ifPresent(builder::logResponses);
        config.getOptional(NUM_PREDICT).ifPresent(builder::numPredict);
        config.getOptional(STOP).ifPresent(builder::stop);
        config.getOptional(THINK).ifPresent(builder::think);
        config.getOptional(RETURN_THINKING).ifPresent(builder::returnThinking);
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
        config.getOptional(TOP_K)
                .ifPresent(v -> Preconditions.checkArgument(v >= 1,
                        "'" + TOP_K.key() + "' value must be greater than 1"));
        Integer maxRetries = config.get(MAX_RETRIES);
        Preconditions.checkArgument(maxRetries >= 0,
                "'" + MAX_RETRIES.key() + "' value must be greater than or equal to 0");
    }
}
