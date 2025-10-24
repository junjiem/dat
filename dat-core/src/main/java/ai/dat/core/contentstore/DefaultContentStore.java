package ai.dat.core.contentstore;

import ai.dat.core.contentstore.data.*;
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
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
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

import java.util.*;
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

    private final EmbeddingModel embeddingModel;

    private final EmbeddingStore<TextSegment> mdlEmbeddingStore;
    private final EmbeddingStore<TextSegment> sqlEmbeddingStore;
    private final EmbeddingStore<TextSegment> synEmbeddingStore;
    private final EmbeddingStore<TextSegment> docEmbeddingStore;

    private final ChatModel defaultChatModel;

    private final Integer maxResults;
    private final Double minScore;

    private final ScoringModel scoringModel;

    private final Boolean rerankMode;
    private final Integer rerankMaxResults;
    private final Double rerankMinScore;

    // -------------------------------------------- Semantic Model -------------------------------------------------
    private final SemanticModelIndexingMethod mdlIndexingMethod;

    private final MdlHyQEAssistant mdlHyQEAssistant;
    private final String mdlHyQEInstruction;
    private final Integer mdlHyQEQuestions;

    private final Integer mdlMaxResults;
    private final Double mdlMinScore;
    // -------------------------------------------------------------------------------------------------------------

    // -------------------------------------------- Business Knowledge ---------------------------------------------
    private final BusinessKnowledgeIndexingMethod docIndexingMethod;

    private final Integer docGCEMaxChunkSize;
    private final Integer docGCEMaxChunkOverlap;
    private final String docGCEChunkRegex;

    private final BusinessKnowledgeIndexingParentMode docPCCEParentMode;
    private final Integer docPCCEParentMaxChunkSize;
    private final String docPCCEParentChunkRegex;
    private final Integer docPCCEChildMaxChunkSize;
    private final String docPCCEChildChunkRegex;

    private final Integer docMaxResults;
    private final Double docMinScore;
    // -------------------------------------------------------------------------------------------------------------

    @Builder
    public DefaultContentStore(@NonNull EmbeddingModel embeddingModel,
                               @NonNull EmbeddingStore<TextSegment> mdlEmbeddingStore,
                               @NonNull EmbeddingStore<TextSegment> sqlEmbeddingStore,
                               @NonNull EmbeddingStore<TextSegment> synEmbeddingStore,
                               @NonNull EmbeddingStore<TextSegment> docEmbeddingStore,
                               @NonNull ChatModel defaultChatModel,
                               Integer maxResults, Double minScore,
                               ScoringModel scoringModel, Boolean rerankMode,
                               Integer rerankMaxResults, Double rerankMinScore,

                               SemanticModelIndexingMethod mdlIndexingMethod,
                               ChatModel mdlHyQEChatModel,
                               String mdlHyQEInstruction, Integer mdlHyQEQuestions,
                               Integer mdlMaxResults, Double mdlMinScore,

                               BusinessKnowledgeIndexingMethod docIndexingMethod,
                               Integer docGCEMaxChunkSize, Integer docGCEMaxChunkOverlap,
                               String docGCEChunkRegex,
                               BusinessKnowledgeIndexingParentMode docPCCEParentMode,
                               Integer docPCCEParentMaxChunkSize, String docPCCEParentChunkRegex,
                               Integer docPCCEChildMaxChunkSize, String docPCCEChildChunkRegex,
                               Integer docMaxResults, Double docMinScore) {
        this.defaultChatModel = defaultChatModel;
        this.embeddingModel = embeddingModel;
        this.mdlEmbeddingStore = mdlEmbeddingStore;
        this.sqlEmbeddingStore = sqlEmbeddingStore;
        this.synEmbeddingStore = synEmbeddingStore;
        this.docEmbeddingStore = docEmbeddingStore;
        this.maxResults = Optional.ofNullable(maxResults).orElse(5);
        Preconditions.checkArgument(this.maxResults <= 200 && this.maxResults >= 1,
                "maxResults must be between 1 and 200");
        this.minScore = Optional.ofNullable(minScore).orElse(0.6);
        Preconditions.checkArgument(this.minScore >= 0.0 && this.minScore <= 1.0,
                "minScore must be between 0.0 and 1.0");
        this.scoringModel = scoringModel;
        this.rerankMode = Optional.ofNullable(rerankMode).orElse(false);
        Preconditions.checkArgument(!this.rerankMode || this.scoringModel != null,
                "scoringModel cannot be null when rerankMode is true");
        this.rerankMaxResults = Optional.ofNullable(rerankMaxResults).orElse(this.maxResults);
        int rerankMaxResultsUpperLimit = Math.min(this.maxResults, 20);
        Preconditions.checkArgument(this.rerankMaxResults <= rerankMaxResultsUpperLimit
                        && this.rerankMaxResults >= 1,
                "rerankMaxResults must be between 1 and %s", rerankMaxResultsUpperLimit);
        this.rerankMinScore = rerankMinScore;

        // -------------------------------------------- Semantic Model ------------------------------------------
        this.mdlIndexingMethod = Optional.ofNullable(mdlIndexingMethod)
                .orElse(SemanticModelIndexingMethod.CE);
        this.mdlHyQEAssistant = AiServices.builder(MdlHyQEAssistant.class)
                .chatModel(Objects.requireNonNullElse(mdlHyQEChatModel, defaultChatModel))
                .build();
        this.mdlHyQEInstruction = Optional.ofNullable(mdlHyQEInstruction).orElse("");
        this.mdlHyQEQuestions = Optional.ofNullable(mdlHyQEQuestions).orElse(5);
        Preconditions.checkArgument(this.mdlHyQEQuestions <= 20 && this.mdlHyQEQuestions >= 3,
                "mdlHyQEQuestions must be between 3 and 20");
        this.mdlMaxResults = Optional.ofNullable(mdlMaxResults).orElse(this.maxResults);
        Preconditions.checkArgument(this.mdlMaxResults <= 200 && this.mdlMaxResults >= 1,
                "mdlMaxResults must be between 1 and 200");
        this.mdlMinScore = Optional.ofNullable(mdlMinScore).orElse(this.minScore);
        Preconditions.checkArgument(this.mdlMinScore >= 0.0 && this.mdlMinScore <= 1.0,
                "mdlMinScore must be between 0.0 and 1.0");
        // -----------------------------------------------------------------------------------------------------

        // -------------------------------------------- Business Knowledge -------------------------------------
        this.docIndexingMethod = Optional.ofNullable(docIndexingMethod)
                .orElse(BusinessKnowledgeIndexingMethod.PCCE);

        this.docGCEMaxChunkSize = Optional.ofNullable(docGCEMaxChunkSize).orElse(512);
        Preconditions.checkArgument(this.docGCEMaxChunkSize > 0,
                "docGCEMaxChunkSize must be greater than 0");
        this.docGCEMaxChunkOverlap = Optional.ofNullable(docGCEMaxChunkOverlap).orElse(0);
        Preconditions.checkArgument(this.docGCEMaxChunkOverlap >= 0,
                "docGCEMaxChunkOverlap must be greater than than or equal to 0");
        Preconditions.checkArgument(this.docGCEMaxChunkSize > this.docGCEMaxChunkOverlap,
                "docGCEMaxChunkOverlap value must be less than docGCEMaxChunkSize value");
        this.docGCEChunkRegex = docGCEChunkRegex;

        this.docPCCEParentMode = Optional.ofNullable(docPCCEParentMode)
                .orElse(BusinessKnowledgeIndexingParentMode.FULLTEXT);
        this.docPCCEParentMaxChunkSize = Optional.ofNullable(docPCCEParentMaxChunkSize).orElse(1024);
        Preconditions.checkArgument(this.docPCCEParentMaxChunkSize > 0,
                "docPCCEParentMaxChunkSize must be greater than 0");
        this.docPCCEChildMaxChunkSize = Optional.ofNullable(docPCCEChildMaxChunkSize).orElse(512);
        Preconditions.checkArgument(this.docPCCEChildMaxChunkSize > 0,
                "docPCCEChildMaxChunkSize must be greater than 0");
        Preconditions.checkArgument(this.docPCCEParentMaxChunkSize > this.docPCCEChildMaxChunkSize,
                "docPCCEChildMaxChunkSize value must be less than docPCCEParentMaxChunkSize value");
        this.docPCCEParentChunkRegex = docPCCEParentChunkRegex;
        this.docPCCEChildChunkRegex = docPCCEChildChunkRegex;

        this.docMaxResults = Optional.ofNullable(docMaxResults).orElse(this.maxResults);
        Preconditions.checkArgument(this.docMaxResults <= 200 && this.docMaxResults >= 1,
                "docMaxResults must be between 1 and 200");
        this.docMinScore = Optional.ofNullable(docMinScore).orElse(this.minScore);
        Preconditions.checkArgument(this.docMinScore >= 0.0 && this.docMinScore <= 1.0,
                "docMinScore must be between 0.0 and 1.0");
        // -----------------------------------------------------------------------------------------------------
    }

    @Override
    public List<String> addMdls(List<SemanticModel> semanticModels) {
        if (SemanticModelIndexingMethod.HYQE == mdlIndexingMethod) {
            return addMdlsForHyQE(semanticModels);
        } else if (SemanticModelIndexingMethod.CE == mdlIndexingMethod) {
            return addMdlsForCE(semanticModels);
        }
        return addMdlsForFE(semanticModels);
    }

    private List<String> addMdlsForHyQE(List<SemanticModel> semanticModels) {
        return semanticModels.stream().flatMap(semanticModel -> {
                    SemanticModelUtil.validateSemanticModel(semanticModel);
                    String semanticModelViewText = SemanticModelUtil.toSemanticModelViewText(semanticModel);
                    List<String> questions = mdlHyQEAssistant.genHypotheticalQuestions(
                            mdlHyQEInstruction, mdlHyQEQuestions, semanticModelViewText);
                    if (questions == null || questions.isEmpty()) {
                        return Stream.empty();
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
                    List<TextSegment> textSegments = embeddings.stream().map(o -> textSegment).collect(Collectors.toList());
                    return mdlEmbeddingStore.addAll(embeddings, textSegments).stream();
                })
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
        return semanticModels.stream().flatMap(semanticModel -> {
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
                    List<TextSegment> textSegments = embeddings.stream().map(o -> textSegment).collect(Collectors.toList());
                    return mdlEmbeddingStore.addAll(embeddings, textSegments).stream();
                })
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
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(mdlEmbeddingStore)
                .maxResults(mdlMaxResults)
                .minScore(mdlMinScore)
                .build();
    }

    @Override
    public ContentAggregator getMdlContentAggregator() {
        if (scoringModel == null) {
            return new DefaultContentAggregator();
        }
        ReRankingContentAggregator.ReRankingContentAggregatorBuilder builder =
                ReRankingContentAggregator.builder()
                        .scoringModel(scoringModel)
                        .maxResults(rerankMaxResults);
        Optional.ofNullable(rerankMinScore).ifPresent(builder::minScore);
        return builder.build();
    }

    @Override
    public List<SemanticModel> retrieveMdl(String question) {
        Query query = Query.from(question);
        List<Content> contents = getMdlContentRetriever().retrieve(query);
        if (rerankMode && !contents.isEmpty()) {
            contents = getMdlContentAggregator().aggregate(
                    Collections.singletonMap(query, Collections.singletonList(contents)));
        }
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
    public ContentAggregator getSqlContentAggregator() {
        if (scoringModel == null) {
            return new DefaultContentAggregator();
        }
        ReRankingContentAggregator.ReRankingContentAggregatorBuilder builder =
                ReRankingContentAggregator.builder()
                        .scoringModel(scoringModel)
                        .maxResults(rerankMaxResults);
        Optional.ofNullable(rerankMinScore).ifPresent(builder::minScore);
        return builder.build();
    }

    @Override
    public List<QuestionSqlPair> retrieveSql(String question) {
        Query query = Query.from(question);
        List<Content> contents = getSqlContentRetriever().retrieve(query);
        if (rerankMode && !contents.isEmpty()) {
            contents = getSqlContentAggregator().aggregate(
                    Collections.singletonMap(query, Collections.singletonList(contents)));
        }
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
    public ContentAggregator getSynContentAggregator() {
        if (scoringModel == null) {
            return new DefaultContentAggregator();
        }
        ReRankingContentAggregator.ReRankingContentAggregatorBuilder builder =
                ReRankingContentAggregator.builder()
                        .scoringModel(scoringModel)
                        .maxResults(rerankMaxResults);
        Optional.ofNullable(rerankMinScore).ifPresent(builder::minScore);
        return builder.build();
    }

    @Override
    public List<WordSynonymPair> retrieveSyn(String question) {
        Query query = Query.from(question);
        List<Content> contents = getSynContentRetriever().retrieve(query);
        if (rerankMode && !contents.isEmpty()) {
            contents = getSynContentAggregator().aggregate(
                    Collections.singletonMap(query, Collections.singletonList(contents)));
        }
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
        } else if (BusinessKnowledgeIndexingMethod.PCCE == docIndexingMethod) {
            return addDocsForPCCE(docs);
        }
        return addDocsForFE(docs);
    }

    private List<String> addDocsForPCCE(List<String> docs) {
        DocumentSplitter parentSplitter = null;
        if (BusinessKnowledgeIndexingParentMode.PARAGRAPH == docPCCEParentMode) {
            parentSplitter = DocumentSplitters.recursive(docPCCEParentMaxChunkSize, 0);
            if (docPCCEParentChunkRegex != null) {
                parentSplitter = new DocumentByRegexSplitter(docPCCEParentChunkRegex, "\n\n",
                        docPCCEParentMaxChunkSize, 0, parentSplitter);
            }
        }
        DocumentSplitter childSplitter = DocumentSplitters.recursive(docPCCEChildMaxChunkSize, 0);
        if (docPCCEChildChunkRegex != null) {
            childSplitter = new DocumentByRegexSplitter(docPCCEChildChunkRegex, "\n",
                    docPCCEChildMaxChunkSize, 0, childSplitter);
        }
        DocumentSplitter finalParentSplitter = parentSplitter;
        DocumentSplitter finalChildSplitter = childSplitter;
        return docs.stream().flatMap(text -> {
            List<String> parentTexts = BusinessKnowledgeIndexingParentMode.PARAGRAPH == docPCCEParentMode ?
                    finalParentSplitter.split(Document.document(text)).stream().map(TextSegment::text).toList() :
                    Collections.singletonList(text);
            return parentTexts.stream().flatMap(parentText -> {
                TextSegment textSegment = TextSegment.from(parentText, DOC_METADATA);
                List<TextSegment> embedTextSegments = finalChildSplitter.split(Document.document(parentText));
                List<Embedding> embeddings = embeddingModel.embedAll(embedTextSegments).content();
                List<TextSegment> textSegments = embeddings.stream().map(o -> textSegment).collect(Collectors.toList());
                return docEmbeddingStore.addAll(embeddings, textSegments).stream();
            });
        }).collect(Collectors.toList());
    }

    private List<String> addDocsForGCE(List<String> docs) {
        DocumentSplitter splitter = DocumentSplitters.recursive(docGCEMaxChunkSize, docGCEMaxChunkOverlap);
        if (docGCEChunkRegex != null) {
            splitter = new DocumentByRegexSplitter(docGCEChunkRegex, "\n",
                    docGCEMaxChunkSize, docGCEMaxChunkOverlap, splitter);
        }
        List<Document> documents = docs.stream().map(doc -> Document.document(doc, DOC_METADATA)).collect(Collectors.toList());
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
    public ContentAggregator getDocContentAggregator() {
        if (scoringModel == null) {
            return new DefaultContentAggregator();
        }
        ReRankingContentAggregator.ReRankingContentAggregatorBuilder builder =
                ReRankingContentAggregator.builder()
                        .scoringModel(scoringModel)
                        .maxResults(rerankMaxResults);
        Optional.ofNullable(rerankMinScore).ifPresent(builder::minScore);
        return builder.build();
    }

    @Override
    public List<String> retrieveDoc(String question) {
        Query query = Query.from(question);
        List<Content> contents = getDocContentRetriever().retrieve(query);
        if (rerankMode && !contents.isEmpty()) {
            contents = getDocContentAggregator().aggregate(
                    Collections.singletonMap(query, Collections.singletonList(contents)));
        }
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
