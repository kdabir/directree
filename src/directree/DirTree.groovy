package directree

/**
 * A DSL to perform operations on and/or create a Directory Tree and Text Files with content
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

    /**
     * Constructs a representation of Directory Tree. This is not written to file system by default.
     * use create method to write the tree on file system
     *
     * @param options
     * @param baseDir
     * @param closure
     */
    DirTree(Map options = [:], String baseDir, Closure closure = {}) {
        this(options, new File(baseDir), closure)
    }

    /**
     * Creates the tree on the file system.
     *
     * @return
     */
    def create() {
        dir.mkdirs()
        children.each { key, value -> value.create() }
    }

    /**
     * Represents a Directory Tree with files.
     *
     * @param options Map of supported options
     * @param name Required, name of file)
     * @param content Optional, String or Closure
     */
    def dir(Map options, String name, Closure closure = {}) {
        children[name] = new DirTree(options, new File(dir, name), closure)
        this
    }

    /**
     * Represents a Directory Tree with files.
     *
     * @param name Required, name of directory
     * @param closure Optional, Closure representing nested files
     */
    def dir(String name, Closure closure = {}) {
        dir([:], name, closure)
    }

    /**
     * Represents a file with content. Options can be stored for processing and/or later retrieval
     *
     * @param options Map of supported options
     * @param name Required, name of file)
     * @param content Optional, String or Closure
     */
    def file(Map options, String name, def content = "") {
        children[name] = new WritableFile(options, new File(dir, name), content)
        this
    }

    /**
     * Represents a file with content.
     *
     * @param name Required, name of file)
     * @param content Optional, String or Closure
     */
    def file(String name, def content = "") {
        file([:],name, content)
    }

    /**
     * Builds a Directory Tree but does not write it to file system
     *
     * @param options
     * @param name
     * @param closure
     */
    static DirTree build(Map options, String name, Closure closure = {}) {
        new DirTree(options, name, closure)
    }

    /**
     * Builds a Directory Tree but does not write it to file system
     *
     * @param name
     * @param closure
     * @return
     */
    static DirTree build(String name, Closure closure = {}) {
        new DirTree([:], name, closure)
    }

    /**
     * Utility method to build and create a directory tree. The changes are written to file system.
     *
     * @param options
     * @param name
     * @param closure
     */
    static void create(Map options, String name, Closure closure = {}) {
        new DirTree(options, name, closure).create()
    }

    /**
     * Utility method to build and create a directory tree. The changes are written to file system.
     *
     * @param name
     * @param closure
     */
    static void create(String name, Closure closure = {}) {
        new DirTree([:], name, closure).create()
    }

    /**
     * returns the Directory represented as a File object
     *
     * @return File
     */
    File getFile() { dir }

    /**
     * get the abstract path by name in the current dir
     *
     * @param key
     * @return path - you need to call .file on the returned path to get java.util.File representation
     */
    def getAt(String key) { children[key] }

    def propertyMissing(String name) { children[name] }

    /**
     * Executes the closure for every entry in the DirTree
     *
     * @param closure
     * @return
     */
    def walk(Closure c) {
        [c.call(this), children.collect { key, value -> (value instanceof DirTree) ? value.walk(c) : c.call(value) }].flatten()
    }

}
