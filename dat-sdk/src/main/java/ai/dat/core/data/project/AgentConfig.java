package ai.dat.core.data.project;

import ai.dat.core.configuration.Configuration;
import ai.dat.core.configuration.ReadableConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Setter
@Getter
public class AgentConfig {

    public static final String DEFAULT_NAME = "default";
    public static final String DEFAULT_PROVIDER = "default";

    @NonNull
    private String name = DEFAULT_NAME;

    private String description;

    @NonNull
    private String provider = DEFAULT_PROVIDER;

    @NonNull
    @JsonProperty("semantic_models")
    private List<String> semanticModels = Collections.emptyList();

    @JsonIgnore
    @NonNull
    private ReadableConfig configuration = new Configuration();

    @JsonProperty("configuration")
    public void setConfiguration(Map<String, Object> configs) {
        this.configuration = Configuration.fromMap(configs);
    }
}