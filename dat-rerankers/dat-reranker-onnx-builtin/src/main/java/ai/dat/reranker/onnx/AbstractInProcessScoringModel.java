package ai.dat.reranker.onnx;

import ai.onnxruntime.OrtSession;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.model.scoring.ScoringModel;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

abstract class AbstractInProcessScoringModel implements ScoringModel {

    public AbstractInProcessScoringModel() {
    }

    static OnnxScoringBertCrossEncoder loadFromJar(String modelFileName, OrtSession.SessionOptions options,
                                                   String tokenizerFileName, int modelMaxLength, boolean normalize) {
        try {
            InputStream model = Thread.currentThread().getContextClassLoader().getResourceAsStream(modelFileName);
            InputStream tokenizer = Thread.currentThread().getContextClassLoader().getResourceAsStream(tokenizerFileName);
            return new OnnxScoringBertCrossEncoder(model, options, tokenizer, modelMaxLength, normalize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract OnnxScoringBertCrossEncoder model();

    public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
        OnnxScoringBertCrossEncoder.ScoringAndTokenCount scoresAndTokenCount = this.model().scoreAll(query,
                segments.stream().map(TextSegment::text).collect(Collectors.toList()));
        return Response.from(scoresAndTokenCount.scores, new TokenUsage(scoresAndTokenCount.tokenCount));
    }
}
