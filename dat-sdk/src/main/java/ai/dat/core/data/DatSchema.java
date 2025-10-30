package ai.dat.core.data;

import ai.dat.core.data.example.Example;
import ai.dat.core.data.seed.SeedSpec;
import ai.dat.core.semantic.data.SemanticModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;

/**
 * Represents the contents of a schema YAML file, including semantic models, seeds, and examples.
 */
@Setter
@Getter
public class DatSchema {
    /**
     * Schema version number.
     */
    @NonNull
    private Integer version = 1;

    /**
     * Semantic models defined within the schema.
     */
    @NonNull
    @JsonProperty("semantic_models")
    private List<SemanticModel> semanticModels = List.of();

    /**
     * Seed specifications declared within the schema.
     */
    @NonNull
    @JsonProperty("seeds")
    private List<SeedSpec> seeds = List.of();

    /**
     * Optional example data associated with the schema.
     */
    @JsonProperty("examples")
    private Example example;
}