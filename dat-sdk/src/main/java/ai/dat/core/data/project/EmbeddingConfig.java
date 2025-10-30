package ai.dat.core.data.project;

import ai.dat.core.configuration.Configuration;
import ai.dat.core.configuration.ReadableConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Map;

/**
 * Configuration wrapper for the embedding model provider used by a DAT project.
 */
@Setter
@Getter
public class EmbeddingConfig {

    public static final String DEFAULT_PROVIDER = "bge-small-zh-v15-q";

    /**
     * Identifier of the embedding model provider.
     */
    @NonNull
    private String provider = DEFAULT_PROVIDER;

    @JsonIgnore
    @NonNull
    private ReadableConfig configuration = new Configuration();

    /**
     * Deserializes configuration properties from YAML into a {@link ReadableConfig} instance.
     *
     * @param configs the raw configuration map
     */
    @JsonProperty("configuration")
    public void setConfiguration(Map<String, Object> configs) {
        this.configuration = Configuration.fromMap(configs);
    }
}