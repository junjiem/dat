package ai.dat.core.utils;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;

import java.util.Map;

/**
 * @Author JunjieM
 * @Date 2025/7/16
 */
public class JinjaTemplateUtil {
    private static final Jinjava JINJAVA;

    static {
        JinjavaConfig config = JinjavaConfig.newBuilder()
                .withTrimBlocks(true)
                .withLstripBlocks(true)
                .build();
        JINJAVA = new Jinjava(config);
    }

    private JinjaTemplateUtil() {
    }

    public static String render(String template, Map<String, Object> variables) {
        return JINJAVA.render(template, variables);
    }
}
