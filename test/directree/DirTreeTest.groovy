package directree

import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertNotNull

class DirTreeTest {

    NamedCounters count
    // use count.increment(..) inside closures
    // see: http://stackoverflow.com/questions/22117694/groovy-implicit-call-not-working-on-instance-variables-inside-closure


    @Before
    void setUp() {
        count = new NamedCounters()
        File.metaClass {
            mkdirs = { -> count.increment("mkdirs"); false }
            setText = { text -> count.increment("setText") }
        }
    }

    @After
    void tearDown() {
        File.metaClass = null
    }

    @Test
    void "should instantiate DirTree with root dir"() {
        def dirTree = new DirTree("a")
        //assert dirTree["a"] == new File("a")
        assertNotNull(dirTree)
    }

    @Test
    void "should be able to create DirTree with root dir"() {
        def called = false
        File.metaClass.mkdirs = { -> assert delegate.name == "root"; called = true }
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
        new DirTree("root").file("a.txt", "hello").dir("src").create()
        DirTree.create("root") {
            file("a.txt")
            dir("test") {
                file("b.txt")
            }
        }

        assert count['mkdirs'] == 4
        assert count['setText'] == 3
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
        File.metaClass.setText { text -> assert text == "hello"; count.increment("setText") }

        new DirTree("root/src")
                .file("a.txt", "hello")
                .file("b.txt", { "hello" })
                .file("c.txt") { "hello" }.create()

        assert count['setText'] == 3
    }

    @Test
    void "test should create empty file when nothing is passed or closure returns nothing"() {
        File.metaClass.setText { text -> assert text == ""; count.increment("setText") }

        DirTree.build("root/src") {
            file("a.txt")
            file("b.txt") {}
            file("c.txt", {})
            file("d.txt", null)
        }.create()

        assert count["setText"] == 4
    }

    @Test
    void "should be able to refer a file in DirTree"() {

        def tree = DirTree.build("root") {
            file("a.txt")
            dir("test") {
                file("b.txt")
            }
        }

        assert tree['a.txt'].file == new File("root/a.txt")
        assert tree.'a.txt'.file == new File("root/a.txt")
        assert tree['test']['b.txt'].file == new File("root/test/b.txt")
    }

    @Test
    void "should return null if file does not exists"() {

        def tree = DirTree.build("root") {
            file("a.txt")
            dir("test") {
                file("b.txt")
            }
        }

        assert tree['b.txt'] == null
    }

    @Test
    void "should be able to add arbitrary options"() {

        def tree = DirTree.build("root") {
            file("a.txt", overwrite:true)
            dir("test", required:true) {
                file("b.txt")
            }
        }

        assert tree['a.txt'].options == [overwrite: true]
        assert tree['test'].options == [required: true]
    }
}
