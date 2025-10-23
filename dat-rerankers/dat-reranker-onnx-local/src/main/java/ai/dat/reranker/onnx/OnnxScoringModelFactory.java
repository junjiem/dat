package ai.dat.reranker.onnx;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.ScoringModelFactory;
import ai.dat.core.utils.FactoryUtil;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.model.scoring.onnx.OnnxScoringModel;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/10/21
 */
public class OnnxScoringModelFactory implements ScoringModelFactory {

    public static final String IDENTIFIER = "onnx";

    public static final ConfigOption<String> MODEL_FILE_PATH =
            ConfigOptions.key("model-file-path")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("""
                            ONNX model file path, for example: /home/dat/model.onnx
                                                        
                            Local scoring (reranking) models, powered by [ONNX runtime](https://onnxruntime.ai/docs/get-started/with-java.html), running in the same Java process.
                            Many models (e.g., from [Hugging Face](https://huggingface.co)) can be used, as long as they are in the ONNX format.
                            Information on how to convert models into ONNX format can be found [here](https://huggingface.co/docs/optimum-onnx/onnx/usage_guides/export_a_model).
                            Many models already converted to ONNX format are available [here](https://huggingface.co/Xenova).
                            """);

    public static final ConfigOption<String> TOKENIZER_FILE_PATH =
            ConfigOptions.key("tokenizer-file-path")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Tokenizer file path, for example: /home/dat/tokenizer.json");

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return new LinkedHashSet<>(List.of(MODEL_FILE_PATH, TOKENIZER_FILE_PATH));
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return Collections.emptySet();
    }

    @Override
    public ScoringModel create(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);

        String modelFilePath = config.get(MODEL_FILE_PATH);
        String tokenizerFilePath = config.get(TOKENIZER_FILE_PATH);

        return new OnnxScoringModel(modelFilePath, tokenizerFilePath);
    }
}
