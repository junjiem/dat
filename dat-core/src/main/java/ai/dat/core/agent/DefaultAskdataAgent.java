package ai.dat.core.agent;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.agent.data.EventOption;
import ai.dat.core.agent.data.StreamAction;
import ai.dat.core.agent.data.StreamEvent;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.contentstore.data.WordSynonymPair;
import ai.dat.core.semantic.data.SemanticModel;
import ai.dat.core.utils.JinjaTemplateUtil;
import ai.dat.core.utils.MarkdownUtil;
import ai.dat.core.utils.SemanticModelUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static ai.dat.core.agent.DefaultEventOptions.*;

/**
 * @Author JunjieM
 * @Date 2025/6/25
 */
@Slf4j
public class DefaultAskdataAgent extends AbstractAskdataAgent {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final String TEXT_TO_SQL_RULES;

    static {
        TEXT_TO_SQL_RULES = loadText("prompts/default/text_to_sql_rules.txt");
    }

    private static String loadText(String fromResource) {
        try (InputStream inputStream = DefaultAskdataAgent.class.getClassLoader()
                .getResourceAsStream(fromResource)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load text from resources: " + fromResource, e);
        }
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<SemanticModel> semanticModels;

    private final String language;
    private final boolean intentClassification;
    private final boolean sqlGenerationReasoning;
    private final String textToSqlRules;
    private final Integer maxHistories;
    private final String instruction;
    private final Integer semanticModelDataPreviewLimit;

    private final Assistant streamingAssistant;

    private final Assistant intentClassificationAssistant;
    private final Assistant sqlGenerationReasoningAssistant;
    private final Assistant sqlGenerationAssistant;

    @Builder
    private DefaultAskdataAgent(@NonNull ContentStore contentStore,
                                @NonNull DatabaseAdapter databaseAdapter,
                                @NonNull ChatModel defaultModel,
                                @NonNull StreamingChatModel defaultStreamingModel,
                                List<SemanticModel> semanticModels,
                                Map<String, Object> variables,
                                String language,
                                Boolean intentClassification,
                                ChatModel intentClassificationModel,
                                Boolean sqlGenerationReasoning,
                                StreamingChatModel sqlGenerationReasoningModel,
                                ChatModel sqlGenerationModel,
                                String textToSqlRules,
                                Integer maxHistories,
                                String instruction,
                                Integer semanticModelDataPreviewLimit) {
        super(contentStore, databaseAdapter, variables);
        SemanticModelUtil.validateSemanticModels(semanticModels);
        this.semanticModels = semanticModels;
        this.language = Optional.ofNullable(language).orElse("English");
        this.intentClassification = Optional.ofNullable(intentClassification).orElse(true);
        this.sqlGenerationReasoning = Optional.ofNullable(sqlGenerationReasoning).orElse(true);
        this.textToSqlRules = Optional.ofNullable(textToSqlRules).orElse(TEXT_TO_SQL_RULES);
        this.maxHistories = Optional.ofNullable(maxHistories).orElse(20);
        Preconditions.checkArgument(this.maxHistories > 0,
                "maxHistories must be greater than 0");
        this.instruction = Optional.ofNullable(instruction).orElse("");
        this.semanticModelDataPreviewLimit = Optional.ofNullable(semanticModelDataPreviewLimit).orElse(0);
        Preconditions.checkArgument(this.semanticModelDataPreviewLimit >= 0 && this.semanticModelDataPreviewLimit <= 20,
                "semanticModelDataPreviewLimit must be between 0 and 20");

        this.streamingAssistant = AiServices.builder(Assistant.class)
                .streamingChatModel(defaultStreamingModel)
                .build();
        this.intentClassificationAssistant = AiServices.builder(Assistant.class)
                .chatModel(Objects.requireNonNullElse(intentClassificationModel, defaultModel))
                .build();
        this.sqlGenerationReasoningAssistant = AiServices.builder(Assistant.class)
                .streamingChatModel(Objects.requireNonNullElse(sqlGenerationReasoningModel, defaultStreamingModel))
                .build();
        this.sqlGenerationAssistant = AiServices.builder(Assistant.class)
                .chatModel(Objects.requireNonNullElse(sqlGenerationModel, defaultModel))
                .build();
    }

    @Override
    public Set<EventOption> eventOptions() {
        return Set.of(EXCEPTION_EVENT, INTENT_CLASSIFICATION_EVENT,
                MISLEADING_ASSISTANCE_EVENT, DATA_ASSISTANCE_EVENT,
                SQL_GENERATION_REASONING_EVENT, SQL_GENERATE_EVENT,
                SEMANTIC_TO_SQL_EVENT, SQL_EXECUTE_EVENT);
    }

    @Override
    protected void run(@NonNull String question, @NonNull List<QuestionSqlPair> histories) {
        String userQuestion = question;
        String questionTime = LocalDateTime.now().format(FORMATTER);

        histories = histories.subList(Math.max(0, histories.size() - maxHistories), histories.size());

        ContentStore contentStore = contentStore();

        List<SemanticModel> semanticModels = this.semanticModels;
        if (semanticModels == null || semanticModels.isEmpty()) {
            semanticModels = contentStore.retrieveMdl(question);
            Preconditions.checkArgument(!semanticModels.isEmpty(), "Retrieve semantic models is empty");
        }

        List<String> semantics = semanticModels.stream()
                .map(semanticModel -> SemanticModelUtil.toSemanticModelViewText(
                        semanticModel, databaseAdapter.semanticAdapter()))
                .collect(Collectors.toList());
        List<QuestionSqlPair> sqlSamples = contentStore.retrieveSql(question);
        List<WordSynonymPair> synonyms = contentStore.retrieveSyn(question);
        List<String> docs = contentStore.retrieveDoc(question);

        if (intentClassification) {
            IntentClassification intentClassification = intentClassification(semantics, sqlSamples,
                    synonyms, docs, histories, questionTime, question);

            StreamEvent event = StreamEvent.from(INTENT_CLASSIFICATION_EVENT)
                    .set(INTENT, intentClassification.intent);
            Optional.ofNullable(intentClassification.rephrased_question)
                    .ifPresent(o -> event.set(REPHRASED_QUESTION, o));
            Optional.ofNullable(intentClassification.reasoning)
                    .ifPresent(o -> event.set(REASONING, o));
            action.add(event);

            String rephrasedQuestion = intentClassification.rephrased_question;
            if (rephrasedQuestion != null && !rephrasedQuestion.isBlank()) {
                userQuestion = rephrasedQuestion; // 问题重写
            }

            Intent intent = intentClassification.intent;
            String userCompositeQuestion = null;
            if (Intent.MISLEADING_QUERY == intent || Intent.GENERAL == intent) {
                userCompositeQuestion = histories.stream()
                        .map(QuestionSqlPair::getQuestion)
                        .collect(Collectors.joining("\n"))
                        + "\n" + userQuestion;
            }

            if (Intent.MISLEADING_QUERY == intent) {
                TokenStream tokenStream = streamingAssistant.misleadingAssistance(
                        semantics, questionTime, userCompositeQuestion, language);
                CompletableFuture<Void> future = new CompletableFuture<>();
                tokenStream.onPartialResponse(s -> action.add(StreamEvent.from(MISLEADING_ASSISTANCE_EVENT, CONTENT, s)))
                        .onCompleteResponse(r -> future.complete(null))
                        .onError(e -> {
                            action.add(StreamEvent.from(MISLEADING_ASSISTANCE_EVENT, ERROR, e.getMessage()));
                            future.completeExceptionally(e);
                        })
                        .start();
                future.join();
                return;
            } else if (Intent.GENERAL == intent) {
                TokenStream tokenStream = streamingAssistant.dataAssistance(
                        semantics, questionTime, userCompositeQuestion, language);
                CompletableFuture<Void> future = new CompletableFuture<>();
                tokenStream.onPartialResponse(s -> action.add(StreamEvent.from(DATA_ASSISTANCE_EVENT, CONTENT, s)))
                        .onCompleteResponse(r -> future.complete(null))
                        .onError(e -> {
                            action.add(StreamEvent.from(DATA_ASSISTANCE_EVENT, ERROR, e.getMessage()));
                            future.completeExceptionally(e);
                        })
                        .start();
                future.join();
                return;
            }
        }

        List<SemanticModel> renderedSemanticModels = semanticModels.stream().map(m -> {
            try {
                SemanticModel semanticModel = JSON_MAPPER.readValue(
                        JSON_MAPPER.writeValueAsString(m), SemanticModel.class);
                semanticModel.setModel(JinjaTemplateUtil.render(semanticModel.getModel(), variables));
                return semanticModel;
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toList());

        List<String> dataSamples = Collections.emptyList();
        if (semanticModelDataPreviewLimit > 0) {
            dataSamples = renderedSemanticModels.stream().map(m -> {
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

        // 生成语义SQL
        String semanticSql = generateSql(action, semantics, dataSamples, sqlSamples, synonyms, docs,
                instruction, histories, questionTime, userQuestion);
        log.info("semanticSql: " + semanticSql);

        // 转换和执行
        try {
            executeQuery(semanticSql, renderedSemanticModels);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private IntentClassification intentClassification(List<String> semantics,
                                                      List<QuestionSqlPair> sqlSamples,
                                                      List<WordSynonymPair> synonyms,
                                                      List<String> docs,
                                                      List<QuestionSqlPair> histories,
                                                      String questionTime,
                                                      String question) {
        try {
            return intentClassificationAssistant.intentClassification(semantics, sqlSamples,
                    synonyms, docs, histories, questionTime, question, language);
        } catch (Exception e) {
            log.warn("Intent classification exception", e);
            return new IntentClassification(Intent.TEXT_TO_SQL);
        }
    }

    private String generateSql(StreamAction action,
                               List<String> semanticContexts,
                               List<String> dataSamples,
                               List<QuestionSqlPair> sqlSamples,
                               List<WordSynonymPair> synonyms,
                               List<String> docs,
                               String instruction,
                               List<QuestionSqlPair> histories,
                               String questionTime,
                               String question) {
        AtomicReference<String> sqlGenerateReasoning = new AtomicReference<>("");
        if (sqlGenerationReasoning) {
            TokenStream tokenStream;
            if (histories.isEmpty()) {
                tokenStream = sqlGenerationReasoningAssistant.sqlGenerateReasoning(
                        semanticContexts, dataSamples, sqlSamples, synonyms, docs, instruction,
                        questionTime, question, language);
            } else {
                tokenStream = sqlGenerationReasoningAssistant.followupSqlGenerateReasoning(
                        semanticContexts, dataSamples, sqlSamples, synonyms, docs, instruction,
                        histories, questionTime, question, language);
            }
            CompletableFuture<Void> future = new CompletableFuture<>();
            tokenStream.onPartialResponse(c -> {
                        action.add(StreamEvent.from(SQL_GENERATION_REASONING_EVENT, CONTENT, c));
                        sqlGenerateReasoning.updateAndGet(s -> s + c);
                    })
                    .onCompleteResponse(c -> future.complete(null))
                    .onError(e -> {
                        action.add(StreamEvent.from(SQL_GENERATION_REASONING_EVENT, ERROR, e.getMessage()));
                        sqlGenerateReasoning.set(""); // 异常则清空推理
                    })
                    .start();
            future.join();
        }

        GenSql genSql;
        if (histories.isEmpty()) {
            genSql = sqlGenerationAssistant.sqlGenerate(textToSqlRules, semanticContexts,
                    dataSamples, sqlSamples, synonyms, docs, instruction, questionTime, question, sqlGenerateReasoning.get());
        } else {
            genSql = sqlGenerationAssistant.followupSqlGenerate(textToSqlRules, semanticContexts,
                    dataSamples, sqlSamples, synonyms, docs, instruction, histories, questionTime, question,
                    sqlGenerateReasoning.get());
        }

        action.add(StreamEvent.from(SQL_GENERATE_EVENT, SQL, genSql.sql));

        return genSql.sql;
    }

    private static class GenSql {
        @Description("ANSI SQL query string")
        private String sql;
    }

    public enum Intent {
        MISLEADING_QUERY, // 误导性问题
        TEXT_TO_SQL, // 文本转SQL
        GENERAL // 一般性问题
        ;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class IntentClassification {
        @Description("rephrased question in full standalone question if there are previous questions, " +
                "otherwise the original question")
        @JsonProperty("rephrased_question")
        private String rephrased_question;

        @Description("brief chain-of-thought reasoning (max 20 words)")
        private String reasoning;

        @Description("\"MISLEADING_QUERY\" | \"TEXT_TO_SQL\" | \"GENERAL\"")
        private Intent intent;

        public IntentClassification(Intent intent) {
            this.intent = intent;
        }
    }

    private interface Assistant {
        @SystemMessage(fromResource = "prompts/default/intent_classification_system_prompt.txt")
        @UserMessage(fromResource = "prompts/default/intent_classification_user_prompt_template.txt")
        IntentClassification intentClassification(@V("semantic_models") List<String> semanticModels,
                                                  @V("sql_samples") List<QuestionSqlPair> sqlSamples,
                                                  @V("synonyms") List<WordSynonymPair> synonyms,
                                                  @V("docs") List<String> docs,
                                                  @V("histories") List<QuestionSqlPair> histories,
                                                  @V("query_time") String queryTime,
                                                  @V("query") String query,
                                                  @V("language") String language);

        @SystemMessage(fromResource = "prompts/default/misleading_assistance_system_prompt.txt")
        @UserMessage(fromResource = "prompts/default/misleading_assistance_user_prompt_template.txt")
        TokenStream misleadingAssistance(@V("semantic_models") List<String> semanticModels,
                                         @V("query_time") String queryTime,
                                         @V("query") String query,
                                         @V("language") String language);

        @SystemMessage(fromResource = "prompts/default/data_assistance_system_prompt.txt")
        @UserMessage(fromResource = "prompts/default/data_assistance_user_prompt_template.txt")
        TokenStream dataAssistance(@V("semantic_models") List<String> semanticModels,
                                   @V("query_time") String queryTime,
                                   @V("query") String query,
                                   @V("language") String language);

        @SystemMessage(fromResource = "prompts/default/sql_generation_reasoning_system_prompt.txt")
        @UserMessage(fromResource = "prompts/default/sql_generation_reasoning_user_prompt_template.txt")
        TokenStream sqlGenerateReasoning(@V("semantic_models") List<String> semanticModels,
                                         @V("data_samples") List<String> dataSamples,
                                         @V("sql_samples") List<QuestionSqlPair> sqlSamples,
                                         @V("synonyms") List<WordSynonymPair> synonyms,
                                         @V("docs") List<String> docs,
                                         @V("instruction") String instruction,
                                         @V("query_time") String queryTime,
                                         @V("query") String query,
                                         @V("language") String language);

        @SystemMessage(fromResource = "prompts/default/sql_generation_reasoning_system_prompt.txt")
        @UserMessage(fromResource = "prompts/default/sql_generation_reasoning_with_followup_user_prompt_template.txt")
        TokenStream followupSqlGenerateReasoning(@V("semantic_models") List<String> semanticModels,
                                                 @V("data_samples") List<String> dataSamples,
                                                 @V("sql_samples") List<QuestionSqlPair> sqlSamples,
                                                 @V("synonyms") List<WordSynonymPair> synonyms,
                                                 @V("docs") List<String> docs,
                                                 @V("instruction") String instruction,
                                                 @V("histories") List<QuestionSqlPair> histories,
                                                 @V("query_time") String queryTime,
                                                 @V("query") String query,
                                                 @V("language") String language);

        @SystemMessage(fromResource = "prompts/default/sql_generation_system_prompt.txt")
        @UserMessage(fromResource = "prompts/default/sql_generation_user_prompt_template.txt")
        GenSql sqlGenerate(@V("text_to_sql_rules") String textToSqlRules,
                           @V("semantic_models") List<String> semanticModels,
                           @V("data_samples") List<String> dataSamples,
                           @V("sql_samples") List<QuestionSqlPair> sqlSamples,
                           @V("synonyms") List<WordSynonymPair> synonyms,
                           @V("docs") List<String> docs,
                           @V("instruction") String instruction,
                           @V("query_time") String queryTime,
                           @V("query") String query,
                           @V("sql_generation_reasoning") String sqlGenerationReasoning);

        @SystemMessage(fromResource = "prompts/default/sql_generation_system_prompt.txt")
        @UserMessage(fromResource = "prompts/default/sql_generation_with_followup_user_prompt_template.txt")
        GenSql followupSqlGenerate(@V("text_to_sql_rules") String textToSqlRules,
                                   @V("semantic_models") List<String> semanticModels,
                                   @V("data_samples") List<String> dataSamples,
                                   @V("sql_samples") List<QuestionSqlPair> sqlSamples,
                                   @V("synonyms") List<WordSynonymPair> synonyms,
                                   @V("docs") List<String> docs,
                                   @V("instruction") String instruction,
                                   @V("histories") List<QuestionSqlPair> histories,
                                   @V("query_time") String queryTime,
                                   @V("query") String query,
                                   @V("sql_generation_reasoning") String sqlGenerationReasoning);
    }
}
