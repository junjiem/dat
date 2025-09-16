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
 * @Author JunjieM
 * @Date 2025/9/12
 */
@Setter
@Getter
public class SeedSpec {
    @NonNull
    private String name;

    @NonNull
    private String description;

    @NonNull
    private SeedConfig config = new SeedConfig();

    @NonNull
    private List<SeedColumn> columns = List.of();

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
