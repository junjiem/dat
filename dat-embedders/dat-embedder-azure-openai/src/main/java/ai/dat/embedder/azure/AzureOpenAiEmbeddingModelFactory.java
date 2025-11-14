package ai.dat.embedder.azure;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.EmbeddingModelFactory;
import ai.dat.core.utils.FactoryUtil;
import com.google.common.base.Preconditions;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class AzureOpenAiEmbeddingModelFactory implements EmbeddingModelFactory {

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

    public static final ConfigOption<Integer> DIMENSIONS =
            ConfigOptions.key("dimensions")
                    .intType()
                    .noDefaultValue()
                    .withDescription("Azure OpenAI embedding model dimensions");

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

    public static final ConfigOption<Boolean> LOG_REQUESTS_AND_RESPONSES =
            ConfigOptions.key("log-requests-and-responses")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to print the LLM requests and responses log");

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
        return new LinkedHashSet<>(List.of(DIMENSIONS, TIMEOUT,
                MAX_RETRIES, LOG_REQUESTS_AND_RESPONSES, CUSTOM_HEADERS));
    }

    @Override
    public Set<ConfigOption<?>> fingerprintOptions() {
        return Set.of(ENDPOINT, DEPLOYMENT_ID, API_VERSION);
    }

    @Override
    public EmbeddingModel create(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);
        validateConfigOptions(config);

        String endpoint = config.get(ENDPOINT);
        String deploymentId = config.get(DEPLOYMENT_ID);
        String apiVersion = config.get(API_VERSION);
        String apiKey = config.get(API_KEY);

        AzureOpenAiEmbeddingModel.Builder builder = AzureOpenAiEmbeddingModel.builder()
                .endpoint(endpoint)
                .deploymentName(deploymentId)
                .serviceVersion(apiVersion)
                .apiKey(apiKey);

        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(DIMENSIONS).ifPresent(builder::dimensions);
        config.getOptional(MAX_RETRIES).ifPresent(builder::maxRetries);
        config.getOptional(LOG_REQUESTS_AND_RESPONSES).ifPresent(builder::logRequestsAndResponses);
        config.getOptional(CUSTOM_HEADERS).ifPresent(builder::customHeaders);

        return builder.build();
    }

    private void validateConfigOptions(ReadableConfig config) {
        Integer maxRetries = config.get(MAX_RETRIES);
        Preconditions.checkArgument(maxRetries >= 0,
                "'" + MAX_RETRIES.key() + "' value must be greater than or equal to 0");
        config.getOptional(DIMENSIONS)
                .ifPresent(d -> Preconditions.checkArgument(d > 0,
                        "'" + DIMENSIONS.key() + "' value must be greater than 0"));
    }
}
