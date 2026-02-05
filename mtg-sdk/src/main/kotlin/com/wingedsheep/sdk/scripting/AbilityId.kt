package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

/**
 * Unique identifier for an ability instance.
 */
@JvmInline
@Serializable
value class AbilityId(val value: String) {
    companion object {
        private val counter = AtomicLong(0)

        fun generate(): AbilityId = AbilityId("ability_${counter.incrementAndGet()}")
    }
}
