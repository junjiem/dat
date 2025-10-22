package ai.dat.core.contentstore;

import ai.dat.core.contentstore.data.BusinessKnowledgeIndexingMethod;
import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.contentstore.data.SemanticModelRetrievalStrategy;
import ai.dat.core.contentstore.data.WordSynonymPair;
import ai.dat.core.contentstore.utils.ContentStoreUtil;
import ai.dat.core.semantic.data.SemanticModel;
import ai.dat.core.semantic.view.ElementView;
import ai.dat.core.semantic.view.SemanticModelView;
import ai.dat.core.utils.SemanticModelUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 默认实现的内存存储器类
 *
 * @Author JunjieM
 * @Date 2025/6/25
 */
public class DefaultContentStore implements ContentStore {

    public static final String METADATA_CONTENT_TYPE = "content_type";

    private static final Metadata MDL_METADATA = Metadata.from(METADATA_CONTENT_TYPE, ContentType.MDL.toString());
    private static final Metadata SQL_METADATA = Metadata.from(METADATA_CONTENT_TYPE, ContentType.SQL.toString());
    private static final Metadata SYN_METADATA = Metadata.from(METADATA_CONTENT_TYPE, ContentType.SYN.toString());
    private static final Metadata DOC_METADATA = Metadata.from(METADATA_CONTENT_TYPE, ContentType.DOC.toString());

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final ChatModel defaultChatModel;

    private final EmbeddingModel embeddingModel;

    private final EmbeddingStore<TextSegment> mdlEmbeddingStore;
    private final EmbeddingStore<TextSegment> sqlEmbeddingStore;
    private final EmbeddingStore<TextSegment> synEmbeddingStore;
    private final EmbeddingStore<TextSegment> docEmbeddingStore;

    private final Integer maxResults;
    private final Double minScore;

    // -------------------------------------------- Semantic Model -------------------------------------------------
    private final SemanticModelRetrievalStrategy mdlRetrievalStrategy;

    private final MdlHyQEAssistant mdlHyQEAssistant;
    private final String mdlHyQEInstruction;
    private final Integer mdlHyQEQuestions;
    private final Integer mdlHyQEMaxResults;
    private final Double mdlHyQEMinScore;

    private final Integer mdlCEMaxResults;
    private final Double mdlCEMinScore;
    // -------------------------------------------------------------------------------------------------------------

    // -------------------------------------------- Business Knowledge ---------------------------------------------
    private final BusinessKnowledgeIndexingMethod docIndexingMethod;

    private final Integer docGCEMaxChunkSize;
    private final Integer docGCEMaxChunkOverlap;
    private final String docGCEChunkRegex;

    private final Integer docMaxResults;
    private final Double docMinScore;
    // -------------------------------------------------------------------------------------------------------------

