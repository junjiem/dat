package ai.dat.core.data.project;

import ai.dat.core.configuration.Configuration;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.utils.ConfigurationConverter;
import ai.dat.llm.openai.OpneAiChatModelFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class LlmConfig {

    @NonNull
    private String name = "default";

    @NonNull
    private String provider = OpneAiChatModelFactory.IDENTIFIER;

    @JsonIgnore
    @NonNull
    private ReadableConfig configuration = new Configuration();

    @JsonProperty("configuration")
    public Map<String, Object> getConfigurationMap() {
        return ConfigurationConverter.toNestedMap(configuration);
    }

    @JsonProperty("configuration")
    public void setConfigurationMap(Map<String, Object> configMap) {
        this.configuration = ConfigurationConverter.fromMap(configMap);
    }
}