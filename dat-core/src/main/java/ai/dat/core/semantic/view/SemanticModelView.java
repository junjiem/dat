package ai.dat.core.semantic.view;

import ai.dat.core.adapter.SemanticAdapter;
import ai.dat.core.semantic.data.SemanticModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SemanticModelView {
    @NonNull
    private String name;

    @NonNull
    private String description;

    private String alias;

    @NonNull
    private List<String> tags = List.of();

    @NonNull
    private SemanticModel.Defaults defaults;

    @NonNull
    private List<EntityView> entities = List.of();

    @NonNull
    private List<DimensionView> dimensions = List.of();

    @NonNull
    private List<MeasureView> measures = List.of();

    public static SemanticModelView from(@NonNull SemanticModel semanticModel) {
        return from(semanticModel, null);
    }

    public static SemanticModelView from(@NonNull SemanticModel semanticModel, SemanticAdapter semanticAdapter) {
        SemanticModelView view = new SemanticModelView();
        view.setName(semanticModel.getName());
        view.setDescription(semanticModel.getDescription());
        view.setAlias(semanticModel.getAlias());
        view.setTags(semanticModel.getTags());
        view.setDefaults(semanticModel.getDefaults());
        view.setEntities(semanticModel.getEntities().stream()
                .map(o -> EntityView.from(o, semanticAdapter))
                .collect(Collectors.toList()));
        view.setDimensions(semanticModel.getDimensions().stream()
                .map(o -> DimensionView.from(o, semanticAdapter))
                .collect(Collectors.toList()));
        view.setMeasures(semanticModel.getMeasures().stream()
                .map(o -> MeasureView.from(o, semanticAdapter))
                .collect(Collectors.toList()));
        return view;
    }
}