package ai.dat.adapter.oracle;

import ai.dat.core.adapter.SemanticAdapter;
import ai.dat.core.semantic.data.Dimension;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.OracleSqlDialect;

/**
 * Oracle数据库语义适配器
 */
public class OracleSemanticAdapter implements SemanticAdapter {

    @Override
    public SqlDialect getSqlDialect() {
        return OracleSqlDialect.DEFAULT;
    }

    @Override
    public String applyTimeGranularity(String dateExpr, Dimension.TypeParams.TimeGranularity granularity) {
        if (dateExpr == null || dateExpr.trim().isEmpty()) {
            throw new IllegalArgumentException("Date expression cannot be null or empty");
        }
        return switch (granularity) {
            case YEAR -> "TRUNC(" + dateExpr + ", 'YEAR')";
            case QUARTER -> "TRUNC(" + dateExpr + ", 'Q')";
            case MONTH -> "TRUNC(" + dateExpr + ", 'MONTH')";
            case WEEK -> "TRUNC(" + dateExpr + ", 'WW')";
            case DAY -> "TRUNC(" + dateExpr + ", 'DD')";
            case HOUR -> "TRUNC(" + dateExpr + ", 'HH24')";
            case MINUTE -> "TRUNC(" + dateExpr + ", 'MI')";
            case SECOND -> "TRUNC(" + dateExpr + ", 'MI') + TRUNC(EXTRACT(SECOND FROM " + dateExpr + ")) / 86400";
        };
    }
} 