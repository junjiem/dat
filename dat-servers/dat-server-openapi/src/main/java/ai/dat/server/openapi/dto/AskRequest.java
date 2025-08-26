package ai.dat.server.openapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ask data request")
public class AskRequest {

    @Schema(name = "conversation_id",
            description = "Conversation ID, to continue the conversation based on previous chat records, " +
                    "it is necessary to pass the previous message's conversation_id.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("conversation_id")
    private String conversationId;

    @Schema(name = "agent_name", description = "Agent name", defaultValue = "default",
            requiredMode = Schema.RequiredMode.AUTO)
    @JsonProperty("agent_name")
    private String agentName = "default";

    @NotBlank(message = "The question cannot be empty")
    @Schema(description = "User question", requiredMode = Schema.RequiredMode.REQUIRED)
    private String question;

    @Schema(description = "History Q&A SQL pair", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<QuestionSqlPair> histories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "History Q&A SQL pair")
    public static class QuestionSqlPair {
        @NotBlank(message = "The user question cannot be empty")
        @Schema(description = "User question")
        private String question;

        @NotBlank(message = "The SQL statement cannot be empty")
        @Schema(description = "SQL statement")
        private String sql;
    }
}