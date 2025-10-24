package ai.dat.embedder.onnx;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.EmbeddingModelFactory;
import ai.dat.core.utils.FactoryUtil;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/10/24
 */
public class OnnxEmbeddingModelFactory implements EmbeddingModelFactory {

    public static final String IDENTIFIER = "onnx";

    public static final ConfigOption<String> MODEL_FILE_PATH =
            ConfigOptions.key("model-file-path")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("""
                            ONNX model file path, for example: /home/dat/model.onnx
                                                        
                            Local embedding models, powered by [ONNX runtime](https://onnxruntime.ai/docs/get-started/with-java.html), running in the same Java process.
                            Many models (e.g., from [Hugging Face](https://huggingface.co)) can be used, as long as they are in the ONNX format.
                            Information on how to convert models into ONNX format can be found [here](https://huggingface.co/docs/optimum-onnx/onnx/usage_guides/export_a_model).
                            Many models already converted to ONNX format are available [here](https://huggingface.co/Xenova).
                            """);

    public static final ConfigOption<String> TOKENIZER_FILE_PATH =
            ConfigOptions.key("tokenizer-file-path")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Tokenizer file path, for example: /home/dat/tokenizer.json");

    public static final ConfigOption<PoolingMode> POOLING_MODE =
            ConfigOptions.key("pooling-mode")
                    .enumType(PoolingMode.class)
                    .defaultValue(PoolingMode.MEAN)
                    .withDescription("""
                            Pooling mode. Supported: `CLS`, `MEAN`.
                                                        
                            The pooling model to use. Can be found in the ".../1_Pooling/config.json" file on HuggingFace.
                            Here is an [example](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/blob/main/1_Pooling/config.json).
                            {"pooling_mode_mean_tokens": true} means that `MEAN` should be used.
                            """);

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return new LinkedHashSet<>(List.of(MODEL_FILE_PATH, TOKENIZER_FILE_PATH, POOLING_MODE));
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<ConfigOption<?>> fingerprintOptions() {
        return Set.of(MODEL_FILE_PATH, TOKENIZER_FILE_PATH);
    }

    @Override
    public EmbeddingModel create(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);

        String modelFilePath = config.get(MODEL_FILE_PATH);
        String tokenizerFilePath = config.get(TOKENIZER_FILE_PATH);
        PoolingMode poolingMode = config.get(POOLING_MODE);

        return new OnnxEmbeddingModel(modelFilePath, tokenizerFilePath, poolingMode);
    }
}
