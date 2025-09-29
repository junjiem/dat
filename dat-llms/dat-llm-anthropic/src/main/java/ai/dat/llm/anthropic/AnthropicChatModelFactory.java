package ai.dat.llm.anthropic;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.ChatModelFactory;
import ai.dat.core.utils.FactoryUtil;
import com.google.common.base.Preconditions;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author JunjieM
 * @Date 2025/8/5
 */
public class AnthropicChatModelFactory implements ChatModelFactory {

    public static final String IDENTIFIER = "anthropic";

    public static final ConfigOption<String> BASE_URL =
            ConfigOptions.key("base-url")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The base URL of Anthropic server.");

    public static final ConfigOption<String> API_KEY =
            ConfigOptions.key("api-key")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The API KEY of Anthropic server.");

    public static final ConfigOption<String> MODEL_NAME =
            ConfigOptions.key("model-name")
                    .stringType()
                    .defaultValue(AnthropicChatModelName.CLAUDE_SONNET_4_20250514.toString())
                    .withDescription("The name of the model to use from Anthropic server. " +
                            "\nFor example: " +
                            Arrays.stream(AnthropicChatModelName.values())
                                    .map(AnthropicChatModelName::toString)
                                    .collect(Collectors.joining(", ")) +
                            ", etc."
                    );

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

    public static final ConfigOption<Integer> MAX_TOKENS =
            ConfigOptions.key("max-tokens")
                    .intType()
                    .defaultValue(4096)
                    .withDescription("Anthropic LLM model maximum tokens");

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

    public static final ConfigOption<String> VERSION =
            ConfigOptions.key("version")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Anthropic version");

    public static final ConfigOption<String> BETA =
            ConfigOptions.key("beta")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Anthropic beta. " +
                            "To use caching, please set to \"prompt-caching-2024-07-31\".");

    public static final ConfigOption<Boolean> CACHE_SYSTEM_MESSAGES =
            ConfigOptions.key("cache-system-messages")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to cache system messages");

    public static final ConfigOption<Boolean> CACHE_TOOLS =
            ConfigOptions.key("cache-tools")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to cache tools");

    public static final ConfigOption<String> THINKING_TYPE =
            ConfigOptions.key("thinking-type")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Anthropic LLM model thinking type. " +
                            "To turn on extended thinking, please set to \"enabled\".");

    public static final ConfigOption<Integer> THINKING_BUDGET_TOKENS =
            ConfigOptions.key("thinking-budget-tokens")
                    .intType()
                    .noDefaultValue()
                    .withDescription("Anthropic LLM model thinking budget tokens. " +
                            "After enabling thinking, and please set to a specified token budget for extended thinking.");

    public static final ConfigOption<Boolean> RETURN_THINKING =
            ConfigOptions.key("return-thinking")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to return thinking.");

    public static final ConfigOption<Boolean> SEND_THINKING =
            ConfigOptions.key("send-thinking")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Controls whether to send thinking and signatures stored " +
                            "in AiMessage to the LLM in follow-up requests.");

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
        return new LinkedHashSet<>(List.of(TEMPERATURE, TOP_P, TOP_K, TIMEOUT,
                MAX_RETRIES, MAX_TOKENS, LOG_REQUESTS, LOG_RESPONSES,
                VERSION, BETA, CACHE_SYSTEM_MESSAGES, CACHE_TOOLS,
                THINKING_TYPE, THINKING_BUDGET_TOKENS, RETURN_THINKING, SEND_THINKING));
    }

    @Override
    public ChatModel create(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);
        validateConfigOptions(config);

        String modelName = config.get(MODEL_NAME);

        AnthropicChatModel.AnthropicChatModelBuilder builder = AnthropicChatModel.builder()
                .modelName(modelName);

        config.getOptional(BASE_URL).ifPresent(builder::baseUrl);
        config.getOptional(API_KEY).ifPresent(builder::apiKey);
        config.getOptional(TEMPERATURE).ifPresent(builder::temperature);
        config.getOptional(TOP_P).ifPresent(builder::topP);
        config.getOptional(TOP_K).ifPresent(builder::topK);
        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(MAX_RETRIES).ifPresent(builder::maxRetries);
        config.getOptional(MAX_TOKENS).ifPresent(builder::maxTokens);
        config.getOptional(LOG_REQUESTS).ifPresent(builder::logRequests);
        config.getOptional(LOG_RESPONSES).ifPresent(builder::logResponses);
        config.getOptional(VERSION).ifPresent(builder::version);
        config.getOptional(BETA).ifPresent(builder::beta);
        config.getOptional(CACHE_SYSTEM_MESSAGES).ifPresent(builder::cacheSystemMessages);
        config.getOptional(CACHE_TOOLS).ifPresent(builder::cacheTools);
        config.getOptional(THINKING_TYPE).ifPresent(builder::thinkingType);
        config.getOptional(THINKING_BUDGET_TOKENS).ifPresent(builder::thinkingBudgetTokens);
        config.getOptional(RETURN_THINKING).ifPresent(builder::returnThinking);
        config.getOptional(SEND_THINKING).ifPresent(builder::sendThinking);

        return builder.build();
    }

    @Override
    public StreamingChatModel createStream(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);
        validateConfigOptions(config);

        String modelName = config.get(MODEL_NAME);

        AnthropicStreamingChatModel.AnthropicStreamingChatModelBuilder builder =
                AnthropicStreamingChatModel.builder()
                        .modelName(modelName);

        config.getOptional(BASE_URL).ifPresent(builder::baseUrl);
        config.getOptional(API_KEY).ifPresent(builder::apiKey);
        config.getOptional(TEMPERATURE).ifPresent(builder::temperature);
        config.getOptional(TOP_P).ifPresent(builder::topP);
        config.getOptional(TOP_K).ifPresent(builder::topK);
        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(MAX_TOKENS).ifPresent(builder::maxTokens);
        config.getOptional(LOG_REQUESTS).ifPresent(builder::logRequests);
        config.getOptional(LOG_RESPONSES).ifPresent(builder::logResponses);
        config.getOptional(VERSION).ifPresent(builder::version);
        config.getOptional(BETA).ifPresent(builder::beta);
        config.getOptional(CACHE_SYSTEM_MESSAGES).ifPresent(builder::cacheSystemMessages);
        config.getOptional(CACHE_TOOLS).ifPresent(builder::cacheTools);
        config.getOptional(THINKING_TYPE).ifPresent(builder::thinkingType);
        config.getOptional(THINKING_BUDGET_TOKENS).ifPresent(builder::thinkingBudgetTokens);
        config.getOptional(RETURN_THINKING).ifPresent(builder::returnThinking);
        config.getOptional(SEND_THINKING).ifPresent(builder::sendThinking);

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
        Integer maxTokens = config.get(MAX_TOKENS);
        Preconditions.checkArgument(maxTokens > 0,
                "'" + MAX_TOKENS.key() + "' value must be greater than 0");
    }

}
