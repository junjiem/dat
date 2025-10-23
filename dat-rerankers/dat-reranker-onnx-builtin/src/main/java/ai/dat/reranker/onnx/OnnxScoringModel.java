package ai.dat.reranker.onnx;

import ai.onnxruntime.OrtSession;

public class OnnxScoringModel extends AbstractInProcessScoringModel {

    private static final int DEFAULT_MODEL_MAX_LENGTH = 510; // 512 - 2 (special tokens [CLS] and [SEP])

    private static final boolean DEFAULT_NORMALIZE = false;

    private final OnnxScoringBertCrossEncoder onnxBertCrossEncoder;

    public OnnxScoringModel(String modelFileName, String tokenizerFileName) {
        this.onnxBertCrossEncoder = loadFromJar(modelFileName, new OrtSession.SessionOptions(),
                tokenizerFileName, DEFAULT_MODEL_MAX_LENGTH, DEFAULT_NORMALIZE);
    }

    public OnnxScoringModel(String modelFileName, OrtSession.SessionOptions options, String tokenizerFileName) {
        this.onnxBertCrossEncoder = loadFromJar(modelFileName, options, tokenizerFileName,
                DEFAULT_MODEL_MAX_LENGTH, DEFAULT_NORMALIZE);
    }

    public OnnxScoringModel(String modelFileName, String tokenizerFileName, int modelMaxLength) {
        this.onnxBertCrossEncoder = loadFromJar(modelFileName, new OrtSession.SessionOptions(),
                tokenizerFileName, modelMaxLength, DEFAULT_NORMALIZE);
    }

    public OnnxScoringModel(String modelFileName, OrtSession.SessionOptions options, String tokenizerFileName,
                            int modelMaxLength, boolean normalize) {
        this.onnxBertCrossEncoder = loadFromJar(modelFileName, options, tokenizerFileName, modelMaxLength, normalize);
    }

    protected OnnxScoringBertCrossEncoder model() {
        return this.onnxBertCrossEncoder;
    }
}
