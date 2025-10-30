package ai.dat.core.contentstore.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 词与其近义词对
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WordSynonymPair {
    @NonNull
    private final String word;

    @NonNull
    private final List<String> synonyms;

    @JsonCreator
    private WordSynonymPair(@JsonProperty("word") @NonNull String word,
                            @JsonProperty("synonyms") @NonNull List<String> synonyms) {
        this.word = word;
        this.synonyms = synonyms;
    }

    public static WordSynonymPair from(@NonNull String word, @NonNull String synonym) {
        return new WordSynonymPair(word, Collections.singletonList(synonym));
    }

    public static WordSynonymPair from(@NonNull String word, @NonNull List<String> synonyms) {
        return new WordSynonymPair(word, synonyms);
    }

    public static WordSynonymPair from(@NonNull String word, @NonNull String... synonyms) {
        return new WordSynonymPair(word, Arrays.asList(synonyms));
    }

    public void add(@NonNull String synonym) {
        synonyms.add(synonym);
    }
}
