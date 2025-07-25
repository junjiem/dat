package ai.dat.core.adapter;

import ai.dat.core.exception.ValidationException;
import ai.dat.core.semantic.SemanticSqlConverter;
import ai.dat.core.semantic.data.SemanticModel;
import org.apache.calcite.sql.parser.SqlParseException;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author JunjieM
 * @Date 2025/7/2
 */
public abstract class GenericSqlDatabaseAdapter implements DatabaseAdapter {

    protected final SemanticAdapter semanticAdapter;
    protected final DataSource dataSource;

    public GenericSqlDatabaseAdapter(SemanticAdapter semanticAdapter, DataSource dataSource) {
        this.semanticAdapter = semanticAdapter;
        this.dataSource = dataSource;
    }

    @Override
    public String generateSql(String semanticSql, List<SemanticModel> semanticModels) {
        SemanticSqlConverter converter = new SemanticSqlConverter(semanticAdapter, semanticModels);
        try {
            return converter.convert(semanticSql);
        } catch (SqlParseException e) {
            throw new RuntimeException("Semantic SQL to dialect SQL failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> executeQuery(String sql) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            int columnCount = md.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = md.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    value = handleSpecificTypes(value, md.getColumnType(i));
                    row.put(columnName, value);
                }
                results.add(row);
            }
        }
        return results;
    }

    protected abstract Object handleSpecificTypes(Object value, int columnType);
}
