package ai.dat.core.agent.data;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.description.Description;
import lombok.Builder;
import lombok.Getter;

import java.util.*;

/**
 * @Author JunjieM
 * @Date 2025/7/17
 */
@Getter
public class EventOption {

    private final String name;

    private final Description description;

    /**
     * 开启增量输出的选项。参数值：
     * <p>
     * null：每次输出为当前已经生成的整个序列，最后一次输出为生成的完整结果。
     * ```
     * I
     * I like
     * I like apple
     * I like apple.
     * ```
     * <p>
     * not null：增量输出，即后续输出内容不包含已输出的内容。您需要实时地逐个读取这些片段以获得完整的结果。
     * ```
     * I
     * like
     * apple
     * .
     * ```
     */
    private final Optional<ConfigOption<String>> incrementalOption;

    private final Optional<ConfigOption<String>> semanticSqlOption;

    private final Optional<ConfigOption<String>> querySqlOption;

    private final Optional<ConfigOption<List<Map<String, Object>>>> queryDataOption;

    /**
     * Human-in-the-loop AI request Option
     */
    private final Optional<ConfigOption<String>> hitlAiRequestOption;

    /**
     * Human-in-the-loop Tool Approval Option
     */
    private final Optional<ConfigOption<String>> hitlToolApprovalOption;

    private final Set<ConfigOption<?>> dataOptions;

    @Builder
    public EventOption(String name, Description description,
                       ConfigOption<String> incrementalOption,
                       ConfigOption<String> semanticSqlOption,
                       ConfigOption<String> querySqlOption,
                       ConfigOption<List<Map<String, Object>>> queryDataOption,
                       ConfigOption<String> hitlAiRequestOption,
                       ConfigOption<String> hitlToolApprovalOption,
                       Set<ConfigOption<?>> dataOptions) {
        this.name = Optional.ofNullable(name).orElse("message");
        this.description = Optional.ofNullable(description).orElse(Description.builder().text("").build());
        this.incrementalOption = Optional.ofNullable(incrementalOption);
        this.semanticSqlOption = Optional.ofNullable(semanticSqlOption);
        this.querySqlOption = Optional.ofNullable(querySqlOption);
        this.queryDataOption = Optional.ofNullable(queryDataOption);
        this.hitlAiRequestOption = Optional.ofNullable(hitlAiRequestOption);
        this.hitlToolApprovalOption = Optional.ofNullable(hitlToolApprovalOption);
        this.dataOptions = Optional.ofNullable(dataOptions).orElse(Collections.emptySet());
    }
}
