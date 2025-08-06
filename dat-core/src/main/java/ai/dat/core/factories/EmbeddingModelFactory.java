package ai.dat.core.factories;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ReadableConfig;
import com.google.common.base.Preconditions;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Embedding模型工厂接口类
 *
 * @Author JunjieM
 * @Date 2025/7/1
 */
public interface EmbeddingModelFactory extends Factory {
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

    EmbeddingModel create(ReadableConfig config);
}
