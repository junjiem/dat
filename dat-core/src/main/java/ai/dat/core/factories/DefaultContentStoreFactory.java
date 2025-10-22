package ai.dat.core.factories;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.contentstore.DefaultContentStore;
import ai.dat.core.contentstore.data.BusinessKnowledgeIndexingMethod;
import ai.dat.core.contentstore.data.SemanticModelRetrievalStrategy;
import ai.dat.core.factories.data.ChatModelInstance;
import ai.dat.core.utils.FactoryUtil;
import com.google.common.base.Preconditions;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.NonNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author JunjieM
 * @Date 2025/7/11
 */
public class DefaultContentStoreFactory implements ContentStoreFactory {

    public static final String IDENTIFIER = "default";

    public static final ConfigOption<Integer> MAX_RESULTS =
            ConfigOptions.key("max-results")
                    .intType()
                    .defaultValue(5)
                    .withDescription("Content store retrieve TopK maximum value, must be between 1 and 20");

    public static final ConfigOption<Double> MIN_SCORE =
            ConfigOptions.key("min-score")
                    .doubleType()
                    .defaultValue(0.6)
                    .withDescription("Content store retrieve Score minimum value, must be between 0.0 and 1.0");

    public static final ConfigOption<String> DEFAULT_LLM =
            ConfigOptions.key("default-llm")
                    .stringType()
                    .defaultValue("default")
                    .withDescription("Specify the default LLM model name.");

    // -------------------------------------------- Semantic Model -------------------------------------------------
    public static final ConfigOption<SemanticModelRetrievalStrategy> SEMANTIC_MODEL_RETRIEVAL_STRATEGY =
            ConfigOptions.key("semantic-model.retrieval-strategy")
                    .enumType(SemanticModelRetrievalStrategy.class)
                    .defaultValue(SemanticModelRetrievalStrategy.FE)
                    .withDescription("Semantic model embedding and retrieval strategy.\n" +
                            Arrays.stream(SemanticModelRetrievalStrategy.values())
                                    .map(e -> e.name() + ": " + e.getDescription())
                                    .collect(Collectors.joining("\n")));

    public static final ConfigOption<Integer> SEMANTIC_MODEL_CE_MAX_RESULTS =
            ConfigOptions.key("semantic-model.ce-max-results")
                    .intType()
                    .noDefaultValue()
                    .withDescription("Semantic model CE strategy retrieve TopK maximum value, must be between 1 and 200. " +
                            "If not specified, use the max-results.");

    public static final ConfigOption<Double> SEMANTIC_MODEL_CE_MIN_SCORE =
            ConfigOptions.key("semantic-model.ce-min-score")
                    .doubleType()
                    .noDefaultValue()
                    .withDescription("Semantic model CE strategy retrieve Score minimum value, must be between 0.0 and 1.0. " +
                            "If not specified, use the min-score.");

