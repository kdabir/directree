package directree

import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertNotNull

class DirTreeTest {

    @Before
    void setUp() {
        File.metaClass {
            mkdirs = {-> false}
            setText = {text -> }
        }
    }

    @After
    void tearDown() {
        File.metaClass = null
    }


    @Test
    void "should instantiate DirTree with root dir" () {
        def dirTree = new DirTree("a")
        //assert dirTree["a"] == new File("a")
        assertNotNull(dirTree)
    }


    @Test
    void "should be able to create DirTree with root dir"() {
        def called = false
        File.metaClass.mkdirs = {-> assert delegate.name == "root"; called = true}
        DirTree.create("root")
        assert called
    }


    @Test(expected = Exception)
    void "should fail creation of DirTreeBuilder without root dir"() {
        DirTree.create()
    }

    @Test(expected = Exception)
    void "should fail creation of DirTreeBuilder without root dir 2"() {
        DirTree.create({})
    }

    @Test
    void "should be able to chain and nest"() {
        def mkdirCount = 0
        def setTextCount = 0
        File.metaClass {
            mkdirs {-> mkdirCount++}
            setText {text -> setTextCount++}
        }

        new DirTree("root").file("a.txt", "hello").dir("src").create()
        DirTree.create("root") {
            file("a.txt")
            dir("test") {
                file("b.txt")
            }
        }

        assert mkdirCount == 4
        assert setTextCount == 3
    }

    @Test
    void "closures should get the complete path from root"() {
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

    @Test
    void "test should write what is passed as string or what closure returns"() {
        int count = 0
        File.metaClass.setText { text -> assert text == "hello"; count++}

        new DirTree("root/src")
                .file("a.txt", "hello")
                .file("b.txt", {"hello"})
                .file("c.txt") {"hello"}.create()
        assert count == 3
    }

    @Test
    void "test should create empty file when nothing is passed or closure returns nothing"() {
        int count = 0
        File.metaClass.setText { text -> assert text == ""; count++}

        DirTree.build("root/src") {
            file("a.txt")
            file("b.txt") { }
            file("c.txt", { })
            file("d.txt", null)
        }.create()
        assert count == 4
    }
}
