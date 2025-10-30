package ai.dat.core.data.seed;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Configuration options that control how seed data files are parsed.
 */
@Setter
@Getter
public class SeedConfig {
    /**
     * Delimiter used to split columns within the seed CSV.
     */
    @NonNull
    private String delimiter = ",";
}
