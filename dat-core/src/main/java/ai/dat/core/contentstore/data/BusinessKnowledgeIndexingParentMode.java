package ai.dat.core.contentstore.data;

import lombok.Getter;

/**
 * @Author JunjieM
 * @Date 2025/10/23
 */
@Getter
public enum BusinessKnowledgeIndexingParentMode {
    FULLTEXT("The entire text is used as the parent chunk and retrieved directly."),
    PARAGRAPH("This mode splits the text in to paragraphs based on regular expression " +
            "and the maximum chunk length, using the split text as the parent chunk for retrieval.");

    private final String description;

    BusinessKnowledgeIndexingParentMode(String description) {
        this.description = description;
    }
}
