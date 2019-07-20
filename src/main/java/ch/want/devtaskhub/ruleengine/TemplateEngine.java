/*
 * Created on 27 Jul 2018
 */
package ch.want.devtaskhub.ruleengine;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Component
public class TemplateEngine {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateEngine.class);
    @Autowired
    private Configuration freemarkerConfig;

    public String render(final String template, final Map<String, Object> templateData) throws IOException {
        final Template t = new Template(Integer.toString(template.hashCode()), new StringReader(template), freemarkerConfig);
        try {
            return FreeMarkerTemplateUtils.processTemplateIntoString(t, templateData);
        } catch (final TemplateException e) {
            LOG.warn("Failed to render template [{}] with data [{}]", template, templateData.toString());
            throw new IOException(e.getMessage(), e);
        }
    }
}
