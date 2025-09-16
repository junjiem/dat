package ai.dat.core.data;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;

import java.util.regex.Pattern;

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
        String sql = removeSqlComments(content).trim();
        Preconditions.checkArgument(
                Pattern.compile("^\\s*select\\b", Pattern.CASE_INSENSITIVE).matcher(sql).find(),
                "Only SELECT statements are allowed. Provided SQL: " + sql);
        this.sql = sql;
    }

    public static DatModel from(@NonNull String name, @NonNull String content) {
        return new DatModel(name, content);
    }

    private String removeSqlComments(String text) {
        return text.replaceAll("--.*", "") // 移除单行注释 (-- 注释)
                .replaceAll("/\\*.*?\\*/", ""); // 移除多行注释 (/* 注释 */)
    }
}