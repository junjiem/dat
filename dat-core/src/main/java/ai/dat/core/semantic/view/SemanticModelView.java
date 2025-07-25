package ai.dat.core.semantic.view;

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
    private SemanticModel.Defaults defaults;

    @NonNull
    private List<EntityView> entities = List.of();

    @NonNull
    private List<DimensionView> dimensions = List.of();

    @NonNull
    private List<MeasureView> measures = List.of();

    public static SemanticModelView from(SemanticModel model) {
        SemanticModelView view = new SemanticModelView();
        view.setName(model.getName());
        view.setDescription(model.getDescription());
        view.setAlias(model.getAlias());
        view.setDefaults(model.getDefaults());
        view.setEntities(model.getEntities().stream()
                .map(EntityView::from)
                .collect(Collectors.toList()));
        view.setDimensions(model.getDimensions().stream()
                .map(DimensionView::from)
                .collect(Collectors.toList()));
        view.setMeasures(model.getMeasures().stream()
                .map(MeasureView::from)
                .collect(Collectors.toList()));
        return view;
    }
}