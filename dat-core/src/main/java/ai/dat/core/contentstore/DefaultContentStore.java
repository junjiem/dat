package ai.dat.core.contentstore;

import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.contentstore.data.WordSynonymPair;
import ai.dat.core.contentstore.utils.ContentStoreUtil;
import ai.dat.core.semantic.data.SemanticModel;
import ai.dat.core.utils.SemanticModelUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.Builder;
import lombok.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private final ChatModel chatModel;

    private final EmbeddingModel embeddingModel;

    private final EmbeddingStore<TextSegment> mdlEmbeddingStore;
    private final EmbeddingStore<TextSegment> sqlEmbeddingStore;
    private final EmbeddingStore<TextSegment> synEmbeddingStore;
    private final EmbeddingStore<TextSegment> docEmbeddingStore;

    private final Integer maxResults;
    private final Double minScore;

    @Builder
    public DefaultContentStore(@NonNull ChatModel chatModel,
                               @NonNull EmbeddingModel embeddingModel,
                               @NonNull EmbeddingStore<TextSegment> mdlEmbeddingStore,
                               @NonNull EmbeddingStore<TextSegment> sqlEmbeddingStore,
                               @NonNull EmbeddingStore<TextSegment> synEmbeddingStore,
                               @NonNull EmbeddingStore<TextSegment> docEmbeddingStore,
                               Integer maxResults, Double minScore) {
        this.chatModel = chatModel;
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
    }

    @Override
    public List<String> addMdls(List<SemanticModel> semanticModels) {
        List<TextSegment> embedTextSegments = semanticModels.stream()
                .map(SemanticModelUtil::toSemanticModelViewText)
                .map(TextSegment::from)
                .toList();
        List<TextSegment> textSegments = semanticModels.stream()
                .map(model -> {
                    SemanticModelUtil.validateSemanticModel(model);
                    String json;
                    try {
                        json = JSON_MAPPER.writeValueAsString(model);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to serialize semantic model to JSON: "
                                + e.getMessage(), e);
                    }
                    return TextSegment.from(json, MDL_METADATA);
                }).collect(Collectors.toList());
        List<Embedding> embeddings = embeddingModel.embedAll(embedTextSegments).content();
        return mdlEmbeddingStore.addAll(embeddings, textSegments);
    }

    @Override
    public ContentRetriever getMdlContentRetriever() {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(mdlEmbeddingStore)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    @Override
    public List<SemanticModel> retrieveMdl(String question) {
        List<TextSegment> textSegments = getMdlContentRetriever()
                .retrieve(Query.from(question))
                .stream()
                .map(Content::textSegment)
                .collect(Collectors.toList());
        return ContentStoreUtil.toSemanticModels(textSegments);
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
        List<Embedding> embeddings = embeddingModel.embedAll(textSegments).content();
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
        List<TextSegment> textSegments = getSqlContentRetriever()
                .retrieve(Query.from(question))
                .stream()
                .map(Content::textSegment)
                .collect(Collectors.toList());
        return ContentStoreUtil.toQuestionSqlPairs(textSegments);
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
        List<TextSegment> textSegments = getSynContentRetriever()
                .retrieve(Query.from(question))
                .stream()
                .map(Content::textSegment)
                .collect(Collectors.toList());
        return ContentStoreUtil.toNounSynonymPairs(textSegments);
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
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    @Override
    public List<String> retrieveDoc(String question) {
        List<TextSegment> textSegments = getDocContentRetriever()
                .retrieve(Query.from(question))
                .stream()
                .map(Content::textSegment)
                .collect(Collectors.toList());
        return ContentStoreUtil.toDocs(textSegments);
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
}
