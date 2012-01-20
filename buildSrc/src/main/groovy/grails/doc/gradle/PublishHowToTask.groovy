/* Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.doc.gradle

import grails.doc.HowToPublisher
import grails.doc.macros.HiddenMacro

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

/**
 * Gradle task for generating a gdoc-based HTML user guide.
 */
class PublishHowToTask extends DefaultTask {
    @InputDirectory File sourceDir = new File(project.projectDir, "src")
    @OutputDirectory File targetDir = project.outputDir as File
    @InputDirectory File resourcesDir = new File(project.projectDir, "resources")
    @Input String language = ""

    Collection macros = []
    File workDir = project.buildDir as File

    @TaskAction
    def publish() {
        def publisher = new HowToPublisher(sourceDir, targetDir)
        publisher.workDir = workDir
        publisher.language = language ?: 'en'
        publisher.images = project.file("${resourcesDir}/images")
        publisher.css = project.file("${resourcesDir}/css")
        publisher.js = project.file("${resourcesDir}/js")
        publisher.templates = project.file("${resourcesDir}/templates")

        // Add custom macros.

        // {hidden} macro for enabling translations.
        publisher.registerMacro(new HiddenMacro())

        for (m in macros) {
            publisher.registerMacro(m)
        }

        // Radeox loads its bundles off the context class loader, which
        // unfortunately doesn't contain the grails-docs JAR. So, we
        // temporarily switch the HowToPublisher class loader into the
        // thread so that the Radeox bundles can be found.
        def oldClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = publisher.getClass().classLoader

        publisher.publish()

        // Restore the old context class loader.
        Thread.currentThread().contextClassLoader = oldClassLoader
    }
}

