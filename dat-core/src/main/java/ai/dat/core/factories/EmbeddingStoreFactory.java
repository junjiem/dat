package ai.dat.core.factories;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentType;
import com.google.common.base.Preconditions;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Embedding存储工厂接口类
 *
 * @Author JunjieM
 * @Date 2025/7/11
 */
public interface EmbeddingStoreFactory extends Factory {
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

    EmbeddingStore<TextSegment> create(String storeId,
                                       ContentType contentType,
                                       ReadableConfig config);
}