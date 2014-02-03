package directree

import groovy.mock.interceptor.MockFor

class SynchronizerTest extends GroovyTestCase {

    Synchronizer synchronizer
    Closure doSomethingBefore = {}
    Closure doSomethingAfter = {}
    Closure doSomethingAnyway = {}

    void setUp() {
        // just to be sure stub the actual system level operations
        Timer.metaClass.schedule { TimerTask timerTask, long start, long frequency -> println "timer says metaclass saved me" }
        AntBuilder.metaClass.sync { hash, closure -> println "sync says metaclass saved me :)" }
        AntBuilder.metaClass.fileset { hash -> println "fileset says metaclass saved me :)" }

        synchronizer = Synchronizer.build {
            sourceDir "src", includes: "*.groovy", excludes: "*.class"
            sourceDir "etc", includes: "*.md"
            targetDir "out"
            preserve includes: "*.html"

            syncEvery 3.seconds
            startAfter 1.second

            beforeSync doSomethingBefore
            beforeSync doSomethingAnyway

            afterSync doSomethingAfter
            afterSync doSomethingAnyway
        }
    }

    void tearDown() {
        Timer.metaClass = null
        AntBuilder.metaClass = null
    }

    /* CREATION RELATED TESTS */

    void "test should create instance of Synchronizer with DSL"() {
        assert synchronizer.target == [todir: "out"]
        assert synchronizer.preserved == [includes: "*.html"]
        assert synchronizer.sources == [[dir: "src", includes: "*.groovy", excludes: "*.class"], [dir: "etc", includes: "*.md"]]
        assert synchronizer.syncFrequencyInSeconds == 3
        assert synchronizer.initialDelayInSeconds == 1
        assert synchronizer.beforeSyncCallbacks.first() == doSomethingBefore
        assert synchronizer.beforeSyncCallbacks.last() == doSomethingAnyway
        assert synchronizer.afterSyncCallbacks.first() == doSomethingAfter
        assert synchronizer.afterSyncCallbacks.last() == doSomethingAnyway
    }

    void "test should create instance of Synchronizer with fluent interface"() {
        def otherSynchronizer = new Synchronizer()
                .sourceDir("dirA", includes: "**/*.java")
                .sourceDir("dirB")
                .targetDir("dirC", overwrite: false)
                .preserve(includes: "generated/**")
                .syncFrequencyInSeconds(10)
                .initialDelayInSeconds(3)
                .beforeSync(doSomethingBefore)
                .beforeSync(doSomethingAnyway)
                .afterSync(doSomethingAfter)
                .afterSync(doSomethingAnyway)

        assert otherSynchronizer.target == [todir: "dirC", overwrite:false]
        assert otherSynchronizer.preserved == [includes: "generated/**"]
        assert otherSynchronizer.sources == [[dir: "dirA", includes: "**/*.java"], [dir: "dirB"]]
        assert otherSynchronizer.syncFrequencyInSeconds == 10
        assert otherSynchronizer.initialDelayInSeconds == 3
        assert otherSynchronizer.beforeSyncCallbacks.first() == doSomethingBefore
        assert otherSynchronizer.beforeSyncCallbacks.last() == doSomethingAnyway
        assert otherSynchronizer.afterSyncCallbacks.first() == doSomethingAfter
        assert otherSynchronizer.afterSyncCallbacks.last() == doSomethingAnyway
    }

    void "test should create instance of Synchronizer with default values where possible"() {
        def otherSynchronizer = new Synchronizer()
                .sourceDir("dirA", includes: "**/*.java")
                .targetDir("dirC", overwrite: false)

        assert otherSynchronizer.preserved == [:]
        assert otherSynchronizer.syncFrequencyInSeconds == 3
        assert otherSynchronizer.initialDelayInSeconds == 0
        assert otherSynchronizer.beforeSyncCallbacks == []
        assert otherSynchronizer.afterSyncCallbacks == []
    }

    void "test lastSynced should for a newly created synchronizer" () {
        assert synchronizer.lastSynced == 0
    }

    /* Ant operations test */

    void "test pass right values to ant for single source dir"() {
        def mockingContextForAntBuilder = new MockFor(AntBuilder)
        mockingContextForAntBuilder.demand.sync { target, cls -> assert [todir:"out", verbose:true]; cls()}
        mockingContextForAntBuilder.demand.fileset { map -> assert [dir:"only", includes:"*.txt", casesensitive:false] == map }
        mockingContextForAntBuilder.demand.preserveintarget { map -> assert [includes: "*.md"]== map }

        def mockAntBuilder = mockingContextForAntBuilder.proxyInstance()

        new Synchronizer()
                .sourceDir("only", includes:"*.txt", casesensitive:false)
                .targetDir("out", verbose:true)
                .preserve(includes: "*.md")
                .withAnt(mockAntBuilder)
                .sync()

        mockingContextForAntBuilder.verify(mockAntBuilder)
    }

    void "test pass right values to ant for multiple source dirs"() {
        def callOrder = [
                [dir: "src", includes: "*.groovy", excludes: "*.class"],
                [dir: "etc", includes: "*.md"]
        ] as Queue

        def mockingContextForAntBuilder = new MockFor(AntBuilder)
        mockingContextForAntBuilder.demand.sync(1) { target, cls ->  cls()}
        mockingContextForAntBuilder.demand.fileset(2) { map -> assert callOrder.poll() == map }
        mockingContextForAntBuilder.demand.preserveintarget(1) { map -> }

        def mockAntBuilder = mockingContextForAntBuilder.proxyInstance()

        synchronizer.withAnt(mockAntBuilder).sync()

        mockingContextForAntBuilder.verify(mockAntBuilder)
    }


    /* Timer related tests */
    void "test start should call sync at set frequency"() {
        def synchronizer = Synchronizer.build {}
        boolean syncCalled = false

        Timer.metaClass.schedule { TimerTask timerTask, long start, long frequency ->
            // have to give exact signature else other schedule method is called
            syncCalled = true
            assert frequency == 3000
            assert start == 0
        }
        synchronizer.start()

        assert syncCalled
    }

    void "test should cancel timer"() {
        def cancelCalled = false
        Timer.metaClass.cancel { -> cancelCalled = true }

        synchronizer.stop()

        assert cancelCalled
    }

    void "test sync should call before and after hooks"() {
        def beforeCalled = false
        def afterCalled = false

        def otherSynchronizer = Synchronizer.build {
            beforeSync { beforeCalled = true }
            afterSync { afterCalled = true }
        }

        otherSynchronizer.sync()

        assert beforeCalled
        assert afterCalled
    }


    void "test every sync should update the lastSynced"() {
        def timeBeforeRun = System.currentTimeMillis()

        synchronizer.sync()

        assert synchronizer.lastSynced >= timeBeforeRun
    }



    // test submapping, so that only valid values are being sent to ant
}
