package ai.dat.core.data.seed;

import ai.dat.core.adapter.data.Table;
import lombok.NonNull;

import java.util.List;

/**
 * Bundles a table definition with its corresponding seed rows for ingestion.
 *
 * @param table the table metadata that describes the seed dataset
 * @param data the tabular data rows, each represented as a list of strings
 */
public record SeedData(@NonNull Table table, @NonNull List<List<String>> data) {
}