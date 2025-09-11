package ai.dat.core.semantic.calcite;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.fun.SqlBetweenOperator;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.util.SqlBasicVisitor;
import org.apache.calcite.util.Util;

/**
 * @Author JunjieM
 * @Date 2025/9/11
 */
public class DatSqlBetweenOperator extends SqlBetweenOperator {

    private static final SqlWriter.FrameType FRAME_TYPE =
            SqlWriter.FrameTypeEnum.create("BETWEEN");

    public DatSqlBetweenOperator(Flag flag, boolean negated) {
        super(flag, negated);
    }

    public void unparse(SqlWriter writer, SqlCall call, int leftPrec, int rightPrec, boolean hasFlag) {
        final SqlWriter.Frame frame =
                writer.startList(FRAME_TYPE, "", "");
        call.operand(VALUE_OPERAND).unparse(writer, getLeftPrec(), 0);

        String name = super.getName();
        if (!hasFlag) {
            name = name.replaceFirst("\\s*(" + Flag.ASYMMETRIC.name()
                    + "|" + Flag.SYMMETRIC.name() + ")$", "");
        }
        writer.sep(name);

        // If the expression for the lower bound contains a call to an AND
        // operator, we need to wrap the expression in parentheses to prevent
        // the AND from associating with BETWEEN. For example, we should
        // unparse
        //    a BETWEEN b OR (c AND d) OR e AND f
        // as
        //    a BETWEEN (b OR c AND d) OR e) AND f
        // If it were unparsed as
        //    a BETWEEN b OR c AND d OR e AND f
        // then it would be interpreted as
        //    (a BETWEEN (b OR c) AND d) OR (e AND f)
        // which would be wrong.
        final SqlNode lower = call.operand(LOWER_OPERAND);
        final SqlNode upper = call.operand(UPPER_OPERAND);
        int lowerPrec = new AndFinder().containsAnd(lower) ? 100 : 0;
        lower.unparse(writer, lowerPrec, lowerPrec);
        writer.sep("AND");
        upper.unparse(writer, 0, getRightPrec());
        writer.endList(frame);
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * Finds an AND operator in an expression.
     */
    private static class AndFinder extends SqlBasicVisitor<Void> {
        @Override
        public Void visit(SqlCall call) {
            final SqlOperator operator = call.getOperator();
            if (operator == SqlStdOperatorTable.AND) {
                throw Util.FoundOne.NULL;
            }
            return super.visit(call);
        }

        boolean containsAnd(SqlNode node) {
            try {
                node.accept(this);
                return false;
            } catch (Util.FoundOne e) {
                return true;
            }
        }
    }
}