    @Builder
    public DefaultContentStore(@NonNull ChatModel defaultChatModel,
                               @NonNull EmbeddingModel embeddingModel,
                               @NonNull EmbeddingStore<TextSegment> mdlEmbeddingStore,
                               @NonNull EmbeddingStore<TextSegment> sqlEmbeddingStore,
                               @NonNull EmbeddingStore<TextSegment> synEmbeddingStore,
                               @NonNull EmbeddingStore<TextSegment> docEmbeddingStore,
                               Integer maxResults, Double minScore,
                               SemanticModelRetrievalStrategy mdlRetrievalStrategy,
                               Integer mdlCEMaxResults, Double mdlCEMinScore,
                               ChatModel mdlHyQEChatModel,
                               String mdlHyQEInstruction, Integer mdlHyQEQuestions,
                               Integer mdlHyQEMaxResults, Double mdlHyQEMinScore,
                               BusinessKnowledgeIndexingMethod docIndexingMethod,
                               Integer docGCEMaxChunkSize, Integer docGCEMaxChunkOverlap,
                               String docGCEChunkRegex,
                               Integer docMaxResults, Double docMinScore) {
        this.defaultChatModel = defaultChatModel;
        this.embeddingModel = embeddingModel;
        this.mdlEmbeddingStore = mdlEmbeddingStore;
        this.sqlEmbeddingStore = sqlEmbeddingStore;
        this.synEmbeddingStore = synEmbeddingStore;
        this.docEmbeddingStore = docEmbeddingStore;

        this.maxResults = Optional.ofNullable(maxResults).orElse(5);
        Preconditions.checkArgument(this.maxResults <= 20 && this.maxResults >= 1,
                "maxResults must be between 1 and 20");
        this.minScore = Optional.ofNullable(minScore).orElse(0.6);
        Preconditions.checkArgument(this.minScore >= 0.0 && this.minScore <= 1.0,
                "minScore must be between 0.0 and 1.0");

        // -------------------------------------------- Semantic Model ------------------------------------------
        this.mdlRetrievalStrategy = Optional.ofNullable(mdlRetrievalStrategy)
                .orElse(SemanticModelRetrievalStrategy.FE);

        this.mdlCEMaxResults = Optional.ofNullable(mdlCEMaxResults).orElse(maxResults);
        Preconditions.checkArgument(this.mdlCEMaxResults <= 200 && this.mdlCEMaxResults >= 1,
                "mdlCEMaxResults must be between 1 and 200");
        this.mdlCEMinScore = Optional.ofNullable(mdlCEMinScore).orElse(minScore);
        Preconditions.checkArgument(this.mdlCEMinScore >= 0.0 && this.mdlCEMinScore <= 1.0,
                "mdlCEMinScore must be between 0.0 and 1.0");

        this.mdlHyQEAssistant = AiServices.builder(MdlHyQEAssistant.class)
                .chatModel(Objects.requireNonNullElse(mdlHyQEChatModel, defaultChatModel))
                .build();
        this.mdlHyQEInstruction = Optional.ofNullable(mdlHyQEInstruction).orElse("");
        this.mdlHyQEQuestions = Optional.ofNullable(mdlHyQEQuestions).orElse(5);
        Preconditions.checkArgument(this.mdlHyQEQuestions <= 20 && this.mdlHyQEQuestions >= 3,
                "mdlHyQEQuestions must be between 3 and 20");
        this.mdlHyQEMaxResults = Optional.ofNullable(mdlHyQEMaxResults).orElse(maxResults);
        Preconditions.checkArgument(this.mdlHyQEMaxResults <= 50 && this.mdlHyQEMaxResults >= 1,
                "mdlHyQEMaxResults must be between 1 and 50");
        this.mdlHyQEMinScore = Optional.ofNullable(mdlHyQEMinScore).orElse(minScore);
        Preconditions.checkArgument(this.mdlHyQEMinScore >= 0.0 && this.mdlHyQEMinScore <= 1.0,
                "mdlHyQEMinScore must be between 0.0 and 1.0");
        // -----------------------------------------------------------------------------------------------------

        // -------------------------------------------- Business Knowledge -------------------------------------
        this.docIndexingMethod = Optional.ofNullable(docIndexingMethod)
                .orElse(BusinessKnowledgeIndexingMethod.FE);

        this.docGCEMaxChunkSize = Optional.ofNullable(docGCEMaxChunkSize).orElse(4096);
        Preconditions.checkArgument(this.docGCEMaxChunkSize > 0,
                "docGCEMaxChunkSize must be greater than 0");
        this.docGCEMaxChunkOverlap = Optional.ofNullable(docGCEMaxChunkOverlap).orElse(0);
        Preconditions.checkArgument(this.docGCEMaxChunkOverlap >= 0,
                "docGCEMaxChunkOverlap must be greater than than or equal to 0");
        Preconditions.checkArgument(this.docGCEMaxChunkSize > this.docGCEMaxChunkOverlap,
                "docGCEMaxChunkOverlap value must be less than docGCEMaxChunkSize value");
        this.docGCEChunkRegex = docGCEChunkRegex;

        this.docMaxResults = Optional.ofNullable(docMaxResults).orElse(this.maxResults);
        Preconditions.checkArgument(this.docMaxResults <= 100 && this.docMaxResults >= 1,
                "docMaxResults must be between 1 and 100");
        this.docMinScore = Optional.ofNullable(docMinScore).orElse(this.minScore);
        Preconditions.checkArgument(this.docMinScore >= 0.0 && this.docMinScore <= 1.0,
                "docMinScore must be between 0.0 and 1.0");
        // -----------------------------------------------------------------------------------------------------
    }

