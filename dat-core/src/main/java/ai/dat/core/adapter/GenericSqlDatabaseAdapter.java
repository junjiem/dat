package ai.dat.core.adapter;

import ai.dat.core.adapter.data.AnsiSqlType;
import ai.dat.core.adapter.data.Column;
import ai.dat.core.adapter.data.ColumnMetadata;
import ai.dat.core.adapter.data.Table;
import ai.dat.core.semantic.SemanticSqlConverter;
import ai.dat.core.semantic.data.SemanticModel;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author JunjieM
 * @Date 2025/7/2
 */
@Slf4j
public abstract class GenericSqlDatabaseAdapter implements DatabaseAdapter {

    protected final SemanticAdapter semanticAdapter;
    protected final DataSource dataSource;

    public GenericSqlDatabaseAdapter(SemanticAdapter semanticAdapter, DataSource dataSource) {
        this.semanticAdapter = semanticAdapter;
        this.dataSource = dataSource;
    }

    // -------------------------------------- semantic ------------------------------------------

    @Override
    public SemanticAdapter semanticAdapter() {
        return semanticAdapter;
    }

    @Override
    public String generateSql(@NonNull String semanticSql, @NonNull List<SemanticModel> semanticModels) {
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

    @Override
    public List<ColumnMetadata> getColumnMetadata(String sql) throws SQLException {
        List<ColumnMetadata> columns = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            ResultSetMetaData metaData = rs.getMetaData();
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
        }
        return columns;
    }

    // -------------------------------------- seed ------------------------------------------

    @Override
    public void initTable(Table table, List<List<String>> data) throws SQLException {
        String tableName = table.getName();
        try (Connection conn = dataSource.getConnection()) {
            dropTableIfExists(conn, tableName);
            createTable(conn, table);
            insertTable(conn, table, data);
        }
    }

