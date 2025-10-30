package ai.dat.core.data.example;

import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.contentstore.data.WordSynonymPair;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;

/**
 * Encapsulates example artifacts used when seeding a project, including SQL pairs,
 * synonym mappings, and supporting background knowledge.
 */
@Setter
@Getter
public class Example {
    /**
     * Collection of sample natural language questions paired with SQL statements.
     */
    @NonNull
    @JsonProperty("sql_pairs")
    private List<QuestionSqlPair> questionSqlPairs = List.of();

    /**
     * Collection of words and their synonyms that help expand user questions.
     */
    @NonNull
    @JsonProperty("synonyms_pairs")
    private List<WordSynonymPair> wordSynonymPairs = List.of();

    /**
     * Background knowledge entries associated with the project domain.
     */
    @NonNull
    @JsonProperty("knowledge")
    private List<String> knowledge = List.of();
}
