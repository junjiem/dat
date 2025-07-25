package ai.dat.core.factories;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/7/11
 */
public class InMemoryEmbeddingStoreFactory implements EmbeddingStoreFactory {

    public static final String IDENTIFIER = "inmemory";

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
        return Collections.emptySet();
    }

    @Override
    public Set<ConfigOption<?>> fingerprintOptions() {
        return Collections.emptySet();
    }

    @Override
    public EmbeddingStore<TextSegment> create(@NonNull String storeId,
                                              @NonNull ContentType contentType,
                                              @NonNull ReadableConfig config) {
        return new InMemoryEmbeddingStore<>();
    }
}
