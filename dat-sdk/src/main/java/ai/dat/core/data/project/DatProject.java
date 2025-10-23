package ai.dat.core.data.project;

import ai.dat.core.configuration.Configuration;
import ai.dat.core.configuration.ReadableConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.*;

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

    public void setLlms(List<LlmConfig> llms) {
        Set<String> names = new HashSet<>();
        for (LlmConfig llm : llms) {
            String name = llm.getName();
            Preconditions.checkArgument(names.add(name),
                    String.format("There is duplicate llm name '%s' in the llms", name));
        }
        this.llms = llms;
    }

    private RerankingConfig reranking;

    @NonNull
    @JsonProperty("content_store")
    private ContentStoreConfig contentStore = new ContentStoreConfig();

    @NonNull
    private List<AgentConfig> agents = Collections.emptyList();

    public void setAgents(List<AgentConfig> agents) {
        Set<String> names = new HashSet<>();
        for (AgentConfig agent : agents) {
            String name = agent.getName();
            Preconditions.checkArgument(names.add(name),
                    String.format("There is duplicate agent name '%s' in the agents", name));
        }
        this.agents = agents;
    }
}