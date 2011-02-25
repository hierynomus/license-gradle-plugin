package nl.javadude.gradle.plugins.license.types

import org.junit.Test
import org.hamcrest.core.Is
import static org.junit.Assert.assertThat
import static org.junit.matchers.JUnitMatchers.hasItem

class LicenseFormatTest {
    @Test
    public void shouldAddSlashStar() {
        List<String> input = ["Line 1", "Line 2"]
        def output = new LicenseFormat(prefix: '/*', line: ' *', suffix: ' */').transform(input)
        assertThat output.lines, hasItem("/* " + LicenseFormat.LICENSE_KEY)
        assertThat output.lines, hasItem(" * Line 1")
        assertThat output.lines, hasItem(" * Line 2")
        assertThat output.lines, hasItem(" */")
        assertThat output.lines.size, Is.is(4)
    }
    
    @Test
    public void shouldAddHashToLines() {
        List<String> input = ["Line 1", "Line 2"]
        def output = new LicenseFormat(prefix: '#', line: '#').transform(input)

        assertThat output.lines, hasItem("# " + LicenseFormat.LICENSE_KEY)
        assertThat output.lines, hasItem("# Line 1")
        assertThat output.lines, hasItem("# Line 2")
        assertThat output.lines.size, Is.is(3)
    }

}
