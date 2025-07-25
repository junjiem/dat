package ai.dat.core.utils;

import ai.dat.core.semantic.data.SemanticModel;
import com.google.common.base.Preconditions;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author JunjieM
 * @Date 2025/7/15
 */
public class SemanticModelUtil {
    private SemanticModelUtil() {
    }

    public static void validateSemanticModels(List<SemanticModel> semanticModels) {
        if (semanticModels != null && !semanticModels.isEmpty()) {
            List<String> duplicates = semanticModels.stream()
                    .map(SemanticModel::getName)
                    .collect(Collectors.groupingBy(name -> name, Collectors.counting()))
                    .entrySet().stream()
                    .filter(entry -> entry.getValue() > 1)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            Preconditions.checkArgument(duplicates.isEmpty(),
                    String.format("There are duplicate semantic model names: %s",
                            String.join(", ", duplicates)));
            semanticModels.forEach(SemanticModelUtil::validateSemanticModel);
        }
    }

    public static void validateSemanticModel(@NonNull SemanticModel semanticModel) {
        String name = semanticModel.getName();
        String sql = semanticModel.getModel();
        Preconditions.checkArgument(sql.trim().toUpperCase().startsWith("SELECT"),
                String.format("The model of the semantic model '%s' must be a SELECT statement", name));
        Preconditions.checkArgument(!Pattern.compile(".*\\s*;\\s*$", Pattern.DOTALL)
                        .matcher(sql).matches(),
                String.format("The model of the semantic model '%s' is and can only be a SELECT statement " +
                        "(The end of an statement cannot contain ';')", name));
    }

}
