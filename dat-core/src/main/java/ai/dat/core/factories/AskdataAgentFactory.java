package ai.dat.core.factories;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.agent.AskdataAgent;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.factories.data.ChatModelInstance;
import ai.dat.core.semantic.data.SemanticModel;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 问数Agent工厂接口类
 *
 * @Author JunjieM
 * @Date 2025/6/30
 */
public interface AskdataAgentFactory extends Factory {
    String factoryDescription();

    AskdataAgent create(ReadableConfig config,
                        List<SemanticModel> semanticModels,
                        ContentStore contentStore,
                        List<ChatModelInstance> chatModelInstances,
                        DatabaseAdapter databaseAdapter,
                        Map<String, Object> variables);

    @Deprecated
    default AskdataAgent create(ReadableConfig config,
                                List<SemanticModel> semanticModels,
                                ContentStore contentStore,
                                List<ChatModelInstance> chatModelInstances,
                                DatabaseAdapter databaseAdapter) {
        return create(config, semanticModels, contentStore, chatModelInstances, databaseAdapter,
                Collections.emptyMap());
    }
}