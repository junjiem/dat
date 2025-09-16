package ai.dat.core.data;

import ai.dat.core.data.seed.SeedSpec;
import ai.dat.core.semantic.data.SemanticModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class DatSchema {
    @NonNull
    private Integer version = 1;

    @NonNull
    @JsonProperty("semantic_models")
    private List<SemanticModel> semanticModels = List.of();

    @NonNull
    @JsonProperty("seeds")
    private List<SeedSpec> seeds = List.of();
}