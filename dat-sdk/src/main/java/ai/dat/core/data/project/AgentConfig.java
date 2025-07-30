package ai.dat.core.data.project;

import ai.dat.core.configuration.Configuration;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.DefaultAskdataAgentFactory;
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

    @NonNull
    private String name = "default";

    private String description;

    @NonNull
    private String provider = DefaultAskdataAgentFactory.IDENTIFIER;

    @NonNull
    private String llm = "default";

    @NonNull
    @JsonProperty("semantic_models")
    private List<String> semanticModels = Collections.emptyList();

    @JsonIgnore
    @NonNull
    private ReadableConfig configuration = new Configuration();

    @JsonProperty("configuration")
    public void setConfigurationMap(Map<String, Object> configMap) {
        this.configuration = Configuration.fromMap(configMap);
    }
}