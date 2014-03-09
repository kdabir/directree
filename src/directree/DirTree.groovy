package directree

import groovy.transform.Canonical

/**
 * Represents any path (file or directory) on file-system
 */
abstract class AbstractPath {
    Map options

    /**
     * creates the path in the appropriate way
     * @return
     */
    abstract def create()

    /**
     * returns the file representation
     *
     * @return File object
     */
    abstract File getFile()

}

/**
 * if no content string is provided or content closure does not return a string the content of file is set to blank string
 */
@Canonical
class WritableFile extends AbstractPath {
    File file
    final def content

    WritableFile(Map options = [:], File file, content) {
        this.file = file
        this.content = content
        this.options = options
    }

    def create() {
        final text = (content instanceof Closure) ? content(file.toString()) : content
        file.text = text ?: ""
    }
}

/**
 * a DSL to perform operations on and/or create a Directory Tree and Text Files with content
 *
 */
class DirTree extends AbstractPath {
    File dir
    Map<String, AbstractPath> children = [:]

    private DirTree(Map options, File baseDir, Closure closure) {
        this.dir = baseDir
        this.options = options
        closure?.resolveStrategy = Closure.DELEGATE_FIRST
        closure?.delegate = this
        closure?.call(baseDir.toString())
    }

    DirTree(Map options = [:], String baseDir, Closure closure = {}) {
        this(options, new File(baseDir), closure)
    }

    def create() {
        dir.mkdirs()
        children.each { key, value -> value.create() }
    }

    def dir(Map options, String name, Closure closure = {}) {
        children[name] = new DirTree(options, new File(dir, name), closure)
        this
    }

    def dir(String name, Closure closure = {}) {
        dir([:], name, closure)
    }

    def file(Map options, String name, def content = "") {
        children[name] = new WritableFile(options, new File(dir, name), content)
        this
    }

    def file(String name, def content = "") {
        file([:],name, content)
    }

    static DirTree build(Map options, String name, Closure closure = {}) {
        new DirTree(options, name, closure)
    }

    static DirTree build(String name, Closure closure = {}) {
        new DirTree([:], name, closure)
    }

    static void create(Map options, String name, Closure closure = {}) {
        new DirTree(options, name, closure).create()
    }

    static void create(String name, Closure closure = {}) {
        new DirTree([:], name, closure).create()
    }

    File getFile() { dir }

    /**
     * get the abstract path by name in the current dir
     *
     * @param key
     * @return path - you need to call .file on the returned path to get java.util.File representation
     */
    def getAt(String key) { children[key] }

    def propertyMissing(String name) { children[name] }

    def walk(Closure c) {
        [c.call(this), children.collect { key, value -> (value instanceof DirTree) ? value.walk(c) : c.call(value) }].flatten()
    }

}
