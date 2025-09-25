package ai.dat.embedder.openai;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.EmbeddingModelFactory;
import ai.dat.core.utils.FactoryUtil;
import com.google.common.base.Preconditions;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModelName;
import dev.langchain4j.model.openai.internal.OpenAiUtils;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/7/1
 */
public class OpenAiEmbeddingModelFactory implements EmbeddingModelFactory {

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
                    .withDescription("OpenAI embedding model name");

    public static final ConfigOption<Integer> DIMENSIONS =
            ConfigOptions.key("dimensions")
                    .intType()
                    .noDefaultValue()
                    .withDescription("OpenAI embedding model dimensions");

    public static final ConfigOption<Duration> TIMEOUT =
            ConfigOptions.key("timeout")
                    .durationType()
                    .noDefaultValue()
                    .withDescription("OpenAI embedding model timeout");

    public static final ConfigOption<Integer> MAX_RETRIES =
            ConfigOptions.key("max-retries")
                    .intType()
                    .defaultValue(2)
                    .withDescription("OpenAI embedding model maximum retries");

    public static final ConfigOption<Integer> MAX_SEGMENTS_PER_BATCH =
            ConfigOptions.key("max-segments-per-batch")
                    .intType()
                    .defaultValue(2048)
                    .withDescription("OpenAI embedding model maximum segments per batch size");

    public static final ConfigOption<Boolean> LOG_REQUESTS =
            ConfigOptions.key("log-requests")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to print the embedding model requests log");

    public static final ConfigOption<Boolean> LOG_RESPONSES =
            ConfigOptions.key("log-responses")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to print the embedding model responses log");

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public EmbeddingModel create(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);
        validateConfigOptions(config);

        String modelName = config.get(MODEL_NAME);
        Integer dimensions = config.getOptional(DIMENSIONS)
                .orElse(OpenAiEmbeddingModelName.knownDimension(modelName));
        Preconditions.checkNotNull(dimensions, "'" + DIMENSIONS.key() + "' cannot be empty");
        Preconditions.checkArgument(dimensions > 0,
                "'" + DIMENSIONS.key() + "' value must be greater than 0");
        Boolean logRequests = config.get(LOG_REQUESTS);
        Boolean logResponses = config.get(LOG_RESPONSES);

        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .modelName(modelName)
                .dimensions(dimensions)
                .logRequests(logRequests)
                .logResponses(logResponses);
        config.getOptional(BASE_URL).ifPresent(builder::baseUrl);
        config.getOptional(API_KEY).ifPresent(builder::apiKey);
        config.getOptional(TIMEOUT).ifPresent(builder::timeout);
        config.getOptional(MAX_RETRIES).ifPresent(builder::maxRetries);
        config.getOptional(MAX_SEGMENTS_PER_BATCH).ifPresent(builder::maxSegmentsPerBatch);
        return builder.build();
    }

    private void validateConfigOptions(ReadableConfig config) {
        Integer maxRetries = config.get(MAX_RETRIES);
        Preconditions.checkArgument(maxRetries >= 0,
                "'" + MAX_RETRIES.key() + "' value must be greater than or equal to 0");
        Integer maxSegmentsPerBatch = config.get(MAX_SEGMENTS_PER_BATCH);
        Preconditions.checkArgument(maxSegmentsPerBatch > 0,
                "'" + MAX_SEGMENTS_PER_BATCH.key() + "' value must be greater than 0");
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return new LinkedHashSet<>(List.of(BASE_URL, MODEL_NAME, API_KEY));
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(DIMENSIONS, TIMEOUT, MAX_RETRIES,
                MAX_SEGMENTS_PER_BATCH, LOG_REQUESTS, LOG_RESPONSES));
    }

    @Override
    public Set<ConfigOption<?>> fingerprintOptions() {
        return Set.of(MODEL_NAME, BASE_URL, API_KEY, DIMENSIONS);
    }
}
