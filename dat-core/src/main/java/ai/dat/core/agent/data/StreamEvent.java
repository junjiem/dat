package ai.dat.core.agent.data;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.Configuration;
import ai.dat.core.configuration.description.Description;
import com.google.common.base.Preconditions;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
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

    public long timestamp() {
        return timestamp;
    }

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

    public Optional<ToolExecutionRequest> getToolExecutionRequest() {
        ToolExecutionRequest toolExecutionRequest = null;
        ToolExecutionRequest.Builder builder = ToolExecutionRequest.builder();
        eventOption.getToolExecutionIdOption().flatMap(data::getOptional).ifPresent(builder::id);
        eventOption.getToolExecutionArgumentsOption().flatMap(data::getOptional).ifPresent(builder::arguments);
        Optional<String> nameOptional = eventOption.getToolExecutionNameOption().flatMap(data::getOptional);
        if (nameOptional.isPresent()) {
            toolExecutionRequest = builder.name(nameOptional.get()).build();
        }
        return Optional.ofNullable(toolExecutionRequest);
    }

    public Optional<String> getToolExecutionResult() {
        return eventOption.getToolExecutionResultOption().flatMap(data::getOptional);
    }

    public Optional<String> getHitlAiRequest() {
        return eventOption.getHitlAiRequestOption().flatMap(data::getOptional);
    }

    public Optional<String> getHitlToolApproval() {
        return eventOption.getHitlToolApprovalOption().flatMap(data::getOptional);
    }

    public Optional<Long> getHitlWaitTimeout() {
        return eventOption.getHitlWaitTimeoutOption().flatMap(data::getOptional);
    }

    public Map<String, Object> getMessages() {
        List<String> exclusions = new ArrayList<>();
        eventOption.getIncrementalOption().ifPresent(o -> exclusions.add(o.key()));
        eventOption.getSemanticSqlOption().ifPresent(o -> exclusions.add(o.key()));
        eventOption.getQuerySqlOption().ifPresent(o -> exclusions.add(o.key()));
        eventOption.getQueryDataOption().ifPresent(o -> exclusions.add(o.key()));
        eventOption.getToolExecutionIdOption().ifPresent(o -> exclusions.add(o.key()));
        eventOption.getToolExecutionNameOption().ifPresent(o -> exclusions.add(o.key()));
        eventOption.getToolExecutionArgumentsOption().ifPresent(o -> exclusions.add(o.key()));
        eventOption.getToolExecutionResultOption().ifPresent(o -> exclusions.add(o.key()));
        eventOption.getHitlAiRequestOption().ifPresent(o -> exclusions.add(o.key()));
        eventOption.getHitlToolApprovalOption().ifPresent(o -> exclusions.add(o.key()));
        eventOption.getHitlWaitTimeoutOption().ifPresent(o -> exclusions.add(o.key()));
        return eventOption.getDataOptions().stream()
                .filter(o -> !exclusions.contains(o.key()))
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
