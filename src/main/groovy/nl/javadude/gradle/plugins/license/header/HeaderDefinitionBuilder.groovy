/*
 * Copyright (C)2011 - Jeroen van Erp <jeroen@javadude.nl>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.javadude.gradle.plugins.license.header

import com.mycila.maven.plugin.license.header.HeaderDefinition
import org.gradle.api.Named
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

class HeaderDefinitionBuilder {
  @Input String type
  @Input String firstLine = ""
  @Input String beforeEachLine = ""
  @Input String afterEachLine = ""
  @Input String endLine = ""
  @Input boolean allowBlankLines = false

  @Input @Optional String skipLinePattern
  @Input String firstLineDetectionPattern
  @Input String lastLineDetectionPattern
  @Input boolean isMultiline = false
  @Input boolean padLines = false

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

  @Internal
  String getName() {
    return type
  }
}
