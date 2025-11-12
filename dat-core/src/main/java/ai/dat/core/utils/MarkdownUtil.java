package ai.dat.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class MarkdownUtil {

    /**
     * 将查询结果转换为Markdown表格格式
     *
     * @param data 数据列表
     * @return Markdown格式的表格文本
     */
    public static String toTable(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "";
        }
        StringBuilder markdown = new StringBuilder();
        // 获取列名（从第一行数据中获取所有的key）
        List<String> columns = new ArrayList<>(data.get(0).keySet());
        // 构建表头
        markdown.append("|");
        for (String column : columns) {
            markdown.append(" ").append(column).append(" |");
        }
        markdown.append("\n");
        // 构建分隔行
        markdown.append("|").append("---------|".repeat(columns.size())).append("\n");
        // 构建数据行
        for (Map<String, Object> row : data) {
            markdown.append("|");
            for (String column : columns) {
                Object value = row.get(column);
                String valueStr = value == null ? "" : value.toString();
                // 转义Markdown中的特殊字符
                valueStr = valueStr.replace("|", "\\|").replace("\n", " ");
                markdown.append(" ").append(valueStr).append(" |");
            }
            markdown.append("\n");
        }
        return markdown.toString();
    }

}
