package directree

import groovy.transform.Canonical

/**
 * Represents any path (file or directory) on file-system
 */
abstract class AbstractPath {

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

    private DirTree(File baseDir, Closure closure) {
        this.dir = baseDir
        closure?.resolveStrategy = Closure.DELEGATE_FIRST
        closure?.delegate = this
        closure?.call(baseDir.toString())
    }

    DirTree(String baseDir, Closure closure = {}) {
        this(new File(baseDir), closure)
    }

    def create() {
        dir.mkdirs()
        children.each { key, value -> value.create() }
    }

    def dir(String name, Closure closure = {}) {
        children[name] = new DirTree(new File(dir, name), closure)
        this
    }

    def file(String name, def content = "") {
        children[name] = new WritableFile(new File(dir, name), content)
        this
    }

    static DirTree build(String name, Closure closure = {}) {
        new DirTree(name, closure)
    }

    static void create(String name, Closure closure = {}) {
        new DirTree(name, closure).create()
    }

    File getFile() {dir}

    /**
     * get the abstract path by name in the current dir
     *
     * @param key
     * @return path - you need to call .file on the returned path to get java.util.File representation
     */
    def getAt(String key) { children[key] }
    def propertyMissing(String name) { children[name] }

}
