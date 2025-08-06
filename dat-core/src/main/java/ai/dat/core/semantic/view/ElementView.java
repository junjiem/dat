package ai.dat.core.semantic.view;

import ai.dat.core.adapter.data.AnsiSqlType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * @Author JunjieM
 * @Date 2025/8/7
 */
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ElementView {
    @NonNull
    private String name;

    @NonNull
    private String description;

    private String alias;

    @JsonProperty("data_type")
    private AnsiSqlType dataType;
}
