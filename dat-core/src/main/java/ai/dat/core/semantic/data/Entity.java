package ai.dat.core.semantic.data;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Setter
@Getter
public class Entity extends Element {

    @NonNull
    private EntityType type;

    public void setType(String type) {
        setEntityType(EntityType.fromValue(type));
    }

    public void setEntityType(EntityType type) {
        this.type = type;
    }

    @Getter
    public enum EntityType {
        PRIMARY("primary"),
        UNIQUE("unique"),
        FOREIGN("foreign");

        private final String value;

        @JsonValue
        public String getValue() {
            return value;
        }

        private static final Map<String, EntityType> VALUE_MAP = Arrays.stream(values())
                .collect(Collectors.toMap(EntityType::getValue, Function.identity()));

        EntityType(String value) {
            this.value = value;
        }

        public static EntityType fromValue(String value) {
            return VALUE_MAP.get(value);
        }

        @Override
        public String toString() {
            return value;
        }
    }
}

