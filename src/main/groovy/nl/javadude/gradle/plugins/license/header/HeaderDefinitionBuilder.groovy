package nl.javadude.gradle.plugins.license.header

import com.google.code.mojo.license.header.HeaderDefinition
import org.gradle.api.Named

class HeaderDefinitionBuilder implements Named {
  String type
  String firstLine
  String beforeEachLine
  String endLine
  boolean allowBlankLines = false

  String skipLinePattern
  String firstLineDetectionPattern
  String lastLineDetectionPattern
  boolean isMultiline = false

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

  HeaderDefinition build() {
    return new HeaderDefinition(type,
      firstLine,
      beforeEachLine,
      endLine,
      skipLinePattern,
      firstLineDetectionPattern,
      lastLineDetectionPattern,
      allowBlankLines,
      isMultiline)
  }

  @Override
  String toString() {
    return "{" +
      "type='" + type + '\'' +
      ", firstLine='" + firstLine + '\'' +
      ", beforeEachLine='" + beforeEachLine + '\'' +
      ", endLine='" + endLine + '\'' +
      ", allowBlankLines=" + allowBlankLines +
      ", skipLinePattern='" + skipLinePattern + '\'' +
      ", firstLineDetectionPattern='" + firstLineDetectionPattern + '\'' +
      ", lastLineDetectionPattern='" + lastLineDetectionPattern + '\'' +
      ", isMultiline=" + isMultiline +
      '}'
  }

  @Override
  String getName() {
    return type
  }
}
