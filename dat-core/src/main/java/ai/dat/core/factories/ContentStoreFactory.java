package ai.dat.core.factories;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.factories.data.ChatModelInstance;
import com.google.common.base.Preconditions;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author JunjieM
 * @Date 2025/7/11
 */
public interface ContentStoreFactory extends Factory {

    Set<ConfigOption<?>> fingerprintOptions();

    default Map<String, String> fingerprintConfigs(@NonNull ReadableConfig config) {
        Set<ConfigOption<?>> fingerprintOptions = fingerprintOptions();
        Preconditions.checkArgument(fingerprintOptions != null,
                "The fingerprintOptions method cannot return null");
        List<String> keys = fingerprintOptions.stream()
                .map(ConfigOption::key)
                .toList();
        return config.toMap().entrySet().stream()
                .filter(e -> keys.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    ContentStore create(ReadableConfig config,
                        EmbeddingModel embeddingModel,
                        EmbeddingStore<TextSegment> mdlEmbeddingStore,
                        EmbeddingStore<TextSegment> sqlEmbeddingStore,
                        EmbeddingStore<TextSegment> synEmbeddingStore,
                        EmbeddingStore<TextSegment> docEmbeddingStore,
                        List<ChatModelInstance> chatModelInstances,
                        ScoringModel scoringModel);

    @Deprecated
    default ContentStore create(ReadableConfig config,
                                EmbeddingModel embeddingModel,
                                EmbeddingStore<TextSegment> mdlEmbeddingStore,
                                EmbeddingStore<TextSegment> sqlEmbeddingStore,
                                EmbeddingStore<TextSegment> synEmbeddingStore,
                                EmbeddingStore<TextSegment> docEmbeddingStore,
                                List<ChatModelInstance> chatModelInstances) {
        return create(config, embeddingModel, mdlEmbeddingStore, sqlEmbeddingStore,
                synEmbeddingStore, docEmbeddingStore, chatModelInstances, null);
    }
}