package directree

/**
 * a DSL to create a Directory Tree and text files with content
 *
 */
class DirTree {
    def baseDir

    // todo - validate the file/dir names

    private DirTree(String baseDir, Closure closure = {}) {
        this.baseDir = baseDir

        new File(baseDir).mkdirs()

        closure?.resolveStrategy = Closure.DELEGATE_FIRST
        closure?.delegate = this
        closure?.call(baseDir)
    }

    static def create(String name, Closure closure = {}) {
        new DirTree(name, closure)
    }

    def dir(String name, Closure closure = {}) {
        new DirTree("$baseDir/$name", closure)
    }

    /**
     * if no content string is provided or content closure does not return a string the content of file is set to blank string
     *
     * @param name
     * @param content String or Closure that returns a string to be written in the file
     * @return
     */
    def file(String name, def content = "") {
        final file_path = "$baseDir/$name"
        final text = (content instanceof Closure) ? content(file_path) : content
        new File(file_path).text = text ?: ""
        this
    }
}
