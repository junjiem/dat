package ai.dat.core.contentstore.data;

import lombok.Getter;

/**
 * @Author JunjieM
 * @Date 2025/10/20
 */
@Getter
public enum BusinessKnowledgeIndexingMethod {
    FE("Full Embeddings"),
    GCE("General Chunking Embeddings"),
    PCCE("Parent-child Chunking Embeddings");

    private final String description;

    BusinessKnowledgeIndexingMethod(String description) {
        this.description = description;
    }
}
