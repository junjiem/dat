package ai.dat.core.factories.data;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.Getter;
import lombok.NonNull;

/**
 * @Author JunjieM
 * @Date 2025/8/8
 */
@Getter
public class ChatModelInstance {
    @NonNull
    private String name;

    @NonNull
    private ChatModel chatModel;

    private StreamingChatModel streamingChatModel;

    private ChatModelInstance(@NonNull String name,
                              @NonNull ChatModel chatModel,
                              StreamingChatModel streamingChatModel) {
        this.name = name;
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
    }

    public static ChatModelInstance from(@NonNull String name,
                                         @NonNull ChatModel chatModel,
                                         StreamingChatModel streamingChatModel) {
        return new ChatModelInstance(name, chatModel, streamingChatModel);
    }

    public static ChatModelInstance from(@NonNull String name,
                                         @NonNull ChatModel chatModel) {
        return new ChatModelInstance(name, chatModel, null);
    }
}
