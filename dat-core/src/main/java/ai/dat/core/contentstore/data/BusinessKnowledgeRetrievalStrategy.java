package ai.dat.core.contentstore.data;

import lombok.Getter;

/**
 * @Author JunjieM
 * @Date 2025/10/20
 */
@Getter
public enum BusinessKnowledgeRetrievalStrategy {
    FE("Full Embeddings"),
    GCE("General Chunking Embeddings");

    private final String description;

    BusinessKnowledgeRetrievalStrategy(String description) {
        this.description = description;
    }
}
