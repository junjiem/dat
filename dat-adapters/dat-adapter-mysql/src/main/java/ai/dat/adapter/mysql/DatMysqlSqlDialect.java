package ai.dat.adapter.mysql;

import ai.dat.core.semantic.SqlParserWrapper;
import ai.dat.core.semantic.calcite.DatSqlBetweenOperator;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.dialect.MysqlSqlDialect;
import org.apache.calcite.sql.fun.SqlBetweenOperator;
import org.apache.calcite.sql.parser.SqlParseException;

/**
 * @Author JunjieM
 * @Date 2025/9/11
 */
public class DatMysqlSqlDialect extends MysqlSqlDialect {

    public static final SqlDialect DEFAULT = new DatMysqlSqlDialect(DEFAULT_CONTEXT);

    public DatMysqlSqlDialect(Context context) {
        super(context);
    }

    @Override
    public void unparseCall(SqlWriter writer, SqlCall call, int leftPrec, int rightPrec) {
        switch (call.getKind()) {
            case BETWEEN:
                SqlBetweenOperator sqlBetweenOperator = (SqlBetweenOperator) call.getOperator();
                DatSqlBetweenOperator datSqlBetweenOperator = new DatSqlBetweenOperator(
                        sqlBetweenOperator.flag, sqlBetweenOperator.isNegated());
                datSqlBetweenOperator.unparse(writer, call, leftPrec, rightPrec, false);
                break;
            default:
                super.unparseCall(writer, call, leftPrec, rightPrec);
        }
    }

    public static void main(String[] args) throws SqlParseException {
        String ansiSql = "select * from test WHERE data_dt BETWEEN '2025-01-01'AND '2025-12-31'";
        SqlNode sqlNode = new SqlParserWrapper().parseQuery(ansiSql);
        String mysqlSql = sqlNode.toSqlString(DatMysqlSqlDialect.DEFAULT).getSql();
        System.out.println(mysqlSql);
    }
}
