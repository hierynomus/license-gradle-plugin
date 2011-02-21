package nl.javadude.gradle.plugins.license.types

import org.junit.Test
import org.hamcrest.core.Is
import static org.junit.internal.matchers.IsCollectionContaining.hasItem
import static org.junit.Assert.assertThat

class SlashStarFormatTest {

	@Test
	public void shouldAddSlashStar() {
		List<String> input = ["Line 1", "Line 2"]
		def output = new SlashStarFormat().transform(input)
		assertThat output.lines, hasItem("/*")
		assertThat output.lines, hasItem(" * Line 1")
		assertThat output.lines, hasItem(" * Line 2")
		assertThat output.lines, hasItem(" */")
		assertThat output.lines.size, Is.is(4)
	}
}