    @Override
    public List<String> addMdls(List<SemanticModel> semanticModels) {
        if (SemanticModelRetrievalStrategy.HYQE == mdlRetrievalStrategy) {
            return addMdlsForHyQE(semanticModels);
        } else if (SemanticModelRetrievalStrategy.CE == mdlRetrievalStrategy) {
            return addMdlsForCE(semanticModels);
        }
        return addMdlsForFE(semanticModels);
    }

    private List<String> addMdlsForHyQE(List<SemanticModel> semanticModels) {
        return semanticModels.stream()
                .map(semanticModel -> {
                    SemanticModelUtil.validateSemanticModel(semanticModel);
                    String semanticModelViewText = SemanticModelUtil.toSemanticModelViewText(semanticModel);
                    List<String> questions = mdlHyQEAssistant.genHypotheticalQuestions(
                            mdlHyQEInstruction, mdlHyQEQuestions, semanticModelViewText);
                    if (questions == null || questions.isEmpty()) {
                        return null;
                    }
                    String json;
                    try {
                        json = JSON_MAPPER.writeValueAsString(semanticModel);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to serialize semantic model to JSON: "
                                + e.getMessage(), e);
                    }
                    TextSegment textSegment = TextSegment.from(json, MDL_METADATA);
                    List<TextSegment> embedTextSegments = questions.stream().map(TextSegment::from).toList();
                    List<Embedding> embeddings = embeddingModel.embedAll(embedTextSegments).content();
                    List<TextSegment> textSegments = questions.stream()
                            .map(question -> textSegment)
                            .collect(Collectors.toList());
                    return mdlEmbeddingStore.addAll(embeddings, textSegments);
                })
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private interface MdlHyQEAssistant {
        @SystemMessage(fromResource = "prompts/default/generation_hypothetical_questions_system_prompt.txt")
        @UserMessage(fromResource = "prompts/default/generation_hypothetical_questions_user_prompt.txt")
        List<String> genHypotheticalQuestions(@V("instruction") String instruction,
                                              @V("question_num") Integer questionNum,
                                              @V("semantic_model") String semanticModel);
    }

