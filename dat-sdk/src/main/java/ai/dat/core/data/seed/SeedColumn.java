package ai.dat.core.data.seed;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Represents a column definition within a seed dataset, including its name, description,
 * and optional data type metadata.
 */
@Setter
@Getter
public class SeedColumn {
    /**
     * Column name as it appears in the seed dataset.
     */
    @NonNull
    private String name;

    /**
     * Human-readable description that explains the purpose of the column.
     */
    @NonNull
    private String description;

    /**
     * Optional data type information associated with the column.
     */
    @JsonProperty("data_type")
    private String dataType;
}
