package ai.dat.core.factories;

import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * @Author JunjieM
 * @Date 2025/7/11
 */
public interface ContentStoreFactory extends Factory {
    ContentStore create(ReadableConfig config,
                        EmbeddingModel embeddingModel,
                        EmbeddingStore<TextSegment> mdlEmbeddingStore,
                        EmbeddingStore<TextSegment> sqlEmbeddingStore,
                        EmbeddingStore<TextSegment> synEmbeddingStore,
                        EmbeddingStore<TextSegment> docEmbeddingStore,
                        ChatModel chatModel);
}