    private List<String> addMdlsForCE(List<SemanticModel> semanticModels) {
        return semanticModels.stream()
                .map(semanticModel -> {
                    SemanticModelUtil.validateSemanticModel(semanticModel);
                    SemanticModelView semanticModelView = SemanticModelUtil.toSemanticModelView(semanticModel);
                    List<String> columnTexts = Stream.of(
                                    semanticModelView.getEntities().stream(),
                                    semanticModelView.getDimensions().stream(),
                                    semanticModelView.getMeasures().stream()
                            )
                            .flatMap(Function.identity())
                            .map(o -> new SemanticModelColumnView(semanticModelView, (ElementView) o))
                            .map(c -> {
                                try {
                                    return JSON_MAPPER.writeValueAsString(c);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException("Failed to serialize semantic model column view to JSON: "
                                            + e.getMessage(), e);
                                }
                            }).toList();
                    String json;
                    try {
                        json = JSON_MAPPER.writeValueAsString(semanticModel);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to serialize semantic model to JSON: "
                                + e.getMessage(), e);
                    }
                    TextSegment textSegment = TextSegment.from(json, MDL_METADATA);
                    List<TextSegment> embedTextSegments = columnTexts.stream().map(TextSegment::from).toList();
                    List<Embedding> embeddings = embeddingModel.embedAll(embedTextSegments).content();
                    List<TextSegment> textSegments = columnTexts.stream()
                            .map(question -> textSegment)
                            .collect(Collectors.toList());
                    return mdlEmbeddingStore.addAll(embeddings, textSegments);
                })
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<String> addMdlsForFE(List<SemanticModel> semanticModels) {
        List<TextSegment> textSegments = semanticModels.stream()
                .map(semanticModel -> {
                    SemanticModelUtil.validateSemanticModel(semanticModel);
                    String json;
                    try {
                        json = JSON_MAPPER.writeValueAsString(semanticModel);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to serialize semantic model to JSON: "
                                + e.getMessage(), e);
                    }
                    return TextSegment.from(json, MDL_METADATA);
                }).collect(Collectors.toList());
        List<TextSegment> embedTextSegments = semanticModels.stream()
                .map(SemanticModelUtil::toSemanticModelViewText)
                .map(TextSegment::from)
                .toList();
        List<Embedding> embeddings = embeddingModel.embedAll(embedTextSegments).content();
        return mdlEmbeddingStore.addAll(embeddings, textSegments);
    }

    @Override
    public ContentRetriever getMdlContentRetriever() {
        Integer maxResults = this.maxResults;
        Double minScore = this.minScore;
        if (SemanticModelRetrievalStrategy.HYQE == mdlRetrievalStrategy) {
            maxResults = this.mdlHyQEMaxResults;
            minScore = this.mdlHyQEMinScore;
        } else if (SemanticModelRetrievalStrategy.CE == mdlRetrievalStrategy) {
            maxResults = this.mdlCEMaxResults;
            minScore = this.mdlCEMinScore;
        }
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(mdlEmbeddingStore)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    @Override
    public List<SemanticModel> retrieveMdl(String question) {
        List<Content> contents = getMdlContentRetriever().retrieve(Query.from(question));
        return ContentStoreUtil.contents2SemanticModels(contents);
    }

    @Override
    public List<SemanticModel> allMdls() {
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed("N/A").content()) // 零向量
                .minScore(0.0) // 匹配所有记录
                .maxResults(Integer.MAX_VALUE) // 返回全部结果
                .build();
        List<TextSegment> textSegments = mdlEmbeddingStore.search(searchRequest)
                .matches()
                .stream()
                .map(EmbeddingMatch::embedded)
                .collect(Collectors.toList());
        return ContentStoreUtil.toSemanticModels(textSegments);
    }

    @Override
    public boolean isMdl(TextSegment textSegment) {
        return ContentType.MDL.toString()
                .equals(textSegment.metadata().getString(METADATA_CONTENT_TYPE));
    }

    @Override
    public void removeMdls(Collection<String> ids) {
        mdlEmbeddingStore.removeAll(ids);
    }

    @Override
    public void removeAllMdls() {
        mdlEmbeddingStore.removeAll();
    }

    @Override
    public List<String> addSqls(List<QuestionSqlPair> sqlPairs) {
        List<TextSegment> embedTextSegments = sqlPairs.stream()
                .map(QuestionSqlPair::getQuestion)
                .map(TextSegment::from)
                .toList();
        List<Embedding> embeddings = embeddingModel.embedAll(embedTextSegments).content();
        List<TextSegment> textSegments = sqlPairs.stream()
                .map(pair -> {
                    String json;
                    try {
                        json = JSON_MAPPER.writeValueAsString(pair);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to serialize question sql pair to JSON: "
                                + e.getMessage(), e);
                    }
                    return TextSegment.from(json, SQL_METADATA);
                }).collect(Collectors.toList());
        return sqlEmbeddingStore.addAll(embeddings, textSegments);
    }

    @Override
    public ContentRetriever getSqlContentRetriever() {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(sqlEmbeddingStore)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    @Override
    public List<QuestionSqlPair> retrieveSql(String question) {
        List<Content> contents = getSqlContentRetriever().retrieve(Query.from(question));
        return ContentStoreUtil.contents2QuestionSqlPairs(contents);
    }

    @Override
    public boolean isSql(TextSegment textSegment) {
        return ContentType.SQL.toString()
                .equals(textSegment.metadata().getString(METADATA_CONTENT_TYPE));
    }

    @Override
    public void removeSqls(Collection<String> ids) {
        sqlEmbeddingStore.removeAll(ids);
    }

    @Override
    public void removeAllSqls() {
        sqlEmbeddingStore.removeAll();
    }

    @Override
    public List<String> addSyns(List<WordSynonymPair> synonymPairs) {
        List<TextSegment> textSegments = synonymPairs.stream()
                .map(pair -> {
                    String json;
                    try {
                        json = JSON_MAPPER.writeValueAsString(pair);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to serialize noun synonyms pair to JSON: "
                                + e.getMessage(), e);
                    }
                    return TextSegment.from(json, SYN_METADATA);
                }).collect(Collectors.toList());
        List<Embedding> embeddings = embeddingModel.embedAll(textSegments).content();
        return synEmbeddingStore.addAll(embeddings, textSegments);
    }

    @Override
    public ContentRetriever getSynContentRetriever() {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(synEmbeddingStore)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    @Override
    public List<WordSynonymPair> retrieveSyn(String question) {
        List<Content> contents = getSynContentRetriever().retrieve(Query.from(question));
        return ContentStoreUtil.contents2NounSynonymPairs(contents);
    }

    @Override
    public boolean isSyn(TextSegment textSegment) {
        return ContentType.SYN.toString()
                .equals(textSegment.metadata().getString(METADATA_CONTENT_TYPE));
    }

    @Override
    public void removeSyns(Collection<String> ids) {
        synEmbeddingStore.removeAll(ids);
    }

    @Override
    public void removeAllSyns() {
        synEmbeddingStore.removeAll();
    }

    @Override
    public List<String> addDocs(List<String> docs) {
        if (BusinessKnowledgeIndexingMethod.GCE == docIndexingMethod) {
            return addDocsForGCE(docs);
        }
        return addDocsForFE(docs);
    }

    private List<String> addDocsForGCE(List<String> docs) {
        DocumentSplitter splitter = DocumentSplitters.recursive(docGCEMaxChunkSize, docGCEMaxChunkOverlap);
        if (docGCEChunkRegex != null) {
            splitter = new DocumentByRegexSplitter(docGCEChunkRegex, "\n",
                    docGCEMaxChunkSize, docGCEMaxChunkOverlap, splitter);
        }
        List<Document> documents = docs.stream().map(Document::document).collect(Collectors.toList());
        List<TextSegment> textSegments = splitter.splitAll(documents);
        List<Embedding> embeddings = embeddingModel.embedAll(textSegments).content();
        return docEmbeddingStore.addAll(embeddings, textSegments);
    }

    private List<String> addDocsForFE(List<String> docs) {
        List<TextSegment> textSegments = docs.stream()
                .map(doc -> TextSegment.from(doc, DOC_METADATA))
                .collect(Collectors.toList());
        List<Embedding> embeddings = embeddingModel.embedAll(textSegments).content();
        return docEmbeddingStore.addAll(embeddings, textSegments);
    }

    @Override
    public ContentRetriever getDocContentRetriever() {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(docEmbeddingStore)
                .maxResults(docMaxResults)
                .minScore(docMinScore)
                .build();
    }

    @Override
    public List<String> retrieveDoc(String question) {
        List<Content> contents = getDocContentRetriever().retrieve(Query.from(question));
        return ContentStoreUtil.contents2Docs(contents);
    }

    @Override
    public boolean isDoc(TextSegment textSegment) {
        return ContentType.DOC.toString()
                .equals(textSegment.metadata().getString(METADATA_CONTENT_TYPE));
    }

    @Override
    public void removeDocs(Collection<String> ids) {
        docEmbeddingStore.removeAll(ids);
    }

    @Override
    public void removeAllDocs() {
        docEmbeddingStore.removeAll();
    }

    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class SemanticModelColumnView {
        @NonNull
        private String name;

        @NonNull
        private String description;

        private String alias;

        @NonNull
        private List<String> tags;

        @NonNull
        private ElementView column;

        public SemanticModelColumnView(@NonNull SemanticModelView semanticModel,
                                       @NonNull ElementView columnElement) {
            this.name = semanticModel.getName();
            this.description = semanticModel.getDescription();
            this.alias = semanticModel.getAlias();
            this.tags = semanticModel.getTags();
            this.column = columnElement;
        }
    }
}
