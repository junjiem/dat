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

/**
 * Represents the root configuration for a DAT project including providers, agents, and resources.
 */
@Setter
@Getter
public class DatProject {

    /**
     * Schema version of the project configuration.
     */
    @NonNull
    private Integer version = 1;

    /**
     * Project name used to identify resources.
     */
    @NonNull
    private String name;

    /**
     * Optional description of the project.
     */
    private String description;

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

    /**
     * Database configuration used to connect to the source system.
     */
    @NonNull
    private DatabaseConfig db;

    /**
     * Embedding model configuration for vectorization tasks.
     */
    @NonNull
    private EmbeddingConfig embedding = new EmbeddingConfig();

    /**
     * Embedding store configuration that controls vector storage.
     */
    @NonNull
    @JsonProperty("embedding_store")
    private EmbeddingStoreConfig embeddingStore = new EmbeddingStoreConfig();

    /**
     * Collection of large language model configurations available to the project.
     */
    @NonNull
    private List<LlmConfig> llms;

    /**
     * Assigns LLM configurations while ensuring unique logical names.
     *
     * @param llms the language model configurations to assign
     * @throws IllegalArgumentException if duplicate names are encountered
     */
    public void setLlms(List<LlmConfig> llms) {
        Set<String> names = new HashSet<>();
        for (LlmConfig llm : llms) {
            String name = llm.getName();
            Preconditions.checkArgument(names.add(name),
                    String.format("There is duplicate llm name '%s' in the llms", name));
        }
        this.llms = llms;
    }

    /**
     * Optional reranking model configuration.
     */
    private RerankingConfig reranking;

    /**
     * Content store configuration controlling project artifacts.
     */
    @NonNull
    @JsonProperty("content_store")
    private ContentStoreConfig contentStore = new ContentStoreConfig();

    /**
     * Agent configurations that expose project capabilities.
     */
    @NonNull
    private List<AgentConfig> agents = Collections.emptyList();

    /**
     * Assigns agent configurations while ensuring unique logical names.
     *
     * @param agents the agent configurations to assign
     * @throws IllegalArgumentException if duplicate names are encountered
     */
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