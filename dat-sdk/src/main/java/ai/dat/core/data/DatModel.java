package ai.dat.core.data;

import ai.dat.core.utils.DatSchemaUtil;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a single semantic model composed of a validated SQL select statement.
 */
@Getter
public class DatModel {

    @NonNull
    private String name;

    @NonNull
    private String sql;

    /**
     * Creates a new instance while enforcing that the underlying SQL is a SELECT statement.
     *
     * @param name the model identifier
     * @param content the original SQL content that may contain comments
     */
    private DatModel(@NonNull String name, @NonNull String content) {
        this.name = name;
        String sql = DatSchemaUtil.removeSqlComments(content).trim();
        Preconditions.checkArgument(DatSchemaUtil.isSelectSql(sql),
                "Only SELECT statements are allowed. Provided SQL: " + sql);
        this.sql = sql;
    }

    /**
     * Builds a {@link DatModel} from the supplied raw SQL content.
     *
     * @param name the model identifier
     * @param content the raw SQL definition, potentially containing comments
     * @return a normalized {@link DatModel}
     */
    public static DatModel from(@NonNull String name, @NonNull String content) {
        return new DatModel(name, content);
    }

    /**
     * Simple demonstration entry point that showcases how SQL comments are stripped.
     *
     * @param args ignored program arguments
     */
    public static void main(String[] args) {
        String sql = "      -- Sample query using MySQL dialect\n" +
                "      select\n" +
                "      CAST(STR_TO_DATE(date_rep, '%d/%m/%Y') AS DATE) as date_rep,\n" +
                "      cases,\n" +
                "      deaths,\n" +
                "      geo_id\n" +
                "      from covid_cases\n" +
                "      \n" +
                "      /*\n" +
                "      -- Sample query using DuckDB dialect\n" +
                "      select\n" +
                "      CAST(strptime(date_rep, '%d/%m/%Y') AS DATE) as date_rep,\n" +
                "      cases,\n" +
                "      deaths,\n" +
                "      geo_id\n" +
                "      from covid_cases\n" +
                "      */";

        DatModel model = DatModel.from("test", sql);
        System.out.println(model.getSql());
    }
}