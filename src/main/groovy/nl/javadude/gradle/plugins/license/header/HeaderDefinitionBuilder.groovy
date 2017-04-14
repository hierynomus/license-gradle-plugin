package nl.javadude.gradle.plugins.license.header

import com.mycila.maven.plugin.license.header.HeaderDefinition
import org.gradle.api.Named

class HeaderDefinitionBuilder implements Named {
  String type
  String firstLine = ""
  String beforeEachLine = ""
  String afterEachLine = ""
  String endLine = ""
  boolean allowBlankLines = false

  String skipLinePattern
  String firstLineDetectionPattern
  String lastLineDetectionPattern
  boolean isMultiline = false
  boolean padLines = false

  static HeaderDefinitionBuilder headerDefinition(String name) {
    return new HeaderDefinitionBuilder(name)
  }

  HeaderDefinitionBuilder(String type) {
    this.type = type
  }

  HeaderDefinitionBuilder withFirstLine(String firstLine) {
    this.firstLine = firstLine
    return this
  }

  HeaderDefinitionBuilder withBeforeEachLine(String beforeEachLine) {
    this.beforeEachLine = beforeEachLine
    return this
  }

  HeaderDefinitionBuilder withAfterEachLine(String afterEachLine) {
    this.afterEachLine = afterEachLine
    return this
  }

  HeaderDefinitionBuilder withEndLine(String endLine) {
    this.endLine = endLine
    return this
  }

  HeaderDefinitionBuilder withNoBlankLines() {
    this.allowBlankLines = false
    return this
  }

  HeaderDefinitionBuilder withBlankLines() {
    this.allowBlankLines = true
    return this
  }

  HeaderDefinitionBuilder withSkipLinePattern(String skipLinePattern) {
    this.skipLinePattern = skipLinePattern
    return this
  }

  HeaderDefinitionBuilder withFirstLineDetectionDetectionPattern(String firstLineDetectionPattern) {
    this.firstLineDetectionPattern = firstLineDetectionPattern
    return this
  }

  HeaderDefinitionBuilder withLastLineDetectionDetectionPattern(String lastLineDetectionPattern) {
    this.lastLineDetectionPattern = lastLineDetectionPattern
    return this
  }

  HeaderDefinitionBuilder multiline() {
    this.isMultiline = true
    return this
  }

  HeaderDefinitionBuilder noMultiLine() {
    this.isMultiline = false
    return this
  }

  HeaderDefinitionBuilder padLines() {
    this.padLines = false
    return this
  }

  HeaderDefinitionBuilder noPadLines() {
    this.padLines = false
    return this
  }

  HeaderDefinition build() {
    return new HeaderDefinition(type,
      firstLine,
      beforeEachLine,
      endLine,
      afterEachLine,
      skipLinePattern,
      firstLineDetectionPattern,
      lastLineDetectionPattern,
      allowBlankLines,
      isMultiline,
      padLines)
  }

  @Override
  String toString() {
    return "{" +
      "type='" + type + '\'' +
      ", firstLine='" + firstLine + '\'' +
      ", beforeEachLine='" + beforeEachLine + '\'' +
      ", afterEachLine='" + afterEachLine + '\'' +
      ", endLine='" + endLine + '\'' +
      ", allowBlankLines=" + allowBlankLines +
      ", skipLinePattern='" + skipLinePattern + '\'' +
      ", firstLineDetectionPattern='" + firstLineDetectionPattern + '\'' +
      ", lastLineDetectionPattern='" + lastLineDetectionPattern + '\'' +
      ", isMultiline=" + isMultiline +
      ", padLines=" + padLines +
      '}'
  }

  @Override
  String getName() {
    return type
  }
}
