package ai.dat.adapter.mysql;

import ai.dat.core.adapter.GenericSqlDatabaseAdapter;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * @Author JunjieM
 * @Date 2025/7/1
 */
public class MySqlDatabaseAdapter extends GenericSqlDatabaseAdapter {

    public MySqlDatabaseAdapter(DataSource dataSource) {
        super(new MySqlSemanticAdapter(), dataSource);
    }

    @Override
    protected Object handleSpecificTypes(Object value, int columnType) {
        if (value == null) {
            return null;
        }
        switch (columnType) {
            case Types.BIT:
                if (value instanceof Boolean) {
                    return value;
                } else if (value instanceof byte[] bytes) {
                    return bytes.length > 0 && bytes[0] != 0;
                }
                break;
            case Types.TINYINT:
                if (value instanceof Number) {
                    int intValue = ((Number) value).intValue();
                    return intValue != 0;
                }
                break;
            case Types.TIMESTAMP:
                if (value instanceof Timestamp) {
                    return value;
                }
                break;
        }
        return value;
    }
}