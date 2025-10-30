package ai.dat.core.contentstore.data;

import lombok.Getter;

/**
 *
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
