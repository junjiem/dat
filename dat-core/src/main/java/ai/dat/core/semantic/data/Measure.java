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
public class Measure {
    @NonNull
    private String name;

    @NonNull
    private String description;

    private String alias;

    @NonNull
    private AggregationType agg = AggregationType.NONE;

    private String expr;

    @JsonProperty("non_additive_dimension")
    private NonAdditiveDimension nonAdditiveDimension;

    @JsonProperty("agg_time_dimension")
    private String aggTimeDimension;

    public void setAgg(String agg) {
        setAggregationType(AggregationType.fromValue(agg));
    }

    public void setAggregationType(AggregationType agg) {
        this.agg = agg;
    }

    public enum AggregationType {
        NONE("none"),
        SUM("sum"),
        MAX("max"),
        MIN("min"),
        AVG("avg"),
        COUNT("count"),
        MEDIAN("median"),
        COUNT_DISTINCT("count_distinct"),
        SUM_BOOLEAN("sum_boolean");

        private final String value;

        @JsonValue
        public String getValue() {
            return value;
        }

        private static final Map<String, AggregationType> VALUE_MAP = Arrays.stream(values())
                .collect(Collectors.toMap(AggregationType::getValue, Function.identity()));

        AggregationType(String value) {
            this.value = value;
        }

        public static AggregationType fromValue(String value) {
            return VALUE_MAP.get(value);
        }
    }

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NonAdditiveDimension {
        private String name;

        @JsonProperty("window_choice")
        private WindowChoiceType windowChoice;

        @JsonProperty("window_groupings")
        private List<String> windowGroupings;

        public void setWindowChoice(String windowChoice) {
            setWindowChoiceType(WindowChoiceType.fromValue(windowChoice));
        }

        public void setWindowChoiceType(WindowChoiceType windowChoice) {
            this.windowChoice = windowChoice;
        }

        public enum WindowChoiceType {
            min("min"),
            max("max");

            private final String value;

            @JsonValue
            public String getValue() {
                return value;
            }

            private static final Map<String, WindowChoiceType> VALUE_MAP = Arrays.stream(values())
                    .collect(Collectors.toMap(WindowChoiceType::getValue, Function.identity()));

            WindowChoiceType(String value) {
                this.value = value;
            }

            public static WindowChoiceType fromValue(String value) {
                return VALUE_MAP.get(value);
            }
        }
    }
}

