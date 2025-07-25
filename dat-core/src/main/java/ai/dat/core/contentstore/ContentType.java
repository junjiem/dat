package ai.dat.core.contentstore;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @Author JunjieM
 * @Date 2025/7/17
 */
public enum ContentType {
    MDL("mdl"), // 模型定义语言（语义模型）
    SQL("sql"), // 问题SQL对
    SYN("syn"), // 近义词对
    DOC("doc"); // 文档（业务知识）

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    ContentType(String value) {
        this.value = value;
    }
}
