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
 * Configuration wrapper for a large language model provider within a DAT project.
 */
@Setter
@Getter
public class LlmConfig {

    public static final String DEFAULT_NAME = "default";
    public static final String DEFAULT_PROVIDER = "openai";

    /**
     * Logical name assigned to the language model configuration.
     */
    @NonNull
    private String name = DEFAULT_NAME;

    /**
     * Identifier of the LLM provider.
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