    protected void dropTableIfExists(Connection conn, String tableName) {
        String sql = getDropTableSqlIfExists(tableName);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            // 表不存在是正常情况，忽略异常
        }
    }

    protected void createTable(Connection conn, Table table) throws SQLException {
        String sql = getCreateTableSql(table);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    protected String getDropTableSqlIfExists(String tableName) {
        return String.format("DROP TABLE IF EXISTS %s", quoteIdentifier(tableName));
    }

    protected String getCreateTableSql(Table table) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(quoteIdentifier(table.getName())).append(" (");
        List<Column> columns = table.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            Column column = columns.get(i);
            sql.append(quoteIdentifier(column.getName())).append(" ")
                    .append(getDataType(column.getType()));
        }
        sql.append(")");
        return sql.toString();
    }

    private String getDataType(String dataType) {
        return StringUtils.isBlank(dataType) ? stringDataType() : dataType;
    }

    protected String stringDataType() {
        return "TEXT";
    }

    protected void insertTable(Connection conn, Table table, List<List<String>> data) throws SQLException {
        if (data.isEmpty()) {
            return;
        }
        String sql = getInsertSql(table);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int batchCount = 0;
            for (List<String> row : data) {
                setInsertParameters(stmt, row, table.getColumns());
                stmt.addBatch();
                batchCount++;
                // 每1000条提交一次批处理
                if (batchCount % 1000 == 0) {
                    stmt.executeBatch();
                    stmt.clearBatch();
                }
            }
            // 提交剩余的批处理
            if (batchCount % 1000 != 0) {
                stmt.executeBatch();
            }
        }
    }

    protected String getInsertSql(Table table) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(quoteIdentifier(table.getName())).append(" (");
        List<Column> columns = table.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(quoteIdentifier(columns.get(i).getName()));
        }
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");
        return sql.toString();
    }

    protected void setInsertParameters(PreparedStatement stmt, List<String> row, List<Column> columns) throws SQLException {
        for (int i = 0; i < columns.size() && i < row.size(); i++) {
            String value = row.get(i);
            Column column = columns.get(i);
            // 如果值为空或null字符串
            if (value == null || value.isEmpty()) {
                stmt.setNull(i + 1, toColumnType(column.getType()));
            } else {
                setParameterValue(stmt, i + 1, value, column.getType());
            }
        }
    }

    protected void setParameterValue(PreparedStatement stmt, int parameterIndex,
                                     String value, String dataType) throws SQLException {
        if (dataType == null) {
            stmt.setString(parameterIndex, value);
            return;
        }
        int columnType = toColumnType(dataType);
        if (Types.TINYINT == columnType) {
            stmt.setByte(parameterIndex, Byte.parseByte(value));
        } else if (Types.SMALLINT == columnType) {
            stmt.setShort(parameterIndex, Short.parseShort(value));
        } else if (Types.INTEGER == columnType) {
            stmt.setInt(parameterIndex, Integer.parseInt(value));
        } else if (Types.BIGINT == columnType) {
            stmt.setLong(parameterIndex, Long.parseLong(value));
        } else if (Types.DECIMAL == columnType || Types.NUMERIC == columnType) {
            stmt.setBigDecimal(parameterIndex, new BigDecimal(value));
        } else if (Types.FLOAT == columnType || Types.REAL == columnType) {
            stmt.setFloat(parameterIndex, Float.parseFloat(value));
        } else if (Types.DOUBLE == columnType) {
            stmt.setDouble(parameterIndex, Double.parseDouble(value));
        } else if (Types.BOOLEAN == columnType || Types.BIT == columnType) {
            stmt.setBoolean(parameterIndex, Boolean.parseBoolean(value));
        } else if (Types.DATE == columnType) {
            stmt.setDate(parameterIndex, Date.valueOf(value));
        } else if (Types.TIMESTAMP == columnType) {
            stmt.setTimestamp(parameterIndex, Timestamp.valueOf(value));
        } else if (Types.TIME == columnType) {
            stmt.setTime(parameterIndex, Time.valueOf(value));
        } else if (Types.BINARY == columnType || Types.VARBINARY == columnType || Types.LONGVARBINARY == columnType) {
            stmt.setBytes(parameterIndex, value.getBytes());
        } else if (Types.CHAR == columnType || Types.VARCHAR == columnType || Types.LONGVARCHAR == columnType) {
            stmt.setString(parameterIndex, value);
        } else if (Types.NCHAR == columnType || Types.NVARCHAR == columnType || Types.LONGNVARCHAR == columnType) {
            stmt.setNString(parameterIndex, value);
        } else if (Types.CLOB == columnType) {
            stmt.setClob(parameterIndex, new java.io.StringReader(value));
        } else if (Types.NCLOB == columnType) {
            stmt.setNClob(parameterIndex, new java.io.StringReader(value));
        } else if (Types.BLOB == columnType) {
            stmt.setBlob(parameterIndex, new java.io.ByteArrayInputStream(value.getBytes()));
        } else if (Types.ARRAY == columnType) {
            // 数组类型，暂时作为字符串处理
            stmt.setString(parameterIndex, value);
        } else if (Types.STRUCT == columnType) {
            // 结构体类型，使用setObject
            stmt.setObject(parameterIndex, value);
        } else if (Types.REF == columnType) {
            // 引用类型，暂时作为字符串处理
            stmt.setString(parameterIndex, value);
        } else if (Types.DATALINK == columnType) {
            try {
                stmt.setURL(parameterIndex, new java.net.URL(value));
            } catch (java.net.MalformedURLException e) {
                // URL格式错误时作为字符串处理
                stmt.setString(parameterIndex, value);
            }
        } else if (Types.SQLXML == columnType) {
            // XML类型，暂时作为字符串处理
            stmt.setString(parameterIndex, value);
        } else if (Types.ROWID == columnType) {
            // 行ID类型，暂时作为字符串处理
            stmt.setString(parameterIndex, value);
        } else if (Types.DISTINCT == columnType || Types.JAVA_OBJECT == columnType) {
            // 自定义类型和Java对象类型
            stmt.setObject(parameterIndex, value);
        } else if (Types.NULL == columnType) {
            // NULL类型
            stmt.setNull(parameterIndex, columnType);
        } else {
            // 默认作为字符串处理
            stmt.setString(parameterIndex, value);
        }
    }

    /**
     * 从数据类型映射到JDBC类型
     *
     * @param dataType
     * @return
     */
    protected abstract int toColumnType(String dataType);

    protected String quoteIdentifier(String identifier) {
        return semanticAdapter.quoteIdentifier(identifier);
    }
}
