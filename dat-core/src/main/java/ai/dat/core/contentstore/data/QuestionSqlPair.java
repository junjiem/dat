package ai.dat.core.contentstore.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;

/**
 * 问题与SQL对
 *
 * @Author JunjieM
 * @Date 2025/6/25
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionSqlPair {
    @NonNull
    private final String question;

    @NonNull
    private final String sql;

    @JsonCreator
    private QuestionSqlPair(@JsonProperty("question") @NonNull String question,
                            @JsonProperty("sql") @NonNull String sql) {
        this.question = question;
        this.sql = sql;
    }

    public static QuestionSqlPair from(@NonNull String question, @NonNull String sql) {
        return new QuestionSqlPair(question, sql);
    }
}
