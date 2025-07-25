package ai.dat.core.semantic.view;

import ai.dat.core.semantic.data.Entity;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityView {
    @NonNull
    private String name;

    @NonNull
    private String description;

    private String alias;

    @NonNull
    private Entity.EntityType type;

    public static EntityView from(Entity entity) {
        EntityView view = new EntityView();
        view.setName(entity.getName());
        view.setDescription(entity.getDescription());
        view.setAlias(entity.getAlias());
        view.setType(entity.getType());
        return view;
    }
}