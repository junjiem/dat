package ai.dat.server.openapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ask data user approval")
public class AskUserApproval {

    @Schema(name = "conversation_id",
            description = "Conversation ID, to continue the conversation based on previous chat records, " +
                    "it is necessary to pass the previous message's conversation_id.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("conversation_id")
    private String conversationId;

    @Schema(name = "user_approval", description = "User approval", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("user_approval")
    private Boolean userApproval;
}