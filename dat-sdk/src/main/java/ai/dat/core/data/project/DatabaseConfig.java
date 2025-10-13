package ai.dat.core.data.project;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.Configuration;
import ai.dat.core.configuration.ReadableConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class DatabaseConfig {

    public static final String DUCKDB_PROVIDER = "duckdb";

    public static final ConfigOption<String> DUCKDB_FILE_PATH =
            ConfigOptions.key("file-path")
                    .stringType()
                    .noDefaultValue();

    public static final String DEFAULT_PROVIDER = "postgresql";

    @NonNull
    private String provider = DEFAULT_PROVIDER;

    @JsonIgnore
    @NonNull
    private ReadableConfig configuration = new Configuration();

    @JsonProperty("configuration")
    public void setConfiguration(Map<String, Object> configs) {
        this.configuration = Configuration.fromMap(configs);
    }
}