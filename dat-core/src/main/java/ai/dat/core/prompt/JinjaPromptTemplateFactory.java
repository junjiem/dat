package ai.dat.core.prompt;

import ai.dat.core.utils.JinjaTemplateUtil;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.spi.prompt.PromptTemplateFactory;

import java.util.Map;

/**
 * Jinja提示词模板工厂类
 *
 * @Author JunjieM
 * @Date 2025/6/26
 */
public class JinjaPromptTemplateFactory implements PromptTemplateFactory {

    @Override
    public Template create(Input input) {
        return new JinjaPromptTemplateFactory.JinjaTemplate(input.getTemplate());
    }

    static class JinjaTemplate implements PromptTemplateFactory.Template {

        private final String template;

        public JinjaTemplate(String template) {
            this.template = ValidationUtils.ensureNotBlank(template, "template");
        }

        @Override
        public String render(Map<String, Object> variables) {
            return JinjaTemplateUtil.render(template, variables);
        }
    }
}
