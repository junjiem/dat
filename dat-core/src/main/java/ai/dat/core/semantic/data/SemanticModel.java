package ai.dat.core.semantic.data;

import ai.dat.core.semantic.view.SemanticModelView;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Setter
@Getter
public class SemanticModel {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @NonNull
    private String name;

    @NonNull
    private String description;

    private String alias;

    @NonNull
    private String model;

    @NonNull
    private List<String> tags = List.of();

    @NonNull
    private Defaults defaults = new Defaults();

    @NonNull
    private List<Entity> entities = List.of();

    @NonNull
    private List<Dimension> dimensions = List.of();

    @NonNull
    private List<Measure> measures = List.of();

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Defaults {
        @NonNull
        @JsonProperty("agg_time_dimension")
        private String aggTimeDimension = "";
    }

    public void setDefaults(Defaults defaults) {
        Defaults olds = this.defaults;
        this.defaults = defaults;
        try {
            validateDefaultsAggTimeDimension();
        } catch (Exception e) {
            this.defaults = olds;
            throw e;
        }
    }

    public void setEntities(@NonNull List<Entity> entities) {
        List<Entity> olds = this.entities;
        this.entities = entities;
        try {
            Preconditions.checkArgument(
                    entities.stream().filter(e -> Entity.EntityType.PRIMARY == e.getType()).count() <= 1,
                    "There can be at most one primary key in the entities");
            validateNameUnique();
        } catch (Exception e) {
            this.entities = olds;
            throw e;
        }
    }

    public void setDimensions(@NonNull List<Dimension> dimensions) {
        List<Dimension> olds = this.dimensions;
        this.dimensions = dimensions;
        try {
            validateNameUnique();
            validateDefaultsAggTimeDimension();
            validateEnumValues();
        } catch (Exception e) {
            this.dimensions = olds;
            throw e;
        }
    }

    public void setMeasures(@NonNull List<Measure> measures) {
        List<Measure> olds = this.measures;
        this.measures = measures;
        try {
            validateNameUnique();
        } catch (Exception e) {
            this.measures = olds;
            throw e;
        }
    }

    private void validateEnumValues() {
        List<String> incorrectNames = dimensions.stream()
                .filter(d -> Dimension.DimensionType.TIME == d.getType())
                .filter(d -> d.getEnumValues() != null && !d.getEnumValues().isEmpty())
                .map(d -> "'" + d.getName() + "'").toList();
        Preconditions.checkArgument(incorrectNames.isEmpty(),
                String.format("The time type %s cannot set enum values in the dimensions of %s",
                        String.join(", ", incorrectNames), theSemanticModelStr()));
    }

    private void validateDefaultsAggTimeDimension() {
        String aggTimeDimension = defaults.getAggTimeDimension();
        if (aggTimeDimension.isBlank()) {
            return;
        }
        Map<String, Dimension> dimensionMap = dimensions.stream().collect(Collectors.toMap(Dimension::getName, d -> d));
        Preconditions.checkArgument(dimensionMap.isEmpty() || (
                        dimensionMap.containsKey(aggTimeDimension)
                                && Dimension.DimensionType.TIME == dimensionMap.get(aggTimeDimension).getType()
                ),
                String.format("The '%s' of the agg_time_dimension of defaults does not exist or type is not time " +
                        "in the dimensions of %s", aggTimeDimension, theSemanticModelStr()));
    }

    private void validateNameUnique() {
        Set<String> names = new HashSet<>();
        for (Entity entity : entities) {
            String name = entity.getName();
            Preconditions.checkArgument(names.add(name),
                    String.format("There is duplicate name in %s: entity '%s'", theSemanticModelStr(), name));
        }
        for (Dimension dimension : dimensions) {
            String name = dimension.getName();
            Preconditions.checkArgument(names.add(name),
                    String.format("There is duplicate name in %s: dimension '%s'", theSemanticModelStr(), name));
        }
        for (Measure measure : measures) {
            String name = measure.getName();
            Preconditions.checkArgument(names.add(name),
                    String.format("There is duplicate name in %s: measure '%s'", theSemanticModelStr(), name));
        }
    }

    public SemanticModelView convertSemanticModelView() {
        return SemanticModelView.from(this);
    }

    @JsonInclude
    public String convertLlmSemanticModelContent() {
        try {
            return JSON_MAPPER.writeValueAsString(convertSemanticModelView());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize semantic model view to JSON: "
                    + e.getMessage(), e);
        }
    }

    private String theSemanticModelStr() {
        return String.format("the semantic model%s", StringUtils.isBlank(this.name) ? "" : " '" + this.name + "'");
    }
}