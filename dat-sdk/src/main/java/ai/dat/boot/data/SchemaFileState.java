package ai.dat.boot.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the persisted state for a schema YAML file and its related artifacts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchemaFileState {
    /**
     * Relative path of the schema file within the models directory.
     */
    private String relativePath;

    /**
     * Last modification timestamp of the schema file, in milliseconds.
     */
    private long lastModified;

    /**
     * MD5 hash of the schema file contents.
     */
    private String md5Hash;

    /**
     * Names of semantic models defined in the schema file.
     */
    private List<String> semanticModelNames = List.of();

    /**
     * Vector identifiers in the content store for semantic models.
     * <p>
     * The legacy field name was {@code vectorIds}; {@link JsonProperty} is retained for compatibility.
     */
    @JsonProperty("vectorIds")
    private List<String> semanticModelVectorIds = List.of();

    /**
     * Metadata about SQL model files referenced by the schema.
     * <p>
     * The legacy field name was {@code dependencies}; {@link JsonProperty} is retained for compatibility.
     */
    @JsonProperty("dependencies")
    private List<RelevantFileState> modelFileStates = List.of();

    /**
     * Vector identifiers for question-SQL pairs stored in the content store.
     */
    @JsonProperty("sqlPairVectorIds")
    private List<String> questionSqlPairVectorIds = List.of();

    /**
     * Vector identifiers for synonym pairs stored in the content store.
     */
    @JsonProperty("synonymPairVectorIds")
    private List<String> wordSynonymPairVectorIds = List.of();

    /**
     * Vector identifiers for knowledge documents stored in the content store.
     */
    @JsonProperty("knowledgeVectorIds")
    private List<String> knowledgeVectorIds = List.of();
}