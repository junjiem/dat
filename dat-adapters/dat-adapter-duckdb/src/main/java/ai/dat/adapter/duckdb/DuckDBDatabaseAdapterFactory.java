package ai.dat.adapter.duckdb;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.DatabaseAdapterFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * @Author JunjieM
 * @Date 2025/7/4
 */
public class DuckDBDatabaseAdapterFactory implements DatabaseAdapterFactory {

    public static final String IDENTIFIER = "duckdb";

    public static final ConfigOption<String> FILE_PATH =
            ConfigOptions.key("file-path")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("""
                            DuckDB database store file path.
                            The project mode does not need to be filled in by default and is stored \
                            in the project root directory under the .dat directory, \
                            files with the prefix 'duckdb'.
                            """);

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(FILE_PATH));
    }

    @Override
    public DatabaseAdapter create(ReadableConfig config) {
        String filePath = config.get(FILE_PATH);
        return new DuckDBDatabaseAdapter(new DuckDBDataSource(filePath));
    }
}
