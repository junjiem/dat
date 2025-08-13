package ai.dat.core.factories;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.contentstore.DefaultContentStore;
import ai.dat.core.factories.data.ChatModelInstance;
import ai.dat.core.utils.FactoryUtil;
import com.google.common.base.Preconditions;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.NonNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author JunjieM
 * @Date 2025/7/11
 */
public class DefaultContentStoreFactory implements ContentStoreFactory {

    public static final String IDENTIFIER = "default";

    public static final ConfigOption<Integer> MAX_RESULTS =
            ConfigOptions.key("max-results")
                    .intType()
                    .defaultValue(5)
                    .withDescription("Content store retrieve TopK maximum value, must be between 1 and 10");

    public static final ConfigOption<Double> MIN_SCORE =
            ConfigOptions.key("min-score")
                    .doubleType()
                    .defaultValue(0.6)
                    .withDescription("Content store retrieve Score minimum value, must be between 0.0 and 1.0");

    public static final ConfigOption<String> DEFAULT_LLM =
            ConfigOptions.key("default-llm")
                    .stringType()
                    .defaultValue("default")
                    .withDescription("Specify the default LLM model name.");

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(MAX_RESULTS, MIN_SCORE, DEFAULT_LLM));
    }

    @Override
    public ContentStore create(@NonNull ReadableConfig config,
                               @NonNull EmbeddingModel embeddingModel,
                               @NonNull EmbeddingStore<TextSegment> mdlEmbeddingStore,
                               @NonNull EmbeddingStore<TextSegment> sqlEmbeddingStore,
                               @NonNull EmbeddingStore<TextSegment> synEmbeddingStore,
                               @NonNull EmbeddingStore<TextSegment> docEmbeddingStore,
                               @NonNull List<ChatModelInstance> chatModelInstances) {
        Preconditions.checkArgument(!chatModelInstances.isEmpty(),
                "chatModelInstances cannot be empty");
        FactoryUtil.validateFactoryOptions(this, config);
        Map<String, ChatModelInstance> instances = chatModelInstances.stream()
                .collect(Collectors.toMap(ChatModelInstance::getName, i -> i));
        validateConfigOptions(config, instances);

        Integer maxResults = config.get(MAX_RESULTS);
        Double minScore = config.get(MIN_SCORE);
        ChatModelInstance instance = config.getOptional(DEFAULT_LLM)
                .map(instances::get).orElseGet(() -> chatModelInstances.get(0));

        return DefaultContentStore.builder()
                .chatModel(instance.getChatModel())
                .embeddingModel(embeddingModel)
                .mdlEmbeddingStore(mdlEmbeddingStore)
                .sqlEmbeddingStore(sqlEmbeddingStore)
                .synEmbeddingStore(synEmbeddingStore)
                .docEmbeddingStore(docEmbeddingStore)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    private void validateConfigOptions(ReadableConfig config, Map<String, ChatModelInstance> instances) {
        Integer maxResults = config.get(MAX_RESULTS);
        Preconditions.checkArgument(maxResults >= 1 && maxResults <= 10,
                "'" + MAX_RESULTS.key() + "' value must be between 1 and 10");
        Double minScore = config.get(MIN_SCORE);
        Preconditions.checkArgument(minScore >= 0.0 && minScore <= 1.0,
                "'" + MIN_SCORE.key() + "' value must be between 0.0 and 1.0");
        String llmNames = String.join(", ", instances.keySet());
        String errorMessageFormat = "'%s' value must be one of [%s]";
        config.getOptional(DEFAULT_LLM)
                .ifPresent(n -> Preconditions.checkArgument(instances.containsKey(n),
                        String.format(errorMessageFormat, DEFAULT_LLM.key(), llmNames)));
    }
}
