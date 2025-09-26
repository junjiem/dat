package ai.dat.core.data.example;

import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.contentstore.data.WordSynonymPair;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;

/**
 * @Author JunjieM
 * @Date 2025/9/26
 */
@Setter
@Getter
public class Example {
    @NonNull
    @JsonProperty("sql_pairs")
    private List<QuestionSqlPair> questionSqlPairs = List.of();

    @NonNull
    @JsonProperty("synonyms_pairs")
    private List<WordSynonymPair> wordSynonymPairs = List.of();

    @NonNull
    @JsonProperty("knowledge")
    private List<String> knowledge = List.of();
}
