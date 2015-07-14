package directree

import groovy.time.TimeCategory
import groovy.time.TimeDuration

/**
 * Syncs source dir(s) to target dir (utilizing the underlying ant) at a given interval.
 *
 * source (see http://ant.apache.org/manual/Types/fileset.html)
 * target (see http://ant.apache.org/manual/Tasks/sync.html)
 *
 * Instances of this class are not meant to be accessed by multiple threads. In short, Thread Safety is not guaranteed.
 *
 */
class Synchronizer {

    /* Constants */
    static final def ALLOWED_KEYS_FOR_PRESERVE = ['includes', 'preserveEmptyDirs'].asImmutable()
    static final def ALLOWED_KEYS_FOR_SOURCE_OPTS = ['includes', 'excludes', 'casesensitive'].asImmutable()
    static final def ALLOWED_KEYS_FOR_TARGET_OPTS = ['overwrite', 'failonerror', 'verbose', 'granularity', 'includeEmptyDirs'].asImmutable()
    static final int DEFAULT_SYNC_FREQUENCY = 3000
    static final int DEFAULT_INITIAL_DELAY = 0

    /* Collaborators */
    Timer timer = new Timer()
    AntBuilder ant = new AntBuilder()

    /* Internal state */
    private final List<Closure> beforeSyncCallbacks = []
    private final List<Closure> afterSyncCallbacks = []
    private final List<Map> sources = []
    private final Map target = [:]
    private final Map preserve = [:]
    private int syncFrequency = DEFAULT_SYNC_FREQUENCY
    private int initialDelay = DEFAULT_INITIAL_DELAY
    private long lastSynced = 0

    /* Query Methods */

    List<Closure> getBeforeSyncCallbacks() { beforeSyncCallbacks.asImmutable() }

    List<Closure> getAfterSyncCallbacks() { afterSyncCallbacks.asImmutable() }

    def getTarget() { target.asImmutable() }

    def getSources() { sources.asImmutable() }

    def getPreserved() { preserve.asImmutable() }

    int getSyncFrequencyInSeconds() { syncFrequency / 1000 }

    int getInitialDelayInSeconds() { initialDelay / 1000 }

    long getLastSynced() { lastSynced }

    /* Fluent setters */

    /**
     * Custom timer instance can be set here and it will be used for scheduling syncs.
     *
     * e.g. If you need to set the Timer name,
     *  withTimer(new Timer("myTimer"))
     *
     * @param timer
     * @return
     */
    def withTimer(Timer timer) { this.timer = timer; this }

    /**
     * Custom AntBuilder instance can be set here and it will be used for performing syncs
     *
     * e.g. you might want to use an AntBuilder instance which has BuildListeners already set.
     *
     * @param timer
     * @return
     */
    def withAnt(AntBuilder antBuilder) { this.ant = antBuilder; this }

    /**
     * Registers a callback hook which will be called before every sync
     *
     * @param closure
     * @return
     */
    def beforeSync(Closure closure) {
        closure?.resolveStrategy = Closure.DELEGATE_FIRST
        closure?.delegate = this

        beforeSyncCallbacks << closure;
        this
    }

    /**
     * Registers a callback hook which will be called after every sync
     *
     * @param closure
     * @return
     */
    def afterSync(Closure closure) {
        closure?.resolveStrategy = Closure.DELEGATE_FIRST
        closure?.delegate = this

        afterSyncCallbacks << closure;
        this
    }

    /**
     * Sync will happen at this frequency. Default is 3 seconds
     *
     * @param seconds
     * @return
     */
    def syncFrequencyInSeconds(int seconds) { syncFrequency = seconds * 1000; this }

    /**
     * Sync will happen at this frequency. Default is 3 seconds
     *
     * @param seconds
     * @return
     */
    def syncEvery(TimeDuration duration) { syncFrequency = duration.toMilliseconds(); this }

    /**
     * If provided, the first sync happens after the provided seconds. Default is 0
     *
     * @param seconds
     * @return
     */
    def initialDelayInSeconds(int seconds) { initialDelay = seconds * 1000; this }

    /**
     * If provided, the first sync happens after the provided seconds. Default is 0
     *
     * @param seconds
     * @return
     */
    def startAfter(TimeDuration duration) { initialDelay = duration.toMilliseconds(); this }

    // false values are okay, just don't let nulls in
    private def nonNullValues = { it.value != null }

    /**
     * Sets the Source Directory for Synchronizer
     *
     * Calling it multiple times for same Synchronizer instance indicates multiple source directories
     *
     * @param options - Optional Map, see ALLOWED_KEYS_FOR_SOURCE_OPTS
     * @param sourceDir - Required, Directory from which content is sync'ed
     * @return
     */
    def sourceDir(options = [:], String sourceDir) {
        final map = [dir: sourceDir] + options.subMap(ALLOWED_KEYS_FOR_SOURCE_OPTS).findAll(nonNullValues)
        sources << map.asImmutable()
        this
    }

    /**
     * Sets the Target Directory for Synchronizer
     *
     * There can be only one Target Directory to which content will be sync'ed. Calling it multiple times overwrites the
     * previous target values.
     *
     * @param options - Optional Map, see ALLOWED_KEYS_FOR_TARGET_OPTS
     * @param targetDir - Rquired, Directory to which content will be sync'ed
     * @return
     */
    def targetDir(options = [:], String targetDir) {
        // clear the map so the second call, which ideally should never happen, does not retain values from previous call
        target.clear()
        target << [todir: targetDir] + options.subMap(ALLOWED_KEYS_FOR_TARGET_OPTS).findAll(nonNullValues)
        this
    }

    /**
     * Sets the Preserved Directories in the Target Directory
     *
     * Invoking it multiple times overwrites the previous values
     *
     * @param options - Optional Map, see ALLOWED_KEYS_FOR_PRESERVE
     * @return
     */
    def preserve(Map options = [:]) {
        // clear the map so the second call, which ideally should never happen, does not retain values from previous call
        preserve.clear()
        preserve << options.subMap(ALLOWED_KEYS_FOR_PRESERVE).findAll(nonNullValues)
        this
    }

    /* API */
    /**
     * Syncs the Source Directories to Target Directory ONCE.
     * @deprecated use syncOnce
     */
    public sync() {
        syncOnce()
    }

    /**
     * Syncs the Source Directories to Target Directory ONCE.
     */
    public syncOnce() {
        beforeSyncCallbacks*.call()

        ant.sync(target) {
            sources.each { ant.fileset(it) }
            if (preserve) ant.preserveintarget(preserve)
        }

        lastSynced = System.currentTimeMillis()

        afterSyncCallbacks*.call()
    }

    /**
     * Starts the sync'ing at syncFrequency after initialDelay
     *
     */
    public void start() {
        timer.schedule(this.&syncOnce as TimerTask, initialDelay, syncFrequency)
    }

    /**
     * Stops the sync'ing.
     *
     * If you want to start syncing the on the same Synchronizer interface again, you need to provide a fresh instance of Timer.
     */
    public void stop() {
        timer.cancel()
    }

    /* DSL */
    /**
     * Static builder method that exposes nice DSL to create a Synchronizer instance.
     *
     *
     * @param closure
     * @return
     */
    static Synchronizer build(closure) {
        Integer.metaClass.mixin TimeCategory
        Synchronizer synchronizerToBeConfigured = new Synchronizer()

        closure?.resolveStrategy = Closure.DELEGATE_FIRST
        closure?.delegate = synchronizerToBeConfigured
        closure?.call()

        //enhancements: can do the validation here

        synchronizerToBeConfigured
    }
}


