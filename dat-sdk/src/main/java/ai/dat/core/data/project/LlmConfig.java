package ai.dat.core.data.project;

import ai.dat.core.configuration.Configuration;
import ai.dat.core.configuration.ReadableConfig;
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

    public static final String DEFAULT_NAME = "default";
    public static final String DEFAULT_PROVIDER = "openai";

    @NonNull
    private String name = DEFAULT_NAME;

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