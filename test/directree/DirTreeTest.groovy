package directree

class DirTreeTest extends GroovyTestCase {

    protected void setUp() {
        File.metaClass {
            mkdirs = {-> false}
            setText = {text -> }
        }
    }

    protected void tearDown() {
        File.metaClass = null
    }

    void "test should be able to create DirTreeBuilder with root dir"() {
        File.metaClass.mkdirs = {-> assert delegate.name == "root"}
        DirTree.create("root")
    }

    void "test should fail creation of DirTreeBuilder without root dir"() {
        shouldFail {
            DirTree.create()
        }
        shouldFail {
            DirTree.create({})
        }
    }

    void "test chaining and nesting"() {
        def mkdirCount = 0
        def setTextCount = 0
        File.metaClass {
            mkdirs {-> mkdirCount++}
            setText {text -> setTextCount++}
        }

        DirTree.create("root").file("a.txt", "hello").dir("src")
        DirTree.create("root") {
            file("a.txt") {
                // todo probably remove this feature
                // todo test what gets written to this file
                dir("illegaldir") {
                    // illegal semantically, so this dir should be created in parent of file
                    it == "root"
                }
                file("somefile") {
                    // illegal semantically, so this file should be created in parent of file
                    it == "root"
                }
            }
        }

        assert mkdirCount == 4
        assert setTextCount == 3
    }

    void "test closures should get the complete path from root"() {
        DirTree.create "root", {
            assert it == "root"
            dir("src") {
                assert it == "root/src"
                file("a.txt") {
                    assert it == "root/src/a.txt"
                }
            }
        }
    }

    void "test should write what is passed as string or what closure returns"() {
        File.metaClass.setText { text -> assert text == "hello"}

        DirTree.create("root/src")
                .file("a.txt", "hello")
                .file("b.txt", {"hello"})
                .file("c.txt") {"hello"}
    }

    void "test should create empty file when nothing is passed or closure returns nothing"() {
        File.metaClass.setText { text -> assert text == ""}

        DirTree.create("root/src") {
            file("a.txt")
            file("b.txt") { }
            file("c.txt", { })
            file("d.txt", null)
        }
    }
}
