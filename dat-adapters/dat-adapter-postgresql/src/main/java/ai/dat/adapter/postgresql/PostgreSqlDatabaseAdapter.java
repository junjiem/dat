package ai.dat.adapter.postgresql;

import ai.dat.core.adapter.GenericSqlDatabaseAdapter;

import javax.sql.DataSource;

/**
 * @Author JunjieM
 * @Date 2025/7/2
 */
public class PostgreSqlDatabaseAdapter extends GenericSqlDatabaseAdapter {
    public PostgreSqlDatabaseAdapter(DataSource dataSource) {
        super(new PostgreSqlSemanticAdapter(), dataSource);
    }

    @Override
    protected Object handleSpecificTypes(Object value, int columnType) {
        return value;
    }
}
