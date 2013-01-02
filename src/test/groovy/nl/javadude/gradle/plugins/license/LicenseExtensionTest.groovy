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

package nl.javadude.gradle.plugins.license

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import org.gradle.api.Task
import nl.javadude.gradle.plugins.license.task.License
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue
import static org.hamcrest.CoreMatchers.*
import static org.junit.matchers.JUnitMatchers.*
import org.junit.Before

class LicenseExtensionTest {
    LicenseExtension extension;

    @Before
    public void setupProject() {
        extension = new LicenseExtension();
    }

    @Test
    public void ableToConstruct() {
        assert extension != null;
    }

    @Test
    public void ableToAddSimpleMapping() {
        extension.mapping('js', 'JAVA')
        assert extension.internalMappings.containsKey('js')

        extension.mapping 'java', 'JAVA'
        assert extension.internalMappings.containsKey('java')
    }

    @Test
    public void ableToAddMapMapping() {
        extension.mapping(['js':'JAVA', 'java':'JAVA'])
        extension.mapping groovy:'JAVA' 
        assert extension.internalMappings.containsKey('js')
        assert extension.internalMappings.containsKey('java')
        assert extension.internalMappings.containsKey('groovy')
    }

    @Test
    public void ableToAddClosureMapping() {
        extension.mapping {
            put('js', 'JAVA')
            put 'java', 'JAVA'
            groovy = 'JAVA'
        }
        assert extension.internalMappings.containsKey('js')
        assert extension.internalMappings.containsKey('java')
        assert extension.internalMappings.containsKey('groovy')
    }

    @Test
    public void ableToAddGStringMapping() {
        def var = 'VA'
        extension.mapping('js', "JA${var}")
        assert extension.internalMappings.containsKey('js')
        assert extension.internalMappings['js'] == 'JAVA'
    }
}

