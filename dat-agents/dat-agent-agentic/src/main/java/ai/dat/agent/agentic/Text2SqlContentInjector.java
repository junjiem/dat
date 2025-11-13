package ai.dat.agent.agentic;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.contentstore.data.WordSynonymPair;
import ai.dat.core.contentstore.utils.ContentStoreUtil;
import ai.dat.core.semantic.data.SemanticModel;
import ai.dat.core.utils.JinjaTemplateUtil;
import ai.dat.core.utils.MarkdownUtil;
import ai.dat.core.utils.SemanticModelUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static ai.dat.core.contentstore.DefaultContentStore.METADATA_CONTENT_TYPE;

/**
 * @Author JunjieM
 * @Date 2025/8/11
 */
@Slf4j
class Text2SqlContentInjector implements ContentInjector {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String HISTORIES_CONTENT_TYPE = "histories";

    private static final PromptTemplate PROMPT_TEMPLATE;
    private static final PromptTemplate FOLLOWUP_PROMPT_TEMPLATE;

    private static final String TEXT_TO_SQL_RULES;

    static {
        PROMPT_TEMPLATE = PromptTemplate.from(loadText(
                "prompts/agentic/text_to_sql_user_prompt.txt"));
        FOLLOWUP_PROMPT_TEMPLATE = PromptTemplate.from(loadText(
                "prompts/agentic/text_to_sql_with_followup_user_prompt.txt"));
        TEXT_TO_SQL_RULES = loadText("prompts/agentic/text_to_sql_rules.txt");
    }

    private static String loadText(String fromResource) {
        try (InputStream inputStream = Text2SqlContentInjector.class.getClassLoader()
                .getResourceAsStream(fromResource)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load text from resources: " + fromResource, e);
        }
    }

    private final ContentStore contentStore;
    private final DatabaseAdapter databaseAdapter;
    private final List<SemanticModel> semanticModels;
    private final Map<String, Object> variables;
    private final String textToSqlRules;
    private final Integer semanticModelDataPreviewLimit;

    public Text2SqlContentInjector(@NonNull ContentStore contentStore,
                                   @NonNull DatabaseAdapter databaseAdapter,
                                   List<SemanticModel> semanticModels,
                                   Map<String, Object> variables,
                                   String textToSqlRules,
                                   Integer semanticModelDataPreviewLimit) {
        this.contentStore = contentStore;
        this.databaseAdapter = databaseAdapter;
        this.semanticModels = semanticModels;
        this.variables = Optional.ofNullable(variables).orElse(Collections.emptyMap());
        this.textToSqlRules = Optional.ofNullable(textToSqlRules).orElse(TEXT_TO_SQL_RULES);
        this.semanticModelDataPreviewLimit = Optional.ofNullable(semanticModelDataPreviewLimit).orElse(0);
    }

    @Override
    public ChatMessage inject(List<Content> contents, ChatMessage chatMessage) {
        if (contents.isEmpty()) {
            return chatMessage;
        } else {
            String queryTime = LocalDateTime.now().format(FORMATTER);

            List<SemanticModel> semanticModels = this.semanticModels;
            if (semanticModels == null || semanticModels.isEmpty()) {
                List<TextSegment> mdlTextSegments = contents.stream()
                        .map(Content::textSegment).filter(contentStore::isMdl).toList();
                semanticModels = ContentStoreUtil.toSemanticModels(mdlTextSegments);
                Preconditions.checkArgument(!semanticModels.isEmpty(), "Retrieve semantic models is empty");
            }
            List<String> semantics = semanticModels.stream()
                    .map(semanticModel -> SemanticModelUtil.toSemanticModelViewText(
                            semanticModel, databaseAdapter.semanticAdapter()))
                    .collect(Collectors.toList());
            List<TextSegment> sqlTextSegments = contents.stream()
                    .map(Content::textSegment).filter(contentStore::isSql).toList();
            List<QuestionSqlPair> sqlSamples = ContentStoreUtil.toQuestionSqlPairs(sqlTextSegments);
            List<TextSegment> synTextSegments = contents.stream()
                    .map(Content::textSegment).filter(contentStore::isSyn).toList();
            List<WordSynonymPair> synonyms = ContentStoreUtil.toNounSynonymPairs(synTextSegments);
            List<TextSegment> docTextSegments = contents.stream()
                    .map(Content::textSegment).filter(contentStore::isDoc).toList();
            List<String> docs = ContentStoreUtil.toDocs(docTextSegments);
            String query = ((dev.langchain4j.data.message.UserMessage) chatMessage).singleText();

            List<String> dataSamples = Collections.emptyList();
            if (semanticModelDataPreviewLimit > 0) {
                dataSamples = semanticModels.stream().map(m -> {
                            try {
                                SemanticModel semanticModel = JSON_MAPPER.readValue(
                                        JSON_MAPPER.writeValueAsString(m), SemanticModel.class);
                                semanticModel.setModel(JinjaTemplateUtil.render(semanticModel.getModel(), variables));
                                return semanticModel;
                            } catch (JsonProcessingException ex) {
                                throw new RuntimeException(ex);
                            }
                        }).map(m -> {
                            String semanticModelSql;
                            try {
                                semanticModelSql = SemanticModelUtil.semanticModelSql(databaseAdapter.semanticAdapter(), m);
                            } catch (SqlParseException e) {
                                log.warn("Skip data preview for semantic model {} due to parse error. SQL template: {}",
                                        m.getName(), m.getModel(), e);
                                return null;
                            }
                            String sql = "SELECT * FROM (" + semanticModelSql + ") AS __dat_semantic_model "
                                    + databaseAdapter.limitClause(semanticModelDataPreviewLimit);
                            List<Map<String, Object>> data;
                            try {
                                data = databaseAdapter.executeQuery(sql);
                            } catch (SQLException e) {
                                log.warn("Skip data preview for semantic model {} due to SQL error. SQL: {}",
                                        m.getName(), sql, e);
                                return null;
                            }
                            return "#### " + m.getName() + "\n\n" + MarkdownUtil.toTable(data);
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("text_to_sql_rules", textToSqlRules);
            variables.put("semantic_models", semantics);
            variables.put("data_samples", dataSamples);
            variables.put("sql_samples", sqlSamples);
            variables.put("synonyms", synonyms);
            variables.put("docs", docs);
            variables.put("query_time", queryTime);
            variables.put("query", query);

            List<QuestionSqlPair> histories = contents.stream()
                    .map(Content::textSegment)
                    .filter(textSegment -> HISTORIES_CONTENT_TYPE
                            .equals(textSegment.metadata().getString(METADATA_CONTENT_TYPE)))
                    .map(textSegment -> {
                        try {
                            return JSON_MAPPER.readValue(textSegment.text(), QuestionSqlPair.class);
                        } catch (JsonProcessingException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
            if (histories.isEmpty()) {
                return PROMPT_TEMPLATE.apply(variables).toUserMessage();
            } else {
                variables.put("histories", histories);
                return FOLLOWUP_PROMPT_TEMPLATE.apply(variables).toUserMessage();
            }
        }
    }
}
