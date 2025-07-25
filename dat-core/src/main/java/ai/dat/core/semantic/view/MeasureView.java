package ai.dat.core.semantic.view;

import ai.dat.core.semantic.data.Measure;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MeasureView {
    @NonNull
    private String name;

    @NonNull
    private String description;

    private String alias;

    @NonNull
    private Measure.AggregationType agg;

    @JsonProperty("non_additive_dimension")
    private Measure.NonAdditiveDimension nonAdditiveDimension;

    @JsonProperty("agg_time_dimension")
    private String aggTimeDimension;

    public static MeasureView from(Measure measure) {
        MeasureView view = new MeasureView();
        view.setName(measure.getName());
        view.setDescription(measure.getDescription());
        view.setAlias(measure.getAlias());
        view.setAgg(measure.getAgg());
        view.setNonAdditiveDimension(measure.getNonAdditiveDimension());
        view.setAggTimeDimension(measure.getAggTimeDimension());
        return view;
    }
}