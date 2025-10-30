package ai.dat.core.semantic.data;

import ai.dat.core.adapter.data.AnsiSqlType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 *
 */
@Setter
@Getter
public abstract class Element {
    @NonNull
    private String name;

    @NonNull
    private String description;

    private String alias;

    private String expr;

    @JsonProperty("data_type")
    private String dataType;

    @JsonProperty("ansi_sql_type")
    private AnsiSqlType ansiSqlType;
}
