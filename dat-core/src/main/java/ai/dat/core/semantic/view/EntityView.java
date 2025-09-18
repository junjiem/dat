package ai.dat.core.semantic.view;

import ai.dat.core.adapter.SemanticAdapter;
import ai.dat.core.adapter.data.AnsiSqlType;
import ai.dat.core.semantic.data.Entity;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityView extends ElementView {

    @NonNull
    private Entity.EntityType type;

    public static EntityView from(@NonNull Entity entity) {
        return from(entity, null);
    }

    public static EntityView from(@NonNull Entity entity, SemanticAdapter semanticAdapter) {
        EntityView view = new EntityView();
        view.setName(entity.getName());
        view.setDescription(entity.getDescription());
        view.setAlias(entity.getAlias());
        view.setType(entity.getType());
        AnsiSqlType ansiSqlType = null;
        if (entity.getAnsiSqlType() != null) {
            ansiSqlType = entity.getAnsiSqlType();
        } else if (entity.getDataType() != null && semanticAdapter != null) {
            ansiSqlType = semanticAdapter.toAnsiSqlType(entity.getDataType());
        }
        view.setDataType(ansiSqlType);
        return view;
    }
}