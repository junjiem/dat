package ai.dat.core.factories;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.agent.AskdataAgent;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.semantic.data.SemanticModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

import java.util.List;

/**
 * 问数Agent工厂接口类
 *
 * @Author JunjieM
 * @Date 2025/6/30
 */
public interface AskdataAgentFactory extends Factory {
    AskdataAgent create(ReadableConfig config,
                        List<SemanticModel> semanticModels,
                        ContentStore contentStore,
                        ChatModel chatModel,
                        StreamingChatModel streamingChatModel,
                        DatabaseAdapter databaseAdapter);
}