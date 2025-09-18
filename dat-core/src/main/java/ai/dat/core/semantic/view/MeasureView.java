package ai.dat.core.semantic.view;

import ai.dat.core.adapter.SemanticAdapter;
import ai.dat.core.adapter.data.AnsiSqlType;
import ai.dat.core.semantic.data.Measure;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MeasureView extends ElementView {

    @NonNull
    private Measure.AggregationType agg;

    @JsonProperty("non_additive_dimension")
    private Measure.NonAdditiveDimension nonAdditiveDimension;

    @JsonProperty("agg_time_dimension")
    private String aggTimeDimension;

    public static MeasureView from(@NonNull Measure measure) {
        return from(measure, null);
    }

    public static MeasureView from(@NonNull Measure measure, SemanticAdapter semanticAdapter) {
        MeasureView view = new MeasureView();
        view.setName(measure.getName());
        view.setDescription(measure.getDescription());
        view.setAlias(measure.getAlias());
        view.setAgg(measure.getAgg());
        view.setNonAdditiveDimension(measure.getNonAdditiveDimension());
        view.setAggTimeDimension(measure.getAggTimeDimension());
        AnsiSqlType ansiSqlType = null;
        if (measure.getAnsiSqlType() != null) {
            ansiSqlType = measure.getAnsiSqlType();
        } else if (measure.getDataType() != null && semanticAdapter != null) {
            ansiSqlType = semanticAdapter.toAnsiSqlType(measure.getDataType());
        }
        view.setDataType(ansiSqlType);
        return view;
    }
}