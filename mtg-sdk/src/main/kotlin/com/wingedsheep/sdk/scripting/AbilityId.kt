package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Unique identifier for an ability instance.
 */
@JvmInline
@Serializable
value class AbilityId(val value: String) {
    companion object {
        private var counter = 0L

        fun generate(): AbilityId = AbilityId("ability_${++counter}")
    }
}
