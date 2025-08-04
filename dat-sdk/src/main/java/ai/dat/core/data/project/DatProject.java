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
public class DatProject {

    @NonNull
    private Integer version = 1;

    @NonNull
    private String name;

    private String description;

    @JsonIgnore
    @NonNull
    private ReadableConfig configuration = new Configuration();

    @JsonProperty("configuration")
    public void setConfiguration(Map<String, Object> configs) {
        this.configuration = Configuration.fromMap(configs);
    }

    @NonNull
    private DatabaseConfig db;

    @NonNull
    private EmbeddingConfig embedding = new EmbeddingConfig();

    @NonNull
    @JsonProperty("embedding_store")
    private EmbeddingStoreConfig embeddingStore = new EmbeddingStoreConfig();

    @NonNull
    private List<LlmConfig> llms;

    @NonNull
    @JsonProperty("content_store")
    private ContentStoreConfig contentStore = new ContentStoreConfig();

    @NonNull
    private List<AgentConfig> agents = Collections.emptyList();

}