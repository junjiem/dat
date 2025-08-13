package ai.dat.core.agent.data;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.Configuration;
import ai.dat.core.configuration.description.Description;
import com.google.common.base.Preconditions;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Author JunjieM
 * @Date 2025/6/25
 */
public class StreamEvent {

    private final EventOption eventOption;

    private final long timestamp = System.currentTimeMillis();

    @NonNull
    private final Configuration data = new Configuration();

    public String name() {
        return eventOption.getName();
    }

    public Description description() {
        return eventOption.getDescription();
    }

//    public EventOption eventOption() {
//        return eventOption;
//    }

    public long timestamp() {
        return timestamp;
    }

//    public <T> T get(ConfigOption<T> option) {
//        return data.get(option);
//    }
//
//    public <T> Optional<T> getOptional(ConfigOption<T> option) {
//        return data.getOptional(option);
//    }

//    public Map<String, Object> getAllMessages() {
//        return eventOption.getDataOptions().stream()
//                .collect(Collectors.toMap(o -> o, data::getOptional))
//                .entrySet().stream().filter(e -> e.getValue().isPresent())
//                .collect(Collectors.toMap(e -> e.getKey().key(), e -> e.getValue().get()));
//    }

    public Optional<String> getIncrementalContent() {
        return eventOption.getIncrementalOption().flatMap(data::getOptional);
    }

    public Optional<String> getSemanticSql() {
        return eventOption.getSemanticSqlOption().flatMap(data::getOptional);
    }

    public Optional<String> getQuerySql() {
        return eventOption.getQuerySqlOption().flatMap(data::getOptional);
    }

    public Optional<List<Map<String, Object>>> getQueryData() {
        return eventOption.getQueryDataOption().flatMap(data::getOptional);
    }

    public Optional<String> getHitlAiRequest() {
        return eventOption.getHitlAiRequestOption().flatMap(data::getOptional);
    }

    public Map<String, Object> getMessages() {
        List<String> keys = new ArrayList<>();
        eventOption.getIncrementalOption().ifPresent(o -> keys.add(o.key()));
        eventOption.getSemanticSqlOption().ifPresent(o -> keys.add(o.key()));
        eventOption.getQuerySqlOption().ifPresent(o -> keys.add(o.key()));
        eventOption.getQueryDataOption().ifPresent(o -> keys.add(o.key()));
        return eventOption.getDataOptions().stream()
                .filter(o -> !keys.contains(o.key()))
                .collect(Collectors.toMap(o -> o, data::getOptional))
                .entrySet().stream().filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(e -> e.getKey().key(), e -> e.getValue().get()));
    }

    private StreamEvent(@NonNull EventOption eventOption) {
        this.eventOption = eventOption;
    }

    public static StreamEvent from(@NonNull EventOption event) {
        return new StreamEvent(event);
    }

    public static <T> StreamEvent from(@NonNull EventOption event, @NonNull ConfigOption<T> option, T value) {
        Preconditions.checkArgument(event.getDataOptions().contains(option),
                "event option ");
        return from(event).set(option, value);
    }

    public <T> StreamEvent set(ConfigOption<T> option, T value) {
        Preconditions.checkArgument(eventOption.getDataOptions().contains(option),
                "There is no '" + option.key() + "' data option in the event option");
        this.data.set(option, value);
        return this;
    }
}
