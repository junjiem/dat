package ai.dat.core.data.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Setter
@Getter
public class DatProject {

    @NonNull
    private Integer version = 1;

    @NonNull
    private String name;

    private String description;

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