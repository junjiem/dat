package ai.dat.core.factories;

import ai.dat.core.configuration.ReadableConfig;
import dev.langchain4j.model.scoring.ScoringModel;

/**
 * Scoring模型工厂接口类
 *
 * @Author JunjieM
 * @Date 2025/10/21
 */
public interface ScoringModelFactory extends Factory {
    ScoringModel create(ReadableConfig config);
}
