package ai.dat.core.semantic.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Setter
@Getter
public class Dimension extends Element {

    @NonNull
    private DimensionType type = DimensionType.CATEGORICAL;

    @JsonProperty("enum_values")
    private List<EnumValue> enumValues = List.of();

    @JsonProperty("type_params")
    private TypeParams typeParams;

    public void setType(String type) {
        setDimensionType(DimensionType.fromValue(type));
    }

    public void setDimensionType(DimensionType type) {
        this.type = type;
    }

    public enum DimensionType {
        CATEGORICAL("categorical"),
        TIME("time");

        private final String value;

        @JsonValue
        public String getValue() {
            return value;
        }

        private static final Map<String, DimensionType> VALUE_MAP = Arrays.stream(values())
                .collect(Collectors.toMap(DimensionType::getValue, Function.identity()));

        DimensionType(String value) {
            this.value = value;
        }

        public static DimensionType fromValue(String value) {
            return VALUE_MAP.get(value);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TypeParams {
        @JsonProperty("time_granularity")
        private TimeGranularity timeGranularity;

        public void setTimeGranularity(String timeGranularity) {
            this.timeGranularity = TimeGranularity.fromValue(timeGranularity);
        }

        public enum TimeGranularity {
            SECOND("second"),
            MINUTE("minute"),
            HOUR("hour"),
            DAY("day"),
            WEEK("week"),
            MONTH("month"),
            QUARTER("quarter"),
            YEAR("year");

            private final String value;

            @JsonValue
            public String getValue() {
                return value;
            }

            private static final Map<String, TimeGranularity> VALUE_MAP = Arrays.stream(values())
                    .collect(Collectors.toMap(TimeGranularity::getValue, Function.identity()));

            TimeGranularity(String value) {
                this.value = value;
            }

            public static TimeGranularity fromValue(String value) {
                return VALUE_MAP.get(value);
            }

            @Override
            public String toString() {
                return value;
            }
        }
    }

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EnumValue {
        @NonNull
        private String value;

        private String label;
    }
}





