package ai.dat.core.adapter.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;

/**
 *
 */
@Getter
@AllArgsConstructor
public class Table {
    @NonNull
    private String name;

    @NonNull
    private List<Column> columns;
}