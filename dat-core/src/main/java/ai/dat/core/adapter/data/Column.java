package ai.dat.core.adapter.data;

import lombok.Getter;
import lombok.NonNull;

/**
 * @Author JunjieM
 * @Date 2025/9/12
 */
@Getter
public class Column {
    @NonNull
    private final String name;

    private String type;

    public Column(@NonNull String name) {
        this.name = name;
    }

    public Column(@NonNull String name, String type) {
        this.name = name;
        this.type = type;
    }
}