package ai.dat.core.data.seed;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * @Author JunjieM
 * @Date 2025/9/12
 */
@Setter
@Getter
public class SeedConfig {
    @NonNull
    private String delimiter = ",";
}
