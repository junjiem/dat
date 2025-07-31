package ai.dat.core.adapter;

import ai.dat.core.semantic.data.SemanticModel;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 数据库适配器接口类
 *
 * @Author JunjieM
 * @Date 2025/6/25
 */
public interface DatabaseAdapter {
    SemanticAdapter semanticAdapter();

    String generateSql(String semanticSql, List<SemanticModel> semanticModels);

    List<Map<String, Object>> executeQuery(String sql) throws SQLException;
}