package directree

/**
 * Represents any path (file or directory) on file-system
 */
abstract class AbstractPath {
    /**
     * options are stored conditional precessing or storing meta data about the file.
     */
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