    public static final ConfigOption<String> SEMANTIC_MODEL_HYQE_LLM =
            ConfigOptions.key("semantic-model.hyqe-llm")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Semantic model HyQE strategy LLM model name. " +
                            "If not specified, use the default llm.");

    public static final ConfigOption<Integer> SEMANTIC_MODEL_HYQE_QUESTION_NUM =
            ConfigOptions.key("semantic-model.hyqe-question-num")
                    .intType()
                    .defaultValue(5)
                    .withDescription("Semantic model HyQE strategy generate the number of questions, must be between 3 and 20.");

    public static final ConfigOption<String> SEMANTIC_MODEL_HYQE_INSTRUCTION =
            ConfigOptions.key("semantic-model.hyqe-instruction")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Semantic model HyQE strategy instruction.");

    public static final ConfigOption<Integer> SEMANTIC_MODEL_HYQE_MAX_RESULTS =
            ConfigOptions.key("semantic-model.hyqe-max-results")
                    .intType()
                    .noDefaultValue()
                    .withDescription("Semantic model HyQE strategy retrieve TopK maximum value, must be between 1 and 50. " +
                            "If not specified, use the max-results.");

    public static final ConfigOption<Double> SEMANTIC_MODEL_HYQE_MIN_SCORE =
            ConfigOptions.key("semantic-model.hyqe-min-score")
                    .doubleType()
                    .noDefaultValue()
                    .withDescription("Semantic model HyQE strategy retrieve Score minimum value, must be between 0.0 and 1.0. " +
                            "If not specified, use the min-score.");
    // -------------------------------------------------------------------------------------------------------------

    // -------------------------------------------- Business Knowledge ---------------------------------------------
    public static final ConfigOption<BusinessKnowledgeIndexingMethod> BUSINESS_KNOWLEDGE_INDEXING_METHOD =
            ConfigOptions.key("business-knowledge.indexing-method")
                    .enumType(BusinessKnowledgeIndexingMethod.class)
                    .defaultValue(BusinessKnowledgeIndexingMethod.FE)
                    .withDescription("Business knowledge indexing method.\n" +
                            Arrays.stream(BusinessKnowledgeIndexingMethod.values())
                                    .map(e -> e.name() + ": " + e.getDescription())
                                    .collect(Collectors.joining("\n")));

    public static final ConfigOption<Integer> BUSINESS_KNOWLEDGE_INDEXING_GCE_MAX_CHUNK_SIZE =
            ConfigOptions.key("business-knowledge.indexing.gce-max-chunk-size")
                    .intType()
                    .defaultValue(4096)
                    .withDescription("Business knowledge `GCE` indexing method maximum chunk length.");

    public static final ConfigOption<Integer> BUSINESS_KNOWLEDGE_INDEXING_GCE_MAX_CHUNK_OVERLAP =
            ConfigOptions.key("business-knowledge.indexing.gce-max-chunk-overlap")
                    .intType()
                    .defaultValue(0)
                    .withDescription("Business knowledge `GCE` indexing method maximum chunk overlap length.");

    public static final ConfigOption<String> BUSINESS_KNOWLEDGE_INDEXING_GCE_CHUNK_REGEX =
            ConfigOptions.key("business-knowledge.indexing.gce-chunk-regex")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Business knowledge `GCE` indexing method split chunk regular expression. " +
                            "When it is empty, use the default built-in recursive split method.");

    public static final ConfigOption<Integer> BUSINESS_KNOWLEDGE_RETRIEVAL_MAX_RESULTS =
            ConfigOptions.key("business-knowledge.retrieval.max-results")
                    .intType()
                    .noDefaultValue()
                    .withDescription("Business knowledge retrieve TopK maximum value, must be between 1 and 100. " +
                            "If not specified, use the max-results.");

    public static final ConfigOption<Double> BUSINESS_KNOWLEDGE_RETRIEVAL_MIN_SCORE =
            ConfigOptions.key("business-knowledge.retrieval.min-score")
                    .doubleType()
                    .noDefaultValue()
                    .withDescription("Business knowledge retrieve Score minimum value, must be between 0.0 and 1.0. " +
                            "If not specified, use the min-score.");
    // -------------------------------------------------------------------------------------------------------------

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(MAX_RESULTS, MIN_SCORE, DEFAULT_LLM,

                SEMANTIC_MODEL_RETRIEVAL_STRATEGY,
                SEMANTIC_MODEL_CE_MAX_RESULTS, SEMANTIC_MODEL_CE_MIN_SCORE,
                SEMANTIC_MODEL_HYQE_LLM,
                SEMANTIC_MODEL_HYQE_INSTRUCTION, SEMANTIC_MODEL_HYQE_QUESTION_NUM,
                SEMANTIC_MODEL_HYQE_MAX_RESULTS, SEMANTIC_MODEL_HYQE_MIN_SCORE,

                BUSINESS_KNOWLEDGE_INDEXING_METHOD,
                BUSINESS_KNOWLEDGE_INDEXING_GCE_MAX_CHUNK_SIZE,
                BUSINESS_KNOWLEDGE_INDEXING_GCE_MAX_CHUNK_OVERLAP,
                BUSINESS_KNOWLEDGE_INDEXING_GCE_CHUNK_REGEX,
                BUSINESS_KNOWLEDGE_RETRIEVAL_MAX_RESULTS,
                BUSINESS_KNOWLEDGE_RETRIEVAL_MIN_SCORE
        ));
    }

    @Override
    public Set<ConfigOption<?>> fingerprintOptions() {
        return Set.of(SEMANTIC_MODEL_RETRIEVAL_STRATEGY,
                SEMANTIC_MODEL_HYQE_INSTRUCTION, SEMANTIC_MODEL_HYQE_QUESTION_NUM,

                BUSINESS_KNOWLEDGE_INDEXING_METHOD,
                BUSINESS_KNOWLEDGE_INDEXING_GCE_MAX_CHUNK_SIZE,
                BUSINESS_KNOWLEDGE_INDEXING_GCE_MAX_CHUNK_OVERLAP,
                BUSINESS_KNOWLEDGE_INDEXING_GCE_CHUNK_REGEX
        );
    }

    @Override
    public ContentStore create(@NonNull ReadableConfig config,
                               @NonNull EmbeddingModel embeddingModel,
                               @NonNull EmbeddingStore<TextSegment> mdlEmbeddingStore,
                               @NonNull EmbeddingStore<TextSegment> sqlEmbeddingStore,
                               @NonNull EmbeddingStore<TextSegment> synEmbeddingStore,
                               @NonNull EmbeddingStore<TextSegment> docEmbeddingStore,
                               @NonNull List<ChatModelInstance> chatModelInstances) {
        Preconditions.checkArgument(!chatModelInstances.isEmpty(),
                "chatModelInstances cannot be empty");
        FactoryUtil.validateFactoryOptions(this, config);
        Map<String, ChatModelInstance> instances = chatModelInstances.stream()
                .collect(Collectors.toMap(ChatModelInstance::getName, i -> i));
        validateConfigOptions(config, instances);

        Integer maxResults = config.get(MAX_RESULTS);
        Double minScore = config.get(MIN_SCORE);

        ChatModelInstance defaultInstance = config.getOptional(DEFAULT_LLM)
                .map(instances::get).orElseGet(() -> chatModelInstances.get(0));
        ChatModelInstance semanticModelHyQEInstance = config.getOptional(SEMANTIC_MODEL_HYQE_LLM)
                .map(instances::get).orElse(defaultInstance);

        SemanticModelRetrievalStrategy semanticModelRetrievalStrategy =
                config.get(SEMANTIC_MODEL_RETRIEVAL_STRATEGY);

        BusinessKnowledgeIndexingMethod businessKnowledgeIndexMethod =
                config.get(BUSINESS_KNOWLEDGE_INDEXING_METHOD);

        DefaultContentStore.DefaultContentStoreBuilder builder = DefaultContentStore.builder()
                .defaultChatModel(defaultInstance.getChatModel())
                .embeddingModel(embeddingModel)
                .mdlEmbeddingStore(mdlEmbeddingStore)
                .sqlEmbeddingStore(sqlEmbeddingStore)
                .synEmbeddingStore(synEmbeddingStore)
                .docEmbeddingStore(docEmbeddingStore)
                .maxResults(maxResults)
                .minScore(minScore)
                .mdlRetrievalStrategy(semanticModelRetrievalStrategy)
                .docIndexingMethod(businessKnowledgeIndexMethod);

        if (SemanticModelRetrievalStrategy.HYQE == semanticModelRetrievalStrategy) {
            builder.mdlHyQEChatModel(semanticModelHyQEInstance.getChatModel());
            config.getOptional(SEMANTIC_MODEL_HYQE_INSTRUCTION).ifPresent(builder::mdlHyQEInstruction);
            config.getOptional(SEMANTIC_MODEL_HYQE_QUESTION_NUM).ifPresent(builder::mdlHyQEQuestions);
            config.getOptional(SEMANTIC_MODEL_HYQE_MAX_RESULTS).ifPresent(builder::mdlHyQEMaxResults);
            config.getOptional(SEMANTIC_MODEL_HYQE_MIN_SCORE).ifPresent(builder::mdlHyQEMinScore);
        } else if (SemanticModelRetrievalStrategy.CE == semanticModelRetrievalStrategy) {
            config.getOptional(SEMANTIC_MODEL_CE_MAX_RESULTS).ifPresent(builder::mdlCEMaxResults);
            config.getOptional(SEMANTIC_MODEL_CE_MIN_SCORE).ifPresent(builder::mdlCEMinScore);
        }

        if (BusinessKnowledgeIndexingMethod.GCE == businessKnowledgeIndexMethod) {
            config.getOptional(BUSINESS_KNOWLEDGE_INDEXING_GCE_MAX_CHUNK_SIZE).ifPresent(builder::docGCEMaxChunkSize);
            config.getOptional(BUSINESS_KNOWLEDGE_INDEXING_GCE_MAX_CHUNK_OVERLAP).ifPresent(builder::docGCEMaxChunkOverlap);
            config.getOptional(BUSINESS_KNOWLEDGE_INDEXING_GCE_CHUNK_REGEX).ifPresent(builder::docGCEChunkRegex);
        }
        config.getOptional(BUSINESS_KNOWLEDGE_RETRIEVAL_MAX_RESULTS).ifPresent(builder::docMaxResults);
        config.getOptional(BUSINESS_KNOWLEDGE_RETRIEVAL_MIN_SCORE).ifPresent(builder::docMinScore);

        return builder.build();
    }

    private void validateConfigOptions(ReadableConfig config, Map<String, ChatModelInstance> instances) {
        Integer maxResults = config.get(MAX_RESULTS);
        Preconditions.checkArgument(maxResults >= 1 && maxResults <= 20,
                "'" + MAX_RESULTS.key() + "' value must be between 1 and 20");
        Double minScore = config.get(MIN_SCORE);
        Preconditions.checkArgument(minScore >= 0.0 && minScore <= 1.0,
                "'" + MIN_SCORE.key() + "' value must be between 0.0 and 1.0");

        String llmNames = String.join(", ", instances.keySet());
        String errorMessageFormat = "'%s' value must be one of [%s]";
        config.getOptional(DEFAULT_LLM)
                .ifPresent(n -> Preconditions.checkArgument(instances.containsKey(n),
                        String.format(errorMessageFormat, DEFAULT_LLM.key(), llmNames)));
        config.getOptional(SEMANTIC_MODEL_HYQE_LLM)
                .ifPresent(n -> Preconditions.checkArgument(instances.containsKey(n),
                        String.format(errorMessageFormat, SEMANTIC_MODEL_HYQE_LLM.key(), llmNames)));

        config.getOptional(SEMANTIC_MODEL_CE_MAX_RESULTS)
                .ifPresent(n -> Preconditions.checkArgument(n >= 1 && n <= 200,
                        "'" + SEMANTIC_MODEL_CE_MAX_RESULTS.key() + "' value must be between 1 and 200"));
        config.getOptional(SEMANTIC_MODEL_CE_MIN_SCORE)
                .ifPresent(n -> Preconditions.checkArgument(n >= 0.0 && n <= 1.0,
                        "'" + SEMANTIC_MODEL_CE_MIN_SCORE.key() + "' value must be between 0.0 and 1.0"));

        Integer semanticModelHyQEQuestionNum = config.get(SEMANTIC_MODEL_HYQE_QUESTION_NUM);
        Preconditions.checkArgument(semanticModelHyQEQuestionNum >= 3 && semanticModelHyQEQuestionNum <= 20,
                "'" + SEMANTIC_MODEL_HYQE_QUESTION_NUM.key() + "' value must be between 3 and 20");
        config.getOptional(SEMANTIC_MODEL_HYQE_MAX_RESULTS)
                .ifPresent(n -> Preconditions.checkArgument(n >= 1 && n <= 50,
                        "'" + SEMANTIC_MODEL_HYQE_MAX_RESULTS.key() + "' value must be between 1 and 50"));
        config.getOptional(SEMANTIC_MODEL_HYQE_MIN_SCORE)
                .ifPresent(n -> Preconditions.checkArgument(n >= 0.0 && n <= 1.0,
                        "'" + SEMANTIC_MODEL_HYQE_MIN_SCORE.key() + "' value must be between 0.0 and 1.0"));

        Integer businessKnowledgeGCEMaxChunkSize = config.get(BUSINESS_KNOWLEDGE_INDEXING_GCE_MAX_CHUNK_SIZE);
        Integer businessKnowledgeGCEMaxChunkOverlap = config.get(BUSINESS_KNOWLEDGE_INDEXING_GCE_MAX_CHUNK_OVERLAP);
        Preconditions.checkArgument(businessKnowledgeGCEMaxChunkSize > 0,
                "'" + BUSINESS_KNOWLEDGE_INDEXING_GCE_MAX_CHUNK_SIZE.key() + "' value must be greater than 0");
        Preconditions.checkArgument(businessKnowledgeGCEMaxChunkOverlap >= 0,
                "'" + BUSINESS_KNOWLEDGE_INDEXING_GCE_MAX_CHUNK_OVERLAP.key() + "' value must be greater than than or equal to 0");
        Preconditions.checkArgument(businessKnowledgeGCEMaxChunkSize > businessKnowledgeGCEMaxChunkOverlap,
                "'" + BUSINESS_KNOWLEDGE_INDEXING_GCE_MAX_CHUNK_SIZE.key() + "' value must be less than '"
                        + BUSINESS_KNOWLEDGE_INDEXING_GCE_MAX_CHUNK_OVERLAP.key() + "' value");
        config.getOptional(BUSINESS_KNOWLEDGE_RETRIEVAL_MAX_RESULTS)
                .ifPresent(n -> Preconditions.checkArgument(n >= 1 && n <= 100,
                        "'" + BUSINESS_KNOWLEDGE_RETRIEVAL_MAX_RESULTS.key() + "' value must be between 1 and 100"));
        config.getOptional(BUSINESS_KNOWLEDGE_RETRIEVAL_MIN_SCORE)
                .ifPresent(n -> Preconditions.checkArgument(n >= 0.0 && n <= 1.0,
                        "'" + BUSINESS_KNOWLEDGE_RETRIEVAL_MIN_SCORE.key() + "' value must be between 0.0 and 1.0"));
    }
}
