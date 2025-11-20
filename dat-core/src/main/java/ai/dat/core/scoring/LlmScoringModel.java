package ai.dat.core.scoring;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import lombok.Builder;
import lombok.NonNull;

import java.util.List;

/**
 * Scoring Model Based LLM
 */
public class LlmScoringModel implements ScoringModel {

    private final Assistant assistant;

    @Builder
    public LlmScoringModel(@NonNull ChatModel chatModel) {
        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .build();
    }

    @Override
    public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
        List<Double> scores = segments.stream()
                .map(segment -> assistant.scoring(query, segment.text()))
                .map(score -> score != null ? score : 0)
                .map(Double::valueOf)
                .toList();
        return Response.from(scores);
    }

    private interface Assistant {
        @SystemMessage(fromResource = "prompts/default/scoring_system_prompt.txt")
        @UserMessage(fromResource = "prompts/default/scoring_user_prompt_template.txt")
        Integer scoring(@V("query") String query, @V("document") String document);
    }
}
