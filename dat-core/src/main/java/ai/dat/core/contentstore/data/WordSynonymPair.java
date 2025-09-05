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
 *
 * @Author JunjieM
 * @Date 2025/6/30
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WordSynonymPair {
    @NonNull
    private final String noun;

    @NonNull
    private final List<String> synonyms;

    private String description;

    @JsonCreator
    private WordSynonymPair(@JsonProperty("noun") @NonNull String noun,
                            @JsonProperty("synonyms") @NonNull List<String> synonyms) {
        this.noun = noun;
        this.synonyms = synonyms;
    }

    @JsonCreator
    private WordSynonymPair(@JsonProperty("noun") @NonNull String noun,
                            @JsonProperty("synonyms") @NonNull List<String> synonyms,
                            @JsonProperty("description") String description) {
        this.noun = noun;
        this.synonyms = synonyms;
        this.description = description;
    }

    public static WordSynonymPair from(@NonNull String noun, @NonNull String synonym, String description) {
        return new WordSynonymPair(noun, Collections.singletonList(synonym), description);
    }

    public static WordSynonymPair from(@NonNull String noun, @NonNull List<String> synonyms, String description) {
        return new WordSynonymPair(noun, synonyms, description);
    }

    public static WordSynonymPair from(@NonNull String noun, @NonNull String synonym) {
        return new WordSynonymPair(noun, Collections.singletonList(synonym));
    }

    public static WordSynonymPair from(@NonNull String noun, @NonNull List<String> synonyms) {
        return new WordSynonymPair(noun, synonyms);
    }

    public static WordSynonymPair from(@NonNull String noun, @NonNull String... synonyms) {
        return new WordSynonymPair(noun, Arrays.asList(synonyms));
    }

    public void add(@NonNull String synonym) {
        synonyms.add(synonym);
    }
}
