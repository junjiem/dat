package ai.dat.core.adapter;

import ai.dat.core.adapter.data.AnsiSqlType;
import ai.dat.core.adapter.data.ColumnMetadata;
import ai.dat.core.adapter.data.Table;
import ai.dat.core.semantic.data.SemanticModel;

import java.sql.SQLException;
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

    List<ColumnMetadata> getColumnMetadata(String sql) throws SQLException;

    default AnsiSqlType toAnsiSqlType(int columnType, String columnTypeName, int precision, int scale) {
        return AnsiSqlType.fromColumnType(columnType);
    }

    String limitClause(int limit);

    // -------------------------------------- seed ------------------------------------------

    void initTable(Table table, List<List<String>> data) throws SQLException;

}