package directree

import groovy.transform.Canonical

/**
 * Represents a writable file (Text file)
 */
@Canonical
class WritableFile extends AbstractPath {
    File file
    final def content

    /**
     * Build a writable file object. The content is not written to disk yet. create method writes changes to the disk
     *
     * @param options
     * @param file
     * @param content
     */
    WritableFile(Map options = [:], File file, content) {
        this.file = file
        this.content = content
        this.options = options
    }

    /**
     * Writes the file to the file system
     *
     * @return
     */
    def create() {
        final text = (content instanceof Closure) ? content(file.toString()) : content
        file.text = text ?: ""
    }
}
