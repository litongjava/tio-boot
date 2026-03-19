package com.litongjava.tio.utils.thymeleaf;

import java.io.Writer;
import java.util.Set;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;

public class ThymyleafEngine {

  private String name;
  private TemplateEngine engine;

  public ThymyleafEngine(TemplateEngine engine) {
    this.name = "main";
    this.engine = engine;
  }

  public ThymyleafEngine(String name, TemplateEngine engine) {
    this.name = name;
    this.engine = engine;
  }

  public String getName() {
    return name;
  }

  public String process(String template, Context context) {
    return engine.process(template, context);
  }

  public final String process(final String template, final Set<String> templateSelectors, final IContext context) {
    return engine.process(template, templateSelectors, context);
  }

  public final String process(final TemplateSpec templateSpec, final IContext context) {
    return engine.process(templateSpec, context);
  }

  public final void process(final String template, final IContext context, final Writer writer) {
    engine.process(template, context, writer);
  }

  public final void process(final String template, final Set<String> templateSelectors, final IContext context, final Writer writer) {
    engine.process(template, templateSelectors, context, writer);
  }

  public final void process(final TemplateSpec templateSpec, final IContext context, final Writer writer) {
    engine.process(templateSpec, context, writer);
  }

  public void clearTemplateCache() {
    engine.clearTemplateCache();
  }

}