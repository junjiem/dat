package ai.dat.llm.azure;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.ChatModelFactory;
import ai.dat.core.utils.FactoryUtil;
import com.google.common.base.Preconditions;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class AzureOpenAiChatModelFactory implements ChatModelFactory {

    public static final String IDENTIFIER = "azure-openai";

    public static final ConfigOption<String> ENDPOINT =
            ConfigOptions.key("endpoint")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("""
                            Supported Azure OpenAI endpoints 
                            (protocol and hostname, for example: https://aoairesource.openai.azure.com. \
                            Replace "aoairesource" with your Azure OpenAI resource name). 
                            https://{your-resource-name}.openai.azure.com
                            """);

    public static final ConfigOption<String> DEPLOYMENT_ID =
            ConfigOptions.key("deployment-id")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Deployment ID of the model which was deployed.");

    public static final ConfigOption<String> API_VERSION =
            ConfigOptions.key("api-version")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The API version of Azure OpenAI.");

    public static final ConfigOption<String> API_KEY =
            ConfigOptions.key("api-key")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Provide Azure OpenAI API key here.");

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
                    .withDescription("Azure OpenAI LLM model maximum tokens");

    public static final ConfigOption<Long> SEED =
            ConfigOptions.key("seed")
                    .longType()
                    .noDefaultValue()
                    .withDescription("Azure OpenAI LLM model seed");

    public static final ConfigOption<String> USER =
            ConfigOptions.key("user")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Azure OpenAI user");

    public static final ConfigOption<List<String>> STOP =
            ConfigOptions.key("stop")
                    .stringType()
                    .asList()
                    .noDefaultValue()
                    .withDescription("A list of strings that, if generated, will mark the end of the response.");

    public static final ConfigOption<Boolean> LOG_REQUESTS_AND_RESPONSES =
            ConfigOptions.key("log-requests-and-responses")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to print the LLM requests and responses log");

    public static final ConfigOption<String> RESPONSE_FORMAT =
            ConfigOptions.key("response-format")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("OpenAI LLM response format. Supported: text, json");

    public static final ConfigOption<Boolean> STRICT_JSON_SCHEMA =
            ConfigOptions.key("strict-json-schema")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to output strict json schema");

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
        return new LinkedHashSet<>(List.of(ENDPOINT, DEPLOYMENT_ID, API_VERSION, API_KEY));
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(TEMPERATURE, TOP_P, TIMEOUT,
                MAX_RETRIES, MAX_TOKENS, LOG_REQUESTS_AND_RESPONSES,
                RESPONSE_FORMAT, STRICT_JSON_SCHEMA, STOP, SEED, USER, CUSTOM_HEADERS));
    }

    @Override
    public ChatModel create(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);
        validateConfigOptions(config);

        String endpoint = config.get(ENDPOINT);
        String deploymentId = config.get(DEPLOYMENT_ID);
        String apiVersion = config.get(API_VERSION);
        String apiKey = config.get(API_KEY);

        AzureOpenAiChatModel.Builder builder = AzureOpenAiChatModel.builder()
                .endpoint(endpoint)
                .deploymentName(deploymentId)
                .serviceVersion(apiVersion)
                .apiKey(apiKey);

        config.getOptional(TEMPERATURE).ifPresent(builder::temperature);
        config.getOptional(TOP_P).ifPresent(builder::topP);
        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(MAX_RETRIES).ifPresent(builder::maxRetries);
        config.getOptional(MAX_TOKENS).ifPresent(builder::maxTokens);
        config.getOptional(RESPONSE_FORMAT).ifPresent(format -> {
            ResponseFormat responseFormat = format.equalsIgnoreCase("JSON")
                    ? ResponseFormat.JSON : ResponseFormat.TEXT;
            builder.responseFormat(responseFormat);
        });
        config.getOptional(LOG_REQUESTS_AND_RESPONSES).ifPresent(builder::logRequestsAndResponses);
        config.getOptional(STRICT_JSON_SCHEMA).ifPresent(builder::strictJsonSchema);
        config.getOptional(STOP).ifPresent(builder::stop);
        config.getOptional(SEED).ifPresent(builder::seed);
        config.getOptional(USER).ifPresent(builder::user);
        config.getOptional(CUSTOM_HEADERS).ifPresent(builder::customHeaders);

        return builder.build();
    }

    @Override
    public StreamingChatModel createStream(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);
        validateConfigOptions(config);

        String endpoint = config.get(ENDPOINT);
        String deploymentId = config.get(DEPLOYMENT_ID);
        String apiVersion = config.get(API_VERSION);
        String apiKey = config.get(API_KEY);

        AzureOpenAiStreamingChatModel.Builder builder = AzureOpenAiStreamingChatModel.builder()
                .endpoint(endpoint)
                .deploymentName(deploymentId)
                .serviceVersion(apiVersion)
                .apiKey(apiKey);

        config.getOptional(TEMPERATURE).ifPresent(builder::temperature);
        config.getOptional(TOP_P).ifPresent(builder::topP);
        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(MAX_RETRIES).ifPresent(builder::maxRetries);
        config.getOptional(MAX_TOKENS).ifPresent(builder::maxTokens);
        config.getOptional(RESPONSE_FORMAT).ifPresent(format -> {
            ResponseFormat responseFormat = format.equalsIgnoreCase("JSON")
                    ? ResponseFormat.JSON : ResponseFormat.TEXT;
            builder.responseFormat(responseFormat);
        });
        config.getOptional(LOG_REQUESTS_AND_RESPONSES).ifPresent(builder::logRequestsAndResponses);
        config.getOptional(STRICT_JSON_SCHEMA).ifPresent(builder::strictJsonSchema);
        config.getOptional(STOP).ifPresent(builder::stop);
        config.getOptional(SEED).ifPresent(builder::seed);
        config.getOptional(USER).ifPresent(builder::user);
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
