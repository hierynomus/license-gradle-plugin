package nl.javadude.gradle.plugins.license.header

import org.hamcrest.CoreMatchers
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.junit.Assert.assertThat

class HeaderDefinitionBuilderTest
{
  @Rule
  public ExpectedException expectedException = ExpectedException.none()

  HeaderDefinitionBuilder builder = HeaderDefinitionBuilder.headerDefinition("freddie")
    .withFirstLine("5 September 1946")
    .withBeforeEachLine("Rock")
    .withEndLine("24 November 1991")
    .withFirstLineDetectionDetectionPattern("First day")
    .withLastLineDetectionDetectionPattern("Last day")
    .withSkipLinePattern("HIV")
    .multiline()
    .withBlankLines()

  @Test
  void setsType()
  {
    assertThat builder.build().type, CoreMatchers.is("freddie")
  }

  @Test
  void setsFirstLine()
  {
    assertThat builder.build().firstLine, CoreMatchers.is("5 September 1946")
  }

  @Test
  void setsEndLine()
  {
    assertThat builder.build().endLine, CoreMatchers.is("24 November 1991")
  }

  @Test
  void setsBeforeEachLine()
  {
    assertThat builder.build().beforeEachLine, CoreMatchers.is("Rock")
  }

  @Test
  void setsFirstLineDetectionPattern()
  {
    assertThat builder.build().isFirstHeaderLine("First day"), CoreMatchers.is(true)
  }

  @Test
  void setsLastLineDetectionPattern()
  {
    assertThat builder.build().isLastHeaderLine("Last day"), CoreMatchers.is(true)
  }

  @Test
  void setsMultiline()
  {
    assertThat builder.build().isMultiLine(), CoreMatchers.is(true)
  }

  @Test
  void disablesMultiline()
  {
    builder.noMultiLine()

    assertThat builder.build().isMultiLine(), CoreMatchers.is(false)
  }

  @Test
  void allowsBlankLines()
  {
    assertThat builder.build().allowBlankLines(), CoreMatchers.is(true)
  }

  @Test
  void noBlankLines()
  {
    builder.withNoBlankLines()
    assertThat builder.build().allowBlankLines(), CoreMatchers.is(false)
  }

  @Test
  void setsSkipLines()
  {
    assertThat builder.build().isSkipLine("HIV"), CoreMatchers.is(true)
  }

  @Test
  void validatesOnBuild()
  {
    expectedException.expect(IllegalStateException)
    expectedException.expectMessage("missing for header definition")

    HeaderDefinitionBuilder.headerDefinition("farrokh_bulsara").build()
  }
}
