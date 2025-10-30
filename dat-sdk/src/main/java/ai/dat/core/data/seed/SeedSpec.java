package ai.dat.core.data.seed;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Describes the metadata, configuration, and schema for a seed dataset used to bootstrap a project.
 */
@Setter
@Getter
public class SeedSpec {
    /**
     * Logical name of the seed dataset.
     */
    @NonNull
    private String name;

    /**
     * Human-readable description for the seed dataset.
     */
    @NonNull
    private String description;

    /**
     * Configuration that determines how the seed data should be processed.
     */
    @NonNull
    private SeedConfig config = new SeedConfig();

    /**
     * Column definitions that compose the seed dataset schema.
     */
    @NonNull
    private List<SeedColumn> columns = List.of();

    /**
     * Configures the seed columns while ensuring column names remain unique.
     *
     * @param columns the column definitions to apply
     * @throws IllegalArgumentException if duplicate column names are detected
     */
    public void setColumns(@NonNull List<SeedColumn> columns) {
        Set<String> names = new HashSet<>();
        for (SeedColumn column : columns) {
            String name = column.getName();
            Preconditions.checkArgument(names.add(name),
                    String.format("There is duplicate column name '%s' in the seed%s", name,
                            StringUtils.isBlank(this.name) ? "" : " '" + this.name + "'"));
        }
        this.columns = columns;
    }
}
