package ai.dat.core.adapter;

import ai.dat.core.adapter.data.AnsiSqlType;
import ai.dat.core.adapter.data.ColumnMetadata;
import ai.dat.core.adapter.data.Table;
import ai.dat.core.semantic.data.SemanticModel;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据库适配器接口类
 *
 * @Author JunjieM
 * @Date 2025/6/25
 */
public interface DatabaseAdapter {
    // -------------------------------------- semantic ------------------------------------------

    SemanticAdapter semanticAdapter();

    String generateSql(String semanticSql, List<SemanticModel> semanticModels);

    List<Map<String, Object>> executeQuery(String sql) throws SQLException;

    ResultSetMetaData getMetaData(String sql) throws SQLException;

    default List<ColumnMetadata> getColumnMetadata(String sql) throws SQLException {
        ResultSetMetaData metaData = getMetaData(sql);
        List<ColumnMetadata> columns = new ArrayList<>();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            String columnLabel = metaData.getColumnLabel(i);
            int columnType = metaData.getColumnType(i);
            String columnTypeName = metaData.getColumnTypeName(i);
            int precision = metaData.getPrecision(i);
            int scale = metaData.getScale(i);
            boolean nullable = metaData.isNullable(i) != ResultSetMetaData.columnNoNulls;
            boolean autoIncrement = metaData.isAutoIncrement(i);
            int displaySize = metaData.getColumnDisplaySize(i);
            AnsiSqlType ansiSqlType = toAnsiSqlType(columnType, columnTypeName, precision, scale);
            ColumnMetadata column = ColumnMetadata.builder()
                    .columnName(columnName)
                    .columnLabel(columnLabel)
                    .columnType(columnType)
                    .columnTypeName(columnTypeName)
                    .ansiSqlType(ansiSqlType)
                    .precision(precision)
                    .scale(scale)
                    .nullable(nullable)
                    .autoIncrement(autoIncrement)
                    .displaySize(displaySize)
                    .columnIndex(i)
                    .build();
            columns.add(column);
        }
        return columns;
    }

    default AnsiSqlType toAnsiSqlType(int columnType, String columnTypeName, int precision, int scale) {
        return AnsiSqlType.fromColumnType(columnType);
    }

    // -------------------------------------- seed ------------------------------------------

    void initTable(Table table, List<List<String>> data) throws SQLException;

}