package nl.javadude.gradle.plugins.license.types

import org.junit.Test
import static org.junit.internal.matchers.StringContains.containsString
import static org.junit.Assert.assertThat
import org.hamcrest.core.Is
import static org.junit.internal.matchers.IsCollectionContaining.hasItem

class HashFormatTest {

	@Test
	public void shouldAddHashToLines() {
		List<String> input = ["Line 1", "Line 2"]
		def output = new HashFormat().transform(input)
		assertThat output, hasItem("# Line 1")
		assertThat output, hasItem("# Line 2")
		assertThat output.size, Is.is(2)
	}
}
