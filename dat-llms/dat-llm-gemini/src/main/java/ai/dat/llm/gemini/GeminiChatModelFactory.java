package ai.dat.llm.gemini;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.ChatModelFactory;
import ai.dat.core.utils.FactoryUtil;
import com.google.common.base.Preconditions;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/9/29
 */
public class GeminiChatModelFactory implements ChatModelFactory {

    public static final String IDENTIFIER = "gemini";

    public static final ConfigOption<String> BASE_URL =
            ConfigOptions.key("base-url")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The base URL of Google Ai Gemini server.");

    public static final ConfigOption<String> API_KEY =
            ConfigOptions.key("api-key")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The API KEY of Google Ai Gemini server. " +
                            "Get an API key for free here: https://ai.google.dev/gemini-api/docs/api-key .");

    public static final ConfigOption<String> MODEL_NAME =
            ConfigOptions.key("model-name")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("""
                            The name of the model to use from Google Ai Gemini server.
                            For example: gemini-2.5-pro, gemini-2.5-flash, gemini-2.5-flash-lite, gemini-2.0-pro, gemini-2.0-flash, etc.
                            """);

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

    public static final ConfigOption<Double> FREQUENCY_PENALTY =
            ConfigOptions.key("frequency-penalty")
                    .doubleType()
                    .noDefaultValue()
                    .withDescription("Frequency penalty, value must be between 0.0 and 2.0");

    public static final ConfigOption<Double> PRESENCE_PENALTY =
            ConfigOptions.key("presence-penalty")
                    .doubleType()
                    .noDefaultValue()
                    .withDescription("Presence penalty, value must be between -2.0 and 2.0");

    public static final ConfigOption<Integer> MAX_OUTPUT_TOKENS =
            ConfigOptions.key("max-output-tokens")
                    .intType()
                    .defaultValue(8192)
                    .withDescription("Google Ai Gemini maximum output tokens");

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

    public static final ConfigOption<List<String>> STOP_SEQUENCES =
            ConfigOptions.key("stop-sequences")
                    .stringType()
                    .asList()
                    .noDefaultValue()
                    .withDescription("A list of strings that, if generated, will mark the end of the response.");

    public static final ConfigOption<Boolean> SEND_THINKING =
            ConfigOptions.key("send-thinking")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to send thinking");

    public static final ConfigOption<Boolean> RETURN_THINKING =
            ConfigOptions.key("return-thinking")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("controls whether thinking field from the API response is parsed and returned.");

    public static final ConfigOption<Boolean> ALLOW_CODE_EXECUTION =
            ConfigOptions.key("allow-code-execution")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to allow code execution");

    public static final ConfigOption<Boolean> INCLUDE_CODE_EXECUTION =
            ConfigOptions.key("include-code-execution")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to include code execution");

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return new LinkedHashSet<>(List.of(API_KEY, MODEL_NAME));
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(
                BASE_URL, TEMPERATURE, TOP_K, TOP_P, TIMEOUT,
                MAX_RETRIES, SEED, FREQUENCY_PENALTY, PRESENCE_PENALTY,
                MAX_OUTPUT_TOKENS, LOG_REQUESTS, LOG_RESPONSES,
                STOP_SEQUENCES, SEND_THINKING, RETURN_THINKING,
                ALLOW_CODE_EXECUTION, INCLUDE_CODE_EXECUTION));
    }

    @Override
    public ChatModel create(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);
        validateConfigOptions(config);

        String apiKey = config.get(API_KEY);
        String modelName = config.get(MODEL_NAME);

        GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder builder = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName);

        config.getOptional(BASE_URL).ifPresent(builder::baseUrl);
        config.getOptional(TEMPERATURE).ifPresent(builder::temperature);
        config.getOptional(TOP_K).ifPresent(builder::topK);
        config.getOptional(TOP_P).ifPresent(builder::topP);
        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(MAX_RETRIES).ifPresent(builder::maxRetries);
        config.getOptional(SEED).ifPresent(builder::seed);
        config.getOptional(FREQUENCY_PENALTY).ifPresent(builder::frequencyPenalty);
        config.getOptional(PRESENCE_PENALTY).ifPresent(builder::presencePenalty);
        config.getOptional(MAX_OUTPUT_TOKENS).ifPresent(builder::maxOutputTokens);
        config.getOptional(LOG_REQUESTS).ifPresent(builder::logRequests);
        config.getOptional(LOG_RESPONSES).ifPresent(builder::logResponses);
        config.getOptional(STOP_SEQUENCES).ifPresent(builder::stopSequences);
        config.getOptional(SEND_THINKING).ifPresent(builder::sendThinking);
        config.getOptional(RETURN_THINKING).ifPresent(builder::returnThinking);
        config.getOptional(ALLOW_CODE_EXECUTION).ifPresent(builder::allowCodeExecution);
        config.getOptional(INCLUDE_CODE_EXECUTION).ifPresent(builder::includeCodeExecutionOutput);

        return builder.build();
    }

    @Override
    public StreamingChatModel createStream(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);
        validateConfigOptions(config);

        String apiKey = config.get(API_KEY);
        String modelName = config.get(MODEL_NAME);

        GoogleAiGeminiStreamingChatModel.GoogleAiGeminiStreamingChatModelBuilder builder =
                GoogleAiGeminiStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName);

        config.getOptional(BASE_URL).ifPresent(builder::baseUrl);
        config.getOptional(TEMPERATURE).ifPresent(builder::temperature);
        config.getOptional(TOP_K).ifPresent(builder::topK);
        config.getOptional(TOP_P).ifPresent(builder::topP);
        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(SEED).ifPresent(builder::seed);
        config.getOptional(FREQUENCY_PENALTY).ifPresent(builder::frequencyPenalty);
        config.getOptional(PRESENCE_PENALTY).ifPresent(builder::presencePenalty);
        config.getOptional(MAX_OUTPUT_TOKENS).ifPresent(builder::maxOutputTokens);
        config.getOptional(LOG_REQUESTS).ifPresent(builder::logRequests);
        config.getOptional(LOG_RESPONSES).ifPresent(builder::logResponses);
        config.getOptional(STOP_SEQUENCES).ifPresent(builder::stopSequences);
        config.getOptional(SEND_THINKING).ifPresent(builder::sendThinking);
        config.getOptional(RETURN_THINKING).ifPresent(builder::returnThinking);
        config.getOptional(ALLOW_CODE_EXECUTION).ifPresent(builder::allowCodeExecution);
        config.getOptional(INCLUDE_CODE_EXECUTION).ifPresent(builder::includeCodeExecutionOutput);

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
        config.getOptional(FREQUENCY_PENALTY)
                .ifPresent(v -> Preconditions.checkArgument(v >= 0.0 && v <= 2.0,
                        "'" + FREQUENCY_PENALTY.key() + "' value must be between 0.0 and 2.0"));
        config.getOptional(PRESENCE_PENALTY)
                .ifPresent(v -> Preconditions.checkArgument(v >= -2.0 && v <= 2.0,
                        "'" + PRESENCE_PENALTY.key() + "' value must be between -2.0 and 2.0"));
    }
}
