package ai.dat.core.semantic.view;

import ai.dat.core.adapter.SemanticAdapter;
import ai.dat.core.adapter.data.AnsiSqlType;
import ai.dat.core.semantic.data.Dimension;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DimensionView extends ElementView {

    @NonNull
    private Dimension.DimensionType type;

    @JsonProperty("enum_values")
    private List<Dimension.EnumValue> enumValues = List.of();

    @JsonProperty("type_params")
    private Dimension.TypeParams typeParams;

    public static DimensionView from(@NonNull SemanticAdapter semanticAdapter,
                                     @NonNull Dimension dimension) {
        DimensionView view = new DimensionView();
        view.setName(dimension.getName());
        view.setDescription(dimension.getDescription());
        view.setAlias(dimension.getAlias());
        view.setType(dimension.getType());
        view.setEnumValues(dimension.getEnumValues());
        view.setTypeParams(dimension.getTypeParams());
        AnsiSqlType ansiSqlType = null;
        if (dimension.getAnsiSqlType() != null) {
            ansiSqlType = dimension.getAnsiSqlType();
        } else if (dimension.getDataType() != null) {
            ansiSqlType = semanticAdapter.toAnsiSqlType(dimension.getDataType());
        }
        view.setDataType(ansiSqlType);
        return view;
    }
}