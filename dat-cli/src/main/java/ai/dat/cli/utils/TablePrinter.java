package ai.dat.cli.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Help.Ansi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 表格打印工具类
 * 用于将查询数据以数据库CLI风格的表格格式显示
 *
 * @Author JunjieM
 * @Date 2025/7/29
 */
@Slf4j
public class TablePrinter {
    private TablePrinter() {
    }

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * 打印查询数据为表格格式
     *
     * @param data 查询数据对象
     */
    public static void printTable(Object data) {
        if (data == null) {
            System.out.println(AnsiUtil.string("@|fg(red) No data to display|@"));
            return;
        }
        try {
            JsonNode jsonNode = JSON_MAPPER.valueToTree(data);
            if (jsonNode.isArray()) {
                printArrayTable(jsonNode);
            } else if (jsonNode.isObject()) {
                printObjectTable(jsonNode);
            } else {
                printSimpleValue(data);
            }
        } catch (Exception e) {
            log.error("Failed to print table", e);
            printSimpleValue(data);
        }
    }

    /**
     * 打印数组数据为表格
     */
    private static void printArrayTable(JsonNode arrayNode) {
        if (arrayNode.isEmpty()) {
            System.out.println(AnsiUtil.string("@|fg(yellow) Empty result set|@"));
            return;
        }
        List<String> columns = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        // 获取所有列名
        for (JsonNode item : arrayNode) {
            if (item.isObject()) {
                item.fieldNames().forEachRemaining(fieldName -> {
                    if (!columns.contains(fieldName)) {
                        columns.add(fieldName);
                    }
                });
            }
        }
        // 如果没有列名，尝试从第一个元素推断
        if (columns.isEmpty() && !arrayNode.isEmpty()) {
            JsonNode firstItem = arrayNode.get(0);
            if (firstItem.isObject()) {
                firstItem.fieldNames().forEachRemaining(columns::add);
            }
        }
        // 构建行数据
        for (JsonNode item : arrayNode) {
            List<String> row = new ArrayList<>();
            for (String column : columns) {
                JsonNode value = item.get(column);
                row.add(formatValue(value));
            }
            rows.add(row);
        }
        printFormattedTable(columns, rows);
    }

    /**
     * 打印对象数据为表格
     */
    private static void printObjectTable(JsonNode objectNode) {
        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();
        objectNode.fieldNames().forEachRemaining(fieldName -> {
            columns.add(fieldName);
            values.add(formatValue(objectNode.get(fieldName)));
        });
        List<List<String>> rows = List.of(values);
        printFormattedTable(columns, rows);
    }

    /**
     * 打印简单值
     */
    private static void printSimpleValue(Object data) {
        System.out.println(AnsiUtil.string("@|fg(blue) " + data.toString() + "|@"));
    }

    /**
     * 格式化值
     */
    private static String formatValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return "NULL";
        }
        if (value.isTextual()) {
            return value.asText();
        }
        if (value.isNumber()) {
            return value.asText();
        }
        if (value.isBoolean()) {
            return value.asBoolean() ? "true" : "false";
        }
        if (value.isArray()) {
            return "[" + value.size() + " items]";
        }
        if (value.isObject()) {
            return "{object}";
        }
        return value.asText();
    }

    /**
     * 打印格式化的表格
     */
    private static void printFormattedTable(List<String> columns, List<List<String>> rows) {
        if (columns.isEmpty()) {
            System.out.println(AnsiUtil.string("@|fg(yellow) No columns to display|@"));
            return;
        }
        // 计算每列的最大宽度
        List<Integer> columnWidths = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            int maxWidth = columns.get(i).length();
            for (List<String> row : rows) {
                if (i < row.size()) {
                    maxWidth = Math.max(maxWidth, row.get(i).length());
                }
            }
            // 不限制最大宽度，显示完整内容
            columnWidths.add(maxWidth);
        }
        // 打印表头
        printTableSeparator(columnWidths);
        printTableRow(columns, columnWidths, true);
        printTableSeparator(columnWidths);
        // 打印数据行
        for (List<String> row : rows) {
            printTableRow(row, columnWidths, false);
        }
        // 打印表尾
        printTableSeparator(columnWidths);
        // 打印统计信息
        System.out.println(AnsiUtil.string("@|fg(green) " + rows.size() + " row(s) returned|@"));
    }

    /**
     * 打印表格行
     */
    private static void printTableRow(List<String> row, List<Integer> columnWidths, boolean isHeader) {
        StringBuilder line = new StringBuilder(AnsiUtil.string("@|fg(green) ||@"));
        for (int i = 0; i < columnWidths.size(); i++) {
            String cell = i < row.size() ? row.get(i) : "";
            String formattedCell = padRight(cell, columnWidths.get(i));
            if (isHeader) {
                line.append(AnsiUtil.string("@|bold,fg(cyan) " + formattedCell + "|@"));
            } else {
                line.append(AnsiUtil.string("@|fg(white) " + formattedCell + "|@"));
            }
            line.append(AnsiUtil.string("@|fg(green) ||@"));
        }
        System.out.println(line);
    }

    /**
     * 打印表格分隔线
     */
    private static void printTableSeparator(List<Integer> columnWidths) {
        StringBuilder separator = new StringBuilder("+");
        for (Integer width : columnWidths) {
            separator.append("-".repeat(width)).append("+");
        }
        System.out.println(AnsiUtil.string("@|fg(green) " + separator + "|@"));
    }

    /**
     * 右填充字符串
     */
    private static String padRight(String str, int length) {
        // 不截断字符串，显示完整内容
        return str + " ".repeat(Math.max(0, length - str.length()));
    }
} 