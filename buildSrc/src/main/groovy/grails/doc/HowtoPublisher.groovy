/* Copyright 2004-2005 the original author or authors.
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

package grails.doc

import grails.doc.internal.StringEscapeCategory
import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import org.apache.commons.logging.LogFactory
import org.radeox.engine.context.BaseInitialRenderContext

/**
 * <p>Publishes a directory of gdoc files as How Tos. The structure is expected
 * to be src/$lang/*.gdoc, where each source file is self-contained.</p>
 * <p>By default, the template layout for the generated HTML is expected to be
 * ./resources/templates/how-to-template.html. This should be a standard
 * Groovy template (for SimpleTemplateEngine) and it has access to these
 * variables:</p>
 * <ul>
 * <li><em>title</em>: The title of the HOWTO.</li>
 * <li><em>content</em>: The HTML content generated from the gdoc file.</li>
 * <li><em>toc</em>: A linked hash map of section aliases and section titles.
 * The aliases are IDs on the heading HTML elements, so can be used as URL
 * fragments in links - useful for generating a TOC.</li>
 * <li><em>resourcesPath</em>: The relative path to the parent directory of the
 * 'img', 'css', and 'js' folders.</li>
 * </ul>
 *
 * @author Peter Ledbrook
 */
class HowToPublisher {
    static final LOG = LogFactory.getLog(this)

    /** The source directory of the documentation */
    File src = new File("src")
    /** The target directory to publish to */
    File target = new File("output")
    /** The temporary work directory */
    File workDir
    /** The directory containing any images to use */
    File img = new File("resources/img")
    /** The directory containing any CSS to use */
    File css = new File("resources/css")
    /** The directory containing any Javascript to use */
    File js = new File("resources/js")
    /** The directory containing any templates to use */
    File templates = new File("resources/templates")
    /** The language we're generating for (gets its own sub-directory). Defaults to '' */
    String language = "en"
    /** The encoding to use (default is UTF-8) */
    String encoding = "UTF-8"

    def output
    private context
    private engine
    private customMacros = []

    HowToPublisher() {
        this(null, null)
    }

    HowToPublisher(File src, File target, out = LOG) {
        this.src = src
        this.target = target
        this.output = out
    }

    /**
     * Registers a custom Radeox macro. If the macro has an 'initialContext'
     * property, it is set to the render context before first use.
     */
    void registerMacro(macro) {
        customMacros << macro
    }

    /**
     * Publishes all the HOWTO gdoc guides in the configured directory.
     */
    void publish() {
        // Adds encodeAsUrlPath(), encodeAsUrlFragment() and encodeAsHtml()
        // methods to String.
        use(StringEscapeCategory) {
            publishWithoutCodecs()
        }
    }

    /**
     * Publishes all the HOWTO gdoc guides in the configured directory. It
     * does not add the encode*() methods to String though, unlike {@link #publish()}.
     */
    protected void publishWithoutCodecs() {
        initialize()
        if (!src?.exists()) {
            this.output.warn "Source directory '${src}' does not exist."
            return
        }

        def outputDir = calculateLanguageDir(target?.absolutePath ?: "./docs")
        copyResources outputDir

        def files = src.listFiles()?.findAll { it.name.endsWith(".gdoc") } ?: []
        def docs = files.collect { f -> new HowToDocument(f, generateHtml(f)) }

        // The HTML page template may need the file names and titles to generate links.
        def howToTemplate = new SimpleTemplateEngine().createTemplate(templateFile.newReader(encoding))
        def filenamesToTitles = docs.collectEntries { d -> [ stripSuffix(d.source.name), d.title ] }

        for (d in docs) {
            def outFile = new File(outputDir, stripSuffix(d.source.name) + ".html")
            def page = new HowToPage(d, howToTemplate, calculatePathToResources(), filenamesToTitles)
            outFile.withWriter(encoding) { w -> w.write page }
        }
    }

    protected File getTemplateFile() {
        return new File(templates, "how-to-template.html")
    }

    protected String generateHtml(file) {
        return engine.render(file.getText(encoding), context)
    }

