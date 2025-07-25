package ai.dat.embedder.inprocess;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.EmbeddingModelFactory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15q.BgeSmallZhV15QuantizedEmbeddingModel;

import java.util.Collections;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/7/1
 */
public class BgeSmallZhV15QuantizedEmbeddingModelFactory implements EmbeddingModelFactory {

    public static final String IDENTIFIER = "bge-small-zh-v15-q";

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public EmbeddingModel create(ReadableConfig config) {
        return new BgeSmallZhV15QuantizedEmbeddingModel();
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<ConfigOption<?>> fingerprintOptions() {
        return Collections.emptySet();
    }
}
