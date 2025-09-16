package ai.dat.core.data.seed;

import ai.dat.core.adapter.data.Table;
import lombok.NonNull;

import java.util.List;

/**
 * @Author JunjieM
 * @Date 2025/9/12
 */
public record SeedData(@NonNull Table table, @NonNull List<List<String>> data) {
}