    protected void initialize() {
        if (language) {
            src = new File(src, language)
        }

        if (!workDir) {
            workDir = new File(System.getProperty("java.io.tmpdir"))
        }

        context = new BaseInitialRenderContext()
        context.set HowToDocEngine.RESOURCES_CONTEXT_PATH, calculatePathToResources()
        engine = new HowToDocEngine(context)
        context.renderEngine = engine

        // Add any custom macros registered with this publisher to the engine.
        for (m in customMacros) {
            if (m.metaClass.hasProperty(m, "initialContext")) {
                m.initialContext = context
            }
            engine.addMacro m
        }
    }

    protected copyResources(outputDir) {
        def ant = new AntBuilder()
        ant.mkdir(dir: outputDir)

        for (type in ["img", "css", "js"]) {
            copyResourcesOfType ant, outputDir, type
        }
    }

    protected copyResourcesOfType(ant, outputDir, type) {
        def dir = new File(outputDir, calculatePathToResources(type)).path
        ant.mkdir dir: dir

        def srcDir = this."$type"
        if (srcDir?.exists()) {
            ant.copy todir: dir, overwrite: true, failonerror: false, {
                fileset dir: this."$type"
            }
        }
    }

    protected String calculateLanguageDir(startPath, endPath = '') {
        def elements = [startPath, language, endPath]
        elements = elements.findAll { it }
        return elements.join('/')
    }

    protected String calculatePathToResources(String pathToRoot = '') {
        return language ? new File('..', pathToRoot).path : pathToRoot
    }

    protected stripSuffix(String name) {
        def suffixPos = name.lastIndexOf('.')
        if (suffixPos != -1) return name.substring(0, suffixPos)
        else return name
    }
}

/**
 * Represents a HOWTO document that has been initialised with the HTML
 * produced from a wiki page. It's main use is to generate the final
 * HTML page including the layout and any table of contents.
 */
class HowToDocument {
    static final titlePattern = ~/<h1[^>]*>(.*?)<\/h1>/
    static final sectionPattern = ~/<h2([^>]*)>(.*?)<\/h2>/

    def template

    private source
    private html
    private toc = [:]
    private title

    HowToDocument(source, String wikiHtml) {
        this.source = source as File
        this.html = generateToc(wikiHtml)
    }

    File getSource() { return source }
    String getHtml() { return html }
    String getTitle() { return title }
    Map getToc() { return Collections.unmodifiableMap(toc) }

    protected generateToc(wikiHtml) {
        title = extractTitle(wikiHtml)
        return processSections(wikiHtml)
    }

    protected extractTitle(wikiHtml) {
        def m = titlePattern.matcher(wikiHtml)    
        if (m) return m[0][1]
        else return null
    }

    /**
     * Takes the HTML generated from a wiki page and extracts the 2nd-level
     * headings into a mini table of contents.
     */
    protected processSections(wikiHtml) {
        def m = sectionPattern.matcher(wikiHtml)
        if (m) {
            def buffer = new StringBuffer()
            def index = 1
            m.reset()
            while (m.find()) {
                // Create an HTML anchor for this section.
                def alias = "s$index"

                // Save the heading's content in the TOC. Doesn't need
                // HTML escaping when rendered in an HTML template!
                toc[alias] = m.group(2)

                // Insert an 'id' attribute into this heading.
                m.appendReplacement(buffer, '<h2 id="' + alias + '"$1>$2</h2>')
                index++
            }

            m.appendTail(buffer)
            return buffer.toString()
        }
        else return wikiHtml
    }
}

/**
 * Composes a HowToDocument and an HTML Groovy template into a single HTML page.
 * Whenever it is written, the page is regenerated.
 */
class HowToPage implements Writable {
    private doc
    private template
    private pathToRoot
    private filenamesToTitlesMap

    HowToPage(doc, Template template, String pathToRoot, Map filenamesToTitlesMap) {
        this.doc = doc
        this.template = template
        this.pathToRoot = pathToRoot
        this.filenamesToTitlesMap = [*: filenamesToTitlesMap]
    }

    Writer writeTo(Writer writer) {
        writer.write(makeTemplate())
    }

    protected makeTemplate() {
        return template.make(
                title: doc.title,
                content: doc.html,
                toc: doc.toc,
                howtos: filenamesToTitlesMap,
                resourcesPath: pathToRoot)
    }
}
