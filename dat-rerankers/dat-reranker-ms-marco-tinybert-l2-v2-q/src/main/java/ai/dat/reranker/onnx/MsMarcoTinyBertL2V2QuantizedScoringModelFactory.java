package ai.dat.reranker.onnx;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.ScoringModelFactory;
import dev.langchain4j.model.scoring.ScoringModel;

import java.util.Collections;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/10/21
 */
public class MsMarcoTinyBertL2V2QuantizedScoringModelFactory implements ScoringModelFactory {

    public static final String IDENTIFIER = "ms-marco-TinyBERT-L2-v2-q";

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
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
    public ScoringModel create(ReadableConfig config) {
        return new OnnxScoringModel(
                "ms-marco-TinyBERT-L2-v2-q.onnx",
                "ms-marco-TinyBERT-L2-v2-q-tokenizer.json"
        );
    }
}
