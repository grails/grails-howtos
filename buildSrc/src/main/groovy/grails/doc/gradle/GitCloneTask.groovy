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

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.console.ConsoleCredentialsProvider
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

/**
 * Gradle task for cloning a git repository.
 */
class GitCloneTask extends DefaultTask {
    def uri
    def remote
    def branch
    def destination

    @Input String getUri() { uri?.toString() }
    @Input boolean bare
    @Input boolean cloneAllBranches = true
    @Input @Optional String getRemote() { remote?.toString() }
    @Input @Optional String getBranch() { branch?.toString() }
    @OutputDirectory File getDestination() { project.file(destination) }

    @TaskAction
    def clone() {
        def cmd = Git.cloneRepository()
        cmd.URI = uri
        cmd.directory = destination
        cmd.bare = bare
        cmd.remote = remote ?: "origin"
        cmd.branch = "refs/heads/${branch ?: 'master'}"
        cmd.cloneAllBranches = cloneAllBranches
        cmd.call()
    }
}

