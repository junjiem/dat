package ai.dat.core.factories;

import ai.dat.core.configuration.ReadableConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

/**
 * 对话模型工厂接口类
 *
 * @Author JunjieM
 * @Date 2025/7/1
 */
public interface ChatModelFactory extends Factory {
    ChatModel create(ReadableConfig config);

    StreamingChatModel createStream(ReadableConfig config);
}
