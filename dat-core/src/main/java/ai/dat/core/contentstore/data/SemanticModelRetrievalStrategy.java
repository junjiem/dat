package ai.dat.core.contentstore.data;

import lombok.Getter;

/**
 * @Author JunjieM
 * @Date 2025/9/24
 */
@Getter
public enum SemanticModelRetrievalStrategy {
    FE("Full Embeddings"),
    HYQE("HyQE (Hypothetical Question Embeddings)");

    private final String description;

    SemanticModelRetrievalStrategy(String description) {
        this.description = description;
    }
}
