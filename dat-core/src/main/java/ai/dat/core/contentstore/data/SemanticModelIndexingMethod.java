package ai.dat.core.contentstore.data;

import lombok.Getter;

/**
 * @Author JunjieM
 * @Date 2025/9/24
 */
@Getter
public enum SemanticModelIndexingMethod {
    FE("Full Embeddings"),
    CE("Column Embeddings"),
    HYQE("HyQE (Hypothetical Question Embeddings)");

    private final String description;

    SemanticModelIndexingMethod(String description) {
        this.description = description;
    }
}
