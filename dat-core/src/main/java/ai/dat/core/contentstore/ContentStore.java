package ai.dat.core.contentstore;

import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.contentstore.data.WordSynonymPair;
import ai.dat.core.semantic.data.SemanticModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;

import java.util.Collection;
import java.util.List;

/**
 * 内容存储接口类
 *
 * @Author JunjieM
 * @Date 2025/6/25
 */
public interface ContentStore {
    // ---------------语义模型-------------------

    default String addMdl(SemanticModel semanticModel) {
        return addMdls(List.of(semanticModel)).get(0);
    }

    List<String> addMdls(List<SemanticModel> semanticModels);

    ContentRetriever getMdlContentRetriever();

    default ContentAggregator getMdlContentAggregator() {
        return new DefaultContentAggregator();
    }

    List<SemanticModel> retrieveMdl(String question);

    List<SemanticModel> allMdls();

    boolean isMdl(TextSegment textSegment);

    default void removeMdl(String id) {
        removeMdls(List.of(id));
    }

    void removeMdls(Collection<String> ids);

    void removeAllMdls();

    // ---------------问题和SQL对-------------------

    default String addSql(QuestionSqlPair sqlPair) {
        return addSqls(List.of(sqlPair)).get(0);
    }

    List<String> addSqls(List<QuestionSqlPair> sqlPairs);

    ContentRetriever getSqlContentRetriever();

    default ContentAggregator getSqlContentAggregator() {
        return new DefaultContentAggregator();
    }

    List<QuestionSqlPair> retrieveSql(String question);

    boolean isSql(TextSegment textSegment);

    default void removeSql(String id) {
        removeSqls(List.of(id));
    }

    void removeSqls(Collection<String> ids);

    void removeAllSqls();

    // ---------------词和同义词对-------------------

    default String addSyn(WordSynonymPair synonymPair) {
        return addSyns(List.of(synonymPair)).get(0);
    }

    List<String> addSyns(List<WordSynonymPair> synonymPairs);

    ContentRetriever getSynContentRetriever();

    default ContentAggregator getSynContentAggregator() {
        return new DefaultContentAggregator();
    }

    List<WordSynonymPair> retrieveSyn(String question);

    boolean isSyn(TextSegment textSegment);

    default void removeSyn(String id) {
        removeSyns(List.of(id));
    }

    void removeSyns(Collection<String> ids);

    void removeAllSyns();

    // ---------------业务知识（术语或定义）-------------------

    default String addDoc(String doc) {
        return addDocs(List.of(doc)).get(0);
    }

    List<String> addDocs(List<String> docs);

    ContentRetriever getDocContentRetriever();

    default ContentAggregator getDocContentAggregator() {
        return new DefaultContentAggregator();
    }

    List<String> retrieveDoc(String question);

    boolean isDoc(TextSegment textSegment);

    default void removeDoc(String id) {
        removeDocs(List.of(id));
    }

    void removeDocs(Collection<String> ids);

    void removeAllDocs();

    // ---------------Remove All-------------------

    default void removeAll() {
        removeAllMdls();
        removeAllSqls();
        removeAllSyns();
        removeAllDocs();
    }
}
