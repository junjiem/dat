package ai.dat.core.data;

import ai.dat.core.utils.DatSchemaUtil;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;

/**
 * @Author JunjieM
 * @Date 2025/7/15
 */
@Getter
public class DatModel {

    @NonNull
    private String name;

    @NonNull
    private String sql;

    private DatModel(@NonNull String name, @NonNull String content) {
        this.name = name;
        String sql = DatSchemaUtil.removeSqlComments(content).trim();
        Preconditions.checkArgument(DatSchemaUtil.isSelectSql(sql),
                "Only SELECT statements are allowed. Provided SQL: " + sql);
        this.sql = sql;
    }

    public static DatModel from(@NonNull String name, @NonNull String content) {
        return new DatModel(name, content);
    }

    public static void main(String[] args) {
        String sql = "      -- 这是一个 MySQL 方言示例查询SQL\n" +
                "      select\n" +
                "      CAST(STR_TO_DATE(date_rep, '%d/%m/%Y') AS DATE) as date_rep,\n" +
                "      cases,\n" +
                "      deaths,\n" +
                "      geo_id\n" +
                "      from covid_cases\n" +
                "      \n" +
                "      /*\n" +
                "      -- 这是一个 DuckDB 方言的示例查询SQL\n" +
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