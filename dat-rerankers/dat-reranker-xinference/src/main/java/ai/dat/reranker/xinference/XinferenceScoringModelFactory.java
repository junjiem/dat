package ai.dat.reranker.xinference;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.ScoringModelFactory;
import ai.dat.core.utils.FactoryUtil;
import com.google.common.base.Preconditions;
import dev.langchain4j.community.model.xinference.XinferenceScoringModel;
import dev.langchain4j.model.scoring.ScoringModel;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/10/21
 */
public class XinferenceScoringModelFactory implements ScoringModelFactory {

    public static final String IDENTIFIER = "xinference";

    public static final ConfigOption<String> BASE_URL =
            ConfigOptions.key("base-url")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The base URL of Xinference server.");

    public static final ConfigOption<String> MODEL_NAME =
            ConfigOptions.key("model-name")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The name of the reranking model to use from Xinference server.");

    public static final ConfigOption<String> API_KEY =
            ConfigOptions.key("api-key")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The API KEY of Xinference server.");

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

    public static final ConfigOption<Integer> TOP_N =
            ConfigOptions.key("top-n")
                    .intType()
                    .noDefaultValue()
                    .withDescription("Specifies the number of highest return.");

    public static final ConfigOption<Boolean> RETURN_DOCUMENTS =
            ConfigOptions.key("return-documents")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Control whether to return the complete document content.");

    public static final ConfigOption<Boolean> RETURN_LEN =
            ConfigOptions.key("return-len")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Control the number of returned results.");

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
        return new LinkedHashSet<>(List.of(TIMEOUT, MAX_RETRIES, LOG_REQUESTS, LOG_RESPONSES,
                TOP_N, RETURN_DOCUMENTS, RETURN_LEN, CUSTOM_HEADERS));
    }

    @Override
    public ScoringModel create(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);
        validateConfigOptions(config);

        String baseUrl = config.get(BASE_URL);
        String modelName = config.get(MODEL_NAME);
        String apiKey = config.get(API_KEY);

        XinferenceScoringModel.XinferenceScoringModelBuilder builder = XinferenceScoringModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .apiKey(apiKey);

        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(MAX_RETRIES).ifPresent(builder::maxRetries);
        config.getOptional(LOG_REQUESTS).ifPresent(builder::logRequests);
        config.getOptional(LOG_RESPONSES).ifPresent(builder::logResponses);
        config.getOptional(TOP_N).ifPresent(builder::topN);
        config.getOptional(RETURN_DOCUMENTS).ifPresent(builder::returnDocuments);
        config.getOptional(RETURN_LEN).ifPresent(builder::returnLen);
        config.getOptional(CUSTOM_HEADERS).ifPresent(builder::customHeaders);

        return builder.build();
    }

    private void validateConfigOptions(ReadableConfig config) {
        Integer maxRetries = config.get(MAX_RETRIES);
        Preconditions.checkArgument(maxRetries >= 0,
                "'" + MAX_RETRIES.key() + "' value must be greater than or equal to 0");
        config.getOptional(TOP_N)
                .ifPresent(v -> Preconditions.checkArgument(v >= 1,
                        "'" + TOP_N.key() + "' value must be greater than 1"));
    }
}
