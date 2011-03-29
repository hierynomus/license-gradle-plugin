/* License added by: GRADLE-LICENSE-PLUGIN
 *
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
        assertThat output.lines.size, Is.is(5)
    }
    
    @Test
    public void shouldAddHashToLines() {
        List<String> input = ["Line 1", "Line 2"]
        def output = new LicenseFormat(prefix: '#', line: '#').transform(input)

        assertThat output.lines, hasItem("# " + LicenseFormat.LICENSE_KEY)
        assertThat output.lines, hasItem("# Line 1")
        assertThat output.lines, hasItem("# Line 2")
        assertThat output.lines.size, Is.is(4)
    }

}





