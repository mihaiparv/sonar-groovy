/*
 * Sonar Groovy Plugin
 * Copyright (C) 2010-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.groovy.codenarc;

import org.apache.commons.lang.CharUtils;
import org.assertj.core.api.Fail;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.groovy.TestUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import static org.assertj.core.api.Assertions.assertThat;

public class CodeNarcProfileExporterTest {

  private StringWriter writer;
  private CodeNarcProfileExporter exporter;

  @Before
  public void setUp() {
    writer = new StringWriter();
    exporter = new CodeNarcProfileExporter(writer);
  }

  @Test
  public void shouldExportProfile() throws Exception {
    exporter.exportActiveRules(new ActiveRulesBuilder()
                                   .addRule(new NewActiveRule.Builder()
                                                .setRuleKey(RuleKey.of(CodeNarcRulesDefinition.REPOSITORY_KEY,
                                                                       "org.codenarc.rule.basic.AddEmptyStringRule"))
                                                .setName("Add Empty String")
                                                .setSeverity(Severity.MAJOR)
                                                .build())
                                   .addRule(new NewActiveRule.Builder()
                                                .setRuleKey(RuleKey.of(CodeNarcRulesDefinition.REPOSITORY_KEY,
                                                                       "org.codenarc.rule.size.ClassSizeRule"))
                                                .setName("Class Size")
                                                .setSeverity(Severity.MAJOR)
                                                .build())
                                   .build());

    assertSimilarXml(
      TestUtils.getResource("/org/sonar/plugins/groovy/codenarc/exportProfile/exportProfile.xml"),
      writer.toString());
  }

  @Test
  public void shouldFailToExport() throws IOException {
    Writer writer = Mockito.mock(Writer.class);
    Mockito.when(writer.append(Matchers.any(CharSequence.class))).thenThrow(new IOException());
    exporter = new CodeNarcProfileExporter(writer);

    try {
      exporter.exportActiveRules(new ActiveRulesBuilder().build());
      Fail.fail("Should have failed");
    }  catch(IllegalStateException e) {
      assertThat(e.getMessage()).contains("Fail to export CodeNarc profile");
    }
  }

  @Test
  public void shouldExportParameters() throws Exception {
    exporter.exportActiveRules(new ActiveRulesBuilder()
                                   .addRule(new NewActiveRule.Builder()
                                                .setRuleKey(RuleKey.of(CodeNarcRulesDefinition.REPOSITORY_KEY,
                                                                       "org.codenarc.rule.size.ClassSizeRule"))
                                                .setName("Class Size")
                                                .setSeverity(Severity.MAJOR)
                                                .setParam("maxLines", "20")
                                                .build())
                                   .build());

    assertSimilarXml(
      TestUtils.getResource("/org/sonar/plugins/groovy/codenarc/exportProfile/exportParameters.xml"),
      writer.toString());
  }

  @Test
  public void shouldNotExportUnsetParameters() throws Exception {
    exporter.exportActiveRules(new ActiveRulesBuilder()
                                   .addRule(new NewActiveRule.Builder()
                                                .setRuleKey(RuleKey.of(CodeNarcRulesDefinition.REPOSITORY_KEY,
                                                                       "org.codenarc.rule.size.ClassSizeRule"))
                                                .setName("Class Size")
                                                .setSeverity(Severity.MAJOR)
                                                .setParam("maxLines", null)
                                                .build())
                                   .build());

    assertSimilarXml(
      TestUtils.getResource("/org/sonar/plugins/groovy/codenarc/exportProfile/exportNullParameters.xml"),
      writer.toString());
  }

  @Test
  public void shouldExportFixedRulesCorrectly() throws Exception {
    exporter.exportActiveRules(new ActiveRulesBuilder()
                                   .addRule(new NewActiveRule.Builder()
                                                .setRuleKey(RuleKey.of(CodeNarcRulesDefinition.REPOSITORY_KEY,
                                                                       "org.codenarc.rule.design.PrivateFieldCouldBeFinalRule.fixed"))
                                                .setName("Private Field Could Be Final")
                                                .setSeverity(Severity.MAJOR)
                                                .build())
                                   .build());

    assertSimilarXml(
      TestUtils.getResource("/org/sonar/plugins/groovy/codenarc/exportProfile/exportFixedRules.xml"),
      writer.toString());
  }

  @Test
  public void shouldEscapeExportedParameters() throws Exception {
    exporter.exportActiveRules(new ActiveRulesBuilder()
                                   .addRule(new NewActiveRule.Builder()
                                                .setRuleKey(RuleKey.of(CodeNarcRulesDefinition.REPOSITORY_KEY,
                                                                       "org.codenarc.rule.naming.ClassNameRule"))
                                                .setName("Class Name")
                                                .setSeverity(Severity.MAJOR)
                                                .setParam("regex", "[A-Z]+[a-z&&[^bc]]")
                                                .build())
                                   .build());

    assertSimilarXml(
      TestUtils.getResource("/org/sonar/plugins/groovy/codenarc/exportProfile/exportEscapedParameters.xml"),
      writer.toString());
  }

  private void assertSimilarXml(File expectedFile, String xml) throws Exception {
    XMLUnit.setIgnoreWhitespace(true);
    Reader reader = new FileReader(expectedFile);
    Diff diff = XMLUnit.compareXML(reader, xml);
    String message = "Diff: " + diff.toString() + CharUtils.LF + "XML: " + xml;
    Assert.assertTrue(message, diff.similar());
  }
}
