package directree

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Maintain multiple named counters
 */
class NamedCounters {

    private final Map<Object, AtomicLong> counters = [:] as ConcurrentHashMap

    private AtomicLong get(name) { counters.get(name, new AtomicLong(0)) }

    // public API
    def increment(name) { get(name).incrementAndGet() }
    def value(name) { get(name).longValue() }

    // groovy convenience
    def call(name) { increment(name) }
    def getAt(String name) { value(name) }
    def propertyMissing(String name) { value(name) }
}
