package ai.dat.core.utils;

import ai.dat.core.data.DatModel;
import com.google.common.base.Preconditions;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author JunjieM
 * @Date 2025/7/15
 */
public class DatModelUtil {
    private DatModelUtil() {
    }

    public static void validateModels(List<DatModel> models) {
        if (models == null || models.isEmpty()) {
            return;
        }
        List<String> duplicates = models.stream()
                .map(DatModel::getName)
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        Preconditions.checkArgument(duplicates.isEmpty(),
                String.format("There are duplicate model names: %s",
                        String.join(", ", duplicates)));
    }
}
