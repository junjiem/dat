package ai.dat.core.utils;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.description.HtmlFormatter;
import ai.dat.core.configuration.time.TimeUtils;
import ai.dat.core.factories.Factory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.Getter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/8/15
 */
public class YamlTemplateUtil {

    private static final YAMLMapper YAML_MAPPER = YAMLMapper.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER) // 禁用 ---
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES) // 最小化引号
            .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE) // 多行字符串用 |
            .enable(YAMLGenerator.Feature.INDENT_ARRAYS) // 美化数组缩进
            .build();

    static {
        YAML_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL); // 忽略 null 字段
    }

    private YamlTemplateUtil() {
    }

    public static String getConfiguration(Set<ConfigOption<?>> requiredOptions,
                                          Set<ConfigOption<?>> optionalOptions) {
        return configurationTemplate(configTemplates(requiredOptions, optionalOptions));
    }

    public static String getConfiguration(Factory factory) {
        return configurationTemplate(configTemplates(factory));
    }

    private static String configurationTemplate(List<ConfigTemplate> configs) {
        StringBuilder sb = new StringBuilder();
        for (ConfigTemplate config : configs) {
            String description = config.getDescription();
            boolean multiLineDescription = description.contains("\n");
            if (multiLineDescription) {
                sb.append("## ------------------------------\n");
                for (String str : description.split("\n")) {
                    sb.append("## ").append(str).append("\n");
                }
                sb.append("## ------------------------------\n");
            }
            if (!config.isRequired()) {
                sb.append("#");
            }
            sb.append(config.getKey()).append(":");
            String value = config.getValue();
            if (value != null) {
                if (value.contains("\n")) {
                    sb.append("\n");
                    for (String str : value.split("\n")) {
                        if (!config.isRequired()) {
                            sb.append("#");
                        }
                        sb.append("  ").append(str).append("\n");
                    }
                } else {
                    sb.append(" ").append(value);
                }
            }
            if (!multiLineDescription) {
                sb.append("  # ").append(description);
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private static List<ConfigTemplate> configTemplates(Factory factory) {
        return configTemplates(factory.requiredOptions(), factory.optionalOptions());
    }

    private static List<ConfigTemplate> configTemplates(Set<ConfigOption<?>> requiredOptions,
                                                        Set<ConfigOption<?>> optionalOptions) {
        List<ConfigTemplate> configs = new ArrayList<>();
        if (requiredOptions != null) {
            configs.addAll(requiredOptions.stream()
                    .map(o -> configTemplate(true, o))
                    .toList());
        }
        if (optionalOptions != null) {
            configs.addAll(optionalOptions.stream()
                    .map(o -> configTemplate(false, o))
                    .toList());
        }
        return configs;
    }

    private static ConfigTemplate configTemplate(boolean required, ConfigOption<?> configOption) {
        return new ConfigTemplate(required, configOption.key(), toValue(configOption.defaultValue()),
                toDescription(required, configOption));
    }

    private static String toValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Duration val) {
            return TimeUtils.formatWithHighestUnit(val);
        } else {
            try {
                return YAML_MAPPER.writeValueAsString(value).trim();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String toDescription(boolean required, ConfigOption<?> configOption) {
        String description = new HtmlFormatter().format(configOption.description());
        String defaultValueDescription = "";
        if (configOption.hasDefaultValue()) {
            String defaultValue = toValue(configOption.defaultValue());
            if (defaultValue.contains("\n")) {
                defaultValue = "\n```\n" + defaultValue + "\n```";
            }
            defaultValueDescription = ", Default: " + defaultValue;
        }
        String classSimpleName = configOption.getClazz().getSimpleName();
        String prefix = "("
                + (configOption.isList() ? "List<" + classSimpleName + ">" : classSimpleName) + ", "
                + (required ? "[Required]" : "[Optional]")
                + defaultValueDescription
                + ")";
        if (description.contains("\n")) {
            return prefix + "\n\n" + description;
        }
        return prefix + " " + description;
    }

    private record ConfigTemplate(@Getter boolean required, @Getter String key, @Getter String value,
                                  @Getter String description) {
    }
}
