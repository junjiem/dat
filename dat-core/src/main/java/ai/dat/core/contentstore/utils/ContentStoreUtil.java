package ai.dat.core.contentstore.utils;

import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.contentstore.data.WordSynonymPair;
import ai.dat.core.semantic.data.SemanticModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @Author JunjieM
 * @Date 2025/8/11
 */
public class ContentStoreUtil {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private ContentStoreUtil() {
    }

    public static List<SemanticModel> toSemanticModels(List<TextSegment> textSegments) {
        return textSegments.stream()
                .map(textSegment -> {
                    try {
                        return JSON_MAPPER.readValue(textSegment.text(), SemanticModel.class);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                // 去重
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                SemanticModel::getName,
                                model -> model,
                                (o1, o2) -> o2
                        ),
                        map -> map.values().stream().toList()
                ));
    }

    public static List<QuestionSqlPair> toQuestionSqlPairs(List<TextSegment> textSegments) {
        return textSegments.stream()
                .map(textSegment -> {
                    try {
                        return JSON_MAPPER.readValue(textSegment.text(), QuestionSqlPair.class);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static List<WordSynonymPair> toNounSynonymPairs(List<TextSegment> textSegments) {
        return textSegments.stream()
                .map(textSegment -> {
                    try {
                        return JSON_MAPPER.readValue(textSegment.text(), WordSynonymPair.class);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static List<String> toDocs(List<TextSegment> textSegments) {
        return textSegments.stream()
                .map(TextSegment::text)
                .collect(Collectors.toList());
    }
}
