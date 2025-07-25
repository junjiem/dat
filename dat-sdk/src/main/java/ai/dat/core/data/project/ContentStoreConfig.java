package ai.dat.core.data.project;

import ai.dat.core.configuration.Configuration;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.DefaultContentStoreFactory;
import ai.dat.core.utils.ConfigurationConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class ContentStoreConfig {

    @NonNull
    private String provider = DefaultContentStoreFactory.IDENTIFIER;

    @NonNull
    private String llm = "default";

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