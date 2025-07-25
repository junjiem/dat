package ai.dat.adapter.oracle;

import ai.dat.core.adapter.GenericSqlDatabaseAdapter;

import javax.sql.DataSource;
import java.sql.Types;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Oracle数据库适配器
 * 处理Oracle特定的数据类型转换
 * 
 * @Author JunjieM
 * @Date 2025/7/3
 */
public class OracleDatabaseAdapter extends GenericSqlDatabaseAdapter {

    public OracleDatabaseAdapter(DataSource dataSource) {
        super(new OracleSemanticAdapter(), dataSource);
    }

    @Override
    protected Object handleSpecificTypes(Object value, int columnType) {
        if (value == null) {
            return null;
        }
        
        switch (columnType) {
            case Types.NUMERIC:
            case Types.DECIMAL:
                // Oracle的NUMBER类型可能映射为BigDecimal
                if (value instanceof BigDecimal bd) {
                    // 如果是整数且在合理范围内，转换为Long或Integer
                    if (bd.scale() == 0) {
                        if (bd.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0 &&
                            bd.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) >= 0) {
                            return bd.intValue();
                        } else {
                            return bd.longValue();
                        }
                    }
                    return bd.doubleValue();
                }
                break;
                
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                // Oracle的TIMESTAMP类型
                if (value instanceof Timestamp) {
                    return value;
                }
                break;
                
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
                // Oracle的字符类型，去除末尾空格（如果是CHAR类型）
                if (value instanceof String str) {
                    return columnType == Types.CHAR ? str.trim() : str;
                }
                break;
                
            case Types.CLOB:
            case Types.NCLOB:
                // Oracle的CLOB类型
                if (value instanceof java.sql.Clob clob) {
                    try {
                        return clob.getSubString(1, (int) clob.length());
                    } catch (Exception e) {
                        return value.toString();
                    }
                }
                break;
        }
        
        return value;
    }
} 