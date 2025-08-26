package ai.dat.server.openapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ask data user response")
public class AskUserResponse {

    @Schema(name = "conversation_id",
            description = "Conversation ID, to continue the conversation based on previous chat records, " +
                    "it is necessary to pass the previous message's conversation_id.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("conversation_id")
    private String conversationId;

    @NotBlank(message = "The user response cannot be empty")
    @Schema(name = "user_response", description = "User response", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("user_response")
    private String userResponse;
}