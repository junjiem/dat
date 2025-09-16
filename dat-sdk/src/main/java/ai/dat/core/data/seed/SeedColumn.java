package ai.dat.core.data.seed;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * @Author JunjieM
 * @Date 2025/9/12
 */
@Setter
@Getter
public class SeedColumn {
    @NonNull
    private String name;

    @NonNull
    private String description;

    @JsonProperty("data_type")
    private String dataType;
}
