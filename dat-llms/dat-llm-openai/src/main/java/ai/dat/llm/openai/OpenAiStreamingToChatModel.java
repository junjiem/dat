package ai.dat.llm.openai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Streaming Chat Model Conversion Non-Streaming Chat Model
 */
public class OpenAiStreamingToChatModel implements ChatModel {

    private final StreamingChatModel streamingChatModel;

    public OpenAiStreamingToChatModel(StreamingChatModel streamingChatModel) {
        this.streamingChatModel = streamingChatModel;
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        ChatRequest finalChatRequest = ChatRequest.builder()
                .messages(chatRequest.messages())
                .parameters(streamingChatModel.defaultRequestParameters().overrideWith(chatRequest.parameters()))
                .build();

        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        streamingChatModel.chat(finalChatRequest, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
            }

            @Override
            public void onPartialThinking(PartialThinking partialThinking) {
            }

            @Override
            public void onPartialToolCall(PartialToolCall partialToolCall) {
            }

            @Override
            public void onCompleteToolCall(CompleteToolCall completeToolCall) {
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                futureResponse.complete(completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                futureResponse.completeExceptionally(error);
            }
        });

        try {
            return futureResponse